name := "vitrin"

version := "0.0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.9",
	"com.typesafe.akka" % "akka-http-core-experimental_2.11" % "0.9",
	"com.typesafe.play" %% "play-json" % "2.3.1"
)