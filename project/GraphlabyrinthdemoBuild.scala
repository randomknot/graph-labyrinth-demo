import sbt._
import sbt.Keys._

object GraphlabyrinthdemoBuild extends Build {

  lazy val graphlabyrinthdemo = Project(
    id = "graph-labyrinth-demo",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "graph-labyrinth-demo",
      organization := "com.randomknot.graphdemo",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.1",

      resolvers += "oss.sonatype.org" at "https://oss.sonatype.org/content/repositories/snapshots",

      libraryDependencies += "com.jcabi" % "jcabi-github" % "1.0-SNAPSHOT",
      libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10",
      libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
      libraryDependencies += "commons-codec" % "commons-codec" % "1.9",

      libraryDependencies += "com.tinkerpop.blueprints" % "blueprints-core" % "2.5.0",
      libraryDependencies += "com.tinkerpop.gremlin" % "gremlin-java" % "2.5.0",
      libraryDependencies += "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.5.0",
      libraryDependencies += "com.orientechnologies" % "orientdb-core" % "1.7-rc2",
      libraryDependencies += "com.orientechnologies" % "orientdb-enterprise" % "1.7-rc2",
      libraryDependencies += "com.orientechnologies" % "orientdb-client" % "1.7-rc2",
      libraryDependencies += "com.orientechnologies" % "orientdb-graphdb" % "1.7-rc2"
    )
  )
}
