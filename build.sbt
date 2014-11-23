name := "vitrin"

version := "0.0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.11",
	"com.typesafe.akka" % "akka-http-core-experimental_2.11" % "0.11",
	//"com.typesafe.play" %% "play-json" % "2.3.1",
	"com.typesafe" % "config" % "1.2.1",
	"org.slf4j" % "slf4j-api" % "1.7.7"
)