import sbt._
import Keys._

object MinimumBuild extends Build {

  val appName         = "iron-cache-plugin"
  val pluginVersion   = "1.0"
  val buildVersion    = "2.1.5"

  val baseSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.10.0"
  )

  lazy val plugin = Project(appName, file("plugin")).settings(baseSettings: _*).settings(
    version := pluginVersion,
    publishTo <<= version { (v: String) =>
      if (v.trim.endsWith("SNAPSHOT"))
        Some(Resolver.file("snapshots",  new File( "../maven-repo/snapshots" )))
      else
        Some(Resolver.file("releases",  new File( "../maven-repo/releases" )))
    },
//    publishTo := Some(Resolver.file("file",  new File( "path/to/my/maven-repo/releases" )) ),
    publishMavenStyle := true,
    organization := "com.github.tmwtmp100",
    libraryDependencies += "play" %% "play" % buildVersion % "provided"
  )

  lazy val sampleProject = play.Project("iron-cache-sample", pluginVersion, path = file("sample")).settings(baseSettings: _*).settings(
    publishLocal := {},
    publish := {}
  )

  lazy val root = Project("root", base = file(".")).settings(baseSettings: _*).settings(
    publishLocal := {},
    publish := {}
  ).aggregate(plugin, sampleProject)



}