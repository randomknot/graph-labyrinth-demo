package com.randomknot.graphdemo


import scala.xml.XML

import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.Graph

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils

import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.client.remote.OServerAdmin

import net.caoticode.Buhtig

object Example2 extends App {

  val connInfo = OConnectionInfo("remote:localhost/githubv3", "admin", "admin")
  
  initializeGraph()

  val token = "f7dd997038fe62bca495c590b52eb9860e84985c"
  val buhtig = new Buhtig(token)
  val github = buhtig.client
  val search = parse(github.search.repositories ? ("q" -> "tinkerpop", "language" -> "java", "sort" -> "forks", "order" -> "desc", "per_page" -> "100") sync)

  private implicit val formats = DefaultFormats

  for (
    JObject(item) <- search \ "items";
    JField("full_name", JString(fullname)) <- item
  ) {

    val Array(user, repo) = fullname.split("/")

    github.repos(user, repo).contents("pom.xml").syncOpt map { pominfo =>
      val itemobj = JObject(item)
      val JString(base64pom) = parse(pominfo) \ "content"

      val repository = buildRepo(itemobj)
      val dependencies = {
        try{ buildDependencies(XML.loadString(decodeB64(base64pom))) } catch { case e: Throwable => Seq() } // xml in the wild can be dangerous...
      }
      val contributors = buildContributors(parse(github.repos(user, repo).contributors ? ("per_page" -> "100") sync))

      buildGraph(repository, dependencies, contributors)
    }
  }

  buhtig.close()

  def buildRepo(item: JObject) = {
    (("gid" -> item \ "id") ~
      ("fullname" -> item \ "full_name") ~
      ("name" -> item \ "name") ~
      ("description" -> item \ "description") ~
      ("watchers" -> item \ "watchers") ~
      ("language" -> item \ "language") ~
      ("created" -> item \ "created_at") ~
      ("node_type" -> "Repository")).extract[Repository]
  }

  def buildDependencies(pom: scala.xml.Elem) = {
    for (dep <- pom \ "dependencies" \ "dependency") yield {
      val (group, artifact) = (dep \ "groupId" text, dep \ "artifactId" text)

      (("group_id" -> group) ~
        ("artifact_id" -> artifact) ~
        ("ga" -> s"${group}_${artifact}") ~
        ("node_type" -> "Dependency")).extract[Dependency]
    }
  }

  def buildContributors(contrib: JValue) = {
    for (
      JObject(c) <- contrib;
      JField("id", JInt(id)) <- c;
      JField("login", JString(login)) <- c
    ) yield {
      (("gid" -> id) ~
        ("login" -> login) ~
        ("node_type" -> "Contributor")).extract[Contributor]
    }
  }

  def decodeB64(s: String) = {
    StringUtils.newStringUtf8(Base64.decodeBase64(s));
  }

  def buildGraph(repo: Repository, deps: Seq[Dependency], contribs: Seq[Contributor]) = {
    val factory = new OrientGraphFactory(connInfo.url , connInfo.user, connInfo.password)
    implicit val graph = factory.getNoTx()

    try {
      val repovxOpt = findVertex("gid", repo.gid)
      
      // go on only if the repo does not exists
      if (repovxOpt.isEmpty) {
        val repovx = graph.addVertex("class:Repository", getCCParams(repo))
        
        for (contrib <- contribs) {
          val contribvx = findVertex("gid", contrib.gid).getOrElse(graph.addVertex("class:Contributor", getCCParams(contrib)))
          graph.addEdge(null, contribvx, repovx, "contributes")
        }

        for (dep <- deps) {
          val depvx = findVertex("ga", dep.ga).getOrElse(graph.addVertex("class:Dependency", getCCParams(dep)))
          graph.addEdge(null, repovx, depvx, "depends")
        }
      }

      println(s"finished importing $repo with contributors and dependencies")
    } finally {
      graph.shutdown()
      factory.close()
    }
  }

  def getCCParams(cc: AnyRef): java.util.Map[String, Any] = {
    import scala.collection.JavaConversions._
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }
  }

  def findVertex(field: String, value: Any)(implicit graph: Graph) = {
    val iter = graph.getVertices(field, value).iterator()
    if (iter.hasNext())
      Some(iter.next())
    else
      None
  }

  def initializeGraph(): Unit = {
    val serverAdmin = new OServerAdmin(connInfo.url).connect("root", "root")

    if (!serverAdmin.existsDatabase("plocal")) {
      serverAdmin.createDatabase("graph", "plocal")

      val factory = new OrientGraphFactory(connInfo.url , connInfo.user, connInfo.password)
      val graph = factory.getNoTx()

      graph.createVertexType("Repository");
      graph.createVertexType("Contributor");
      graph.createVertexType("Dependency");

      graph.createEdgeType("depends");
      graph.createEdgeType("contributes");
      
      graph.createKeyIndex("gid", classOf[Vertex])
      graph.createKeyIndex("ga", classOf[Vertex])
      graph.createKeyIndex("fullname", classOf[Vertex])
      graph.createKeyIndex("login", classOf[Vertex])
      
      graph.shutdown()
      factory.close()
    } 
    
    serverAdmin.close()
  }

  case class Repository(gid: Int, fullname: String = "", name: String = "", description: String = "", watchers: Int = 0, language: String = "", created: String = "", node_type: String)
  case class Dependency(group_id: String, artifact_id: String, ga: String, node_type: String)
  case class Contributor(gid: Int, login: String, node_type: String)
  case class OConnectionInfo(url: String, user: String, password: String)
}