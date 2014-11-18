name := "vitrin"

version := "0.0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.9",
	"com.typesafe.akka" % "akka-http-core-experimental_2.11" % "0.9",
	"com.typesafe.play" %% "play-json" % "2.3.1",
	"ch.qos.logback" % "logback-classic" % "1.1.2",
	"org.scalaz" %% "scalaz-core" % "7.0.6",
	"com.typesafe" % "config" % "1.2.1"
)