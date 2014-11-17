package vitrin

import http.Server
import http.extractors._
import logging.Slf4jLoggerRuntime

import akka.http.model._
import HttpMethods._

import scala.concurrent.Future

object testserver extends Server {

	val loggerRuntime = new Slf4jLoggerRuntime(this.getClass().getName())

	def router = {
		case GET at p"/foo/bar/$x/lofasz" => foobar(x)
		case GET at p"/" => index
		case GET at p"/error" => error
	}

	def foobar(x: String) = for {
		_ <- info(s"foobaring with $x")
	} yield HttpResponse(entity = x)

	def index = for {
		_ <- info("index page accessed")
	} yield HttpResponse(entity = "index")

	def error = for {
		_ <- err("intentional error occured big time")
	} yield throw new Exception("intentional error")

}