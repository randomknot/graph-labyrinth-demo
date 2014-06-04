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
      scalaVersion := "2.11.1"
      // add other settings here
    )
  )
}
