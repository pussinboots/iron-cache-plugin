name      := "sample"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  cache
)

libraryDependencies ++= Seq(
	"org.specs2" %% "specs2-core" % "3.9.0" % "test",
	"org.specs2" %% "specs2-junit" % "3.9.0" % "test"
)