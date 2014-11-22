vitrin
======

Vitrin is a very basic HTTP server for running microservices, based on [Akka Streams and HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/0.9/).

To create a new server, extend the ```vitrin.http.Server``` trait like this:
```scala
import vitrin.http.Server
import vitrin.http.routes._
import vitrin.runtime.logging.DefaultLogging
import vitrin.Failure
import akka.http.model._
import HttpMethods._
import redis.RedisClient
import scala.concurrent.Future

object example1 extends Server with ExampleRuntime {
	/**
	  * Name your server.
	  */
	val name = "example1"

	/**
	  * Specify a default execution context for the server.
	  */
	implicit val executionContext = Dispatchers.workers

	/**
	  * The router is a partial function from http requests to processes of http responses.
	  * The pattern starts with an http method, followed by `at`, followed by a path pattern,
	  * that is a string interpolation called p.
	  */
	def router = {
		case GET at p"/foo/bar/$x" => foobar(x)
		case GET at p"/" => index
		case GET at p"/failure" => failure
	}

	/**
	  * This controller method reads the value of the config key `foo.bar`, assignes it to
	  * `msg` with a default value, then logs an info message, then retrieves the value of
	  * the `foo` key from Redis and assignes it to `foo` with a default value, and returns
	  * an http response.
	  */
	def foobar(x: String) = for {
		prefix <- config("foo.bar")
		msg = prefix.getOrElse("I don't know what's happening but")
		_ <- info(s"$msg $x")
		foo <- redisGet("foo")
		fooMsg = foo.getOrElse("not much else")
	} yield HttpResponse(entity = s"$msg $x and $fooMsg")

	/**
	  * This controller method logs an info message and returns an http response.
	  */
	def index = for {
		_ <- info("index page accessed")
	} yield HttpResponse(entity = "index")

	/**
	  * This controller method logs an error message, throws an error, and returns
	  * an empty http response.
	  */
	def failure = for {
		_ <- err("intentional error occured big time")
		_ <- error("intentional error")
	} yield HttpResponse()

	/**
	  * The error handler is a partial function from failures to futures of http responses.
	  * This error handlers catches all failures, like the one thrown in the previous method,
	  * and replaces the original response with a 500 response and a message.
	  */
	override def errorHandler = {
		case Failure(error) => Future.successful(HttpResponse(500, entity = s"Something went wrong: ${error.message}"))
	}

}
```
The above ```example1``` server has a ```Runtime``` implementation mixed in, that extends the ```DefaultRuntime``` provided by the library. A runtime is where all methods, that interact with the environment (except for logging), can be defined. The following example adds a new method to the default runtime, that can read values from Redis:
```scala
import vitrin.runtime.DefaultRuntime
import vitrin.runtime.DefaultEnvironment
import vitrin.runtime.logging.DefaultLogging
import vitrin.ReadWrite
import vitrin.Success
import scala.concurrent.ExecutionContext

trait ExampleRuntime extends DefaultRuntime with DefaultLogging {
	/**
	  * Set the exact type of the environment. DefaultEnvironment is provided by the library,
	  * and contains the config.
	  */
	type Env = DefaultEnvironment with RedisEnvironment

	/**
	  * Instantiate the environment.
	  */
	val environment = new DefaultEnvironment with RedisEnvironment

	/**
	  * This helper method reads a value from the environment. It reads a key from Redis and
	  * lifts it to a process of an option of a string, so that it can be used flatmapped onto
	  * other computations, and thus can be used in for comprehensions.
	  */
	def redisGet(key: String)(implicit ec: ExecutionContext): Process[Option[String]] =
		ReadWrite.read { env => env.redis.get[String](key).map(Success(_)) }
}
```
The type of the environment used by the runtime is set to be ```DefaultEnvironment with RedisEnvironment```, where ```RedisEnvironment``` defines how to connect to Redis:
```scala
import vitrin.runtime.config.TypesafeConfig
import akka.actor.ActorSystem
import redis.RedisClient

trait RedisEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("redis")
	private implicit val system = ActorSystem("redis", systemConfig)
	val redis = RedisClient()
}
```
The ```example1``` server above used an object that contained all configured execution contexts:
```scala
import vitrin.runtime.config.TypesafeConfig
import akka.actor.ActorSystem

object Dispatchers {
	private val system = ActorSystem("dispatchers", TypesafeConfig.akkaLoggingOff)
	val workers = system.dispatchers.lookup("workers-context")
}
```
The ```workers``` dispatcher is configured in the configuration file, that also contains the previously used ```foo.bar``` value:
```
workers-context {
	executor = "fork-join-executor"
	fork-join-executor {
		parallelism-min = 5
	    parallelism-max = 5
	}
}

foo.bar = "foobaring with"
```
Vitrin currently uses Akka Stream 0.9, Akka Http Core 0.9, [Typesafe Config 1.2.1](https://github.com/typesafehub/config) and [Slf4j Api 1.7.7](http://www.slf4j.org/).
