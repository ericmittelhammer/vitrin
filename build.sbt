name := "vitrin"

version := "0.0.1"

scalaVersion := "2.11.4"

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.11",
	"com.typesafe.akka" % "akka-http-core-experimental_2.11" % "0.11",
	"com.typesafe" % "config" % "1.2.1",
	"org.slf4j" % "slf4j-api" % "1.7.7",
	"com.etaty.rediscala" %% "rediscala" % "1.4.0"
)