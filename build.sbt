name := "myproject"

scalaVersion := "2.11.7"

lazy val ironCachePlugin = (project in file("plugin")).enablePlugins(PlayScala)

lazy val sample = (project in file("sample"))
    .enablePlugins(PlayScala).dependsOn(ironCachePlugin).aggregate(ironCachePlugin)