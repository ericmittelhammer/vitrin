vitrin
======

Vitrin is a very basic HTTP server for running microservices, based on [Akka Streams and HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/0.11/).

To create a new server, extend the ```vitrin.http.Server``` trait like this:
```scala
import vitrin.http.Server
import vitrin.http.routes._
import vitrin.Failure
import vitrin.runtime.DefaultEnvironment
import vitrin.runtime.RedisEnvironment
import akka.util.ByteString
import akka.http.model._
import HttpMethods._
import scala.concurrent.Future

object example1 extends Server {
	/**
	  * Name your server.
	  */
	val name = "example1"

	/**
	  * Specify the runtime and import it.
	  */
	val runtime = new ExampleRuntime(name, new DefaultEnvironment with RedisEnvironment)
	import runtime._

	/**
	  * Specify a default execution context for the server.
	  */
	implicit val executionContext = system.dispatchers.lookup("service")

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
		foo <- withRedis { redis => redis.get[String]("foo") }
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
		_ <- Process.error("intentional error")
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
The above ```example1``` server has a member of type ```Runtime```, that is set to a ```ExampleRuntime``` instance, a class extending ```DefaultRuntime```, provided by the library. A runtime is where all methods, that interact with the environment in a side effecting fashion, can be defined. The following example adds a new method to the default runtime, that can read from and write to Redis:
```scala
import vitrin.runtime.DefaultRuntime
import vitrin.runtime.DefaultEnvironment
import vitrin.runtime.logging.DefaultLogging
import vitrin.Failure
import vitrin.Error
import redis.RedisCommands
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class ExampleRuntime[Env <: DefaultEnvironment with RedisEnvironment](val name: String, val environment: Env)
	extends DefaultRuntime[Env]
	with DefaultLogging {

	def stop = {
		environment.stopRedis
	}
	
	/**
	  * This helper method takes a command against a Redis connection, executes it, and lifts
	  * the result to a process of the return type, so that it can be flatmapped onto other
	  * computations, and thus can be used in for comprehensions, as seen in the previous example.
	  */
	def withRedis[A](fn: RedisCommands => Future[A])(implicit ec: ExecutionContext): Process[A] =
		Process.read { env =>
			fn(env.redis).map(Success(_)).recover {
				case e: Throwable => Failure(Error(e.getMessage))
			}
		}
}
```
The type of the environment used by the runtime is set to be ```DefaultEnvironment with RedisEnvironment```, where ```RedisEnvironment``` defines how to connect to Redis:
```scala
import vitrin.runtime.config.TypesafeConfig
import akka.actor.ActorSystem
import redis.RedisClient

trait RedisEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("redis-system")
	private implicit val system = ActorSystem("redis-system", systemConfig)
	val redis = RedisClient()
	
	def stopRedis = system.shutdown
}
```
This example uses the [rediscala](https://github.com/etaty/rediscala) driver. In reality, such a Redis runtime and environment is provided by the library (see examples [here](https://github.com/privateblue/vitrin-example)), and the code above is just to demonstrate how the default runtime and environment can be extended in a client application.

The ```service``` execution context is configured in the configuration file, that also contains the previously used ```foo.bar``` value. It is possible to configure here the default execution context of every actor system, that is used by the application, like the server's, that listens to requests and responds to them, or the one used by rediscala:
```
example1-server-system {
	akka.actor.default-dispatcher {
		executor = "fork-join-executor"
		fork-join-executor {
			parallelism-min = 1
			parallelism-max = 1
		}
	}
}

redis-system {
	akka.actor.default-dispatcher {
		executor = "fork-join-executor"
		fork-join-executor {
			parallelism-min = 1
			parallelism-max = 1
		}
	}
}

service {
	executor = "fork-join-executor"
	fork-join-executor {
		parallelism-min = 2
	    parallelism-max = 2
	}
}

foo.bar = "foobaring with"
```
The above examples can be found in the [vitrin-example](https://github.com/privateblue/vitrin-example) repository.

Vitrin currently uses Akka Stream 0.11, Akka Http Core 0.11, [Typesafe Config 1.2.1](https://github.com/typesafehub/config) and [Slf4j Api 1.7.7](http://www.slf4j.org/).
