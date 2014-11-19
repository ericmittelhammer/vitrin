package vitrin

import http.Server
import http.extractors._
import runtime.logging.Logging
import runtime.logging.Slf4jLogRuntime
import runtime.Environment
import runtime.DefaultRuntime
import runtime.DefaultContext
import runtime.config.TypesafeConfigRuntime

import akka.http.model._
import HttpMethods._

import scala.concurrent.Future

object testserver extends Server with DefaultRuntime with Logging with Environment {

	import system.dispatcher

	val context = new DefaultContext {
		val config = new TypesafeConfigRuntime
	}

	val logRuntime = new Slf4jLogRuntime("testserver")

	def router = {
		case GET at p"/foo/bar/$x/lofasz" => foobar(x)
		case GET at p"/" => index
		case GET at p"/failure" => failure
	}

	def foobar(x: String) = for {
		prefix <- config("foo.bar")
		msg = prefix.getOrElse("I don't know what's happening but")
		_ <- info(s"$msg $x")
	} yield HttpResponse(entity = s"$msg $x")

	def index = for {
		_ <- info("index page accessed")
	} yield HttpResponse(entity = "index")

	def failure = for {
		_ <- err("intentional error occured big time")
		_ <- error("intentional error")
	} yield HttpResponse()

}