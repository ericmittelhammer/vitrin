package vitrin

import http.Server
import http.extractors._
import env.logging.Slf4jLogRuntime
import env.config.TypesafeConfigRuntime
import env.DefaultEnvironment

import akka.http.model._
import HttpMethods._

import scala.concurrent.Future

object testserver extends Server with DefaultEnvironment {

	val context = new Context {
		val config = new TypesafeConfigRuntime
	}

	val logRuntime = new Slf4jLogRuntime("testserver")

	def router = {
		case GET at p"/foo/bar/$x/lofasz" => foobar(x)
		case GET at p"/" => index
		case GET at p"/error" => error
	}

	def foobar(x: String) = for {
		prefix <- getConfig("foo.bar")
		msg = prefix.getOrElse("I don't know what's happening but")
		_ <- info(s"$msg $x")
	} yield HttpResponse(entity = s"$msg $x")

	def index = for {
		_ <- info("index page accessed")
	} yield HttpResponse(entity = "index")

	def error = for {
		_ <- err("intentional error occured big time")
	} yield throw new Exception("intentional error")

}