package vitrin.http

import extractors._
import vitrin.env.Environment

import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.AskTimeoutException

import akka.io.IO
import akka.http.Http
import akka.http.model._
import HttpMethods._

import akka.stream.scaladsl.Flow
import akka.stream.MaterializerSettings
import akka.stream.FlowMaterializer

import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.concurrent.duration._

trait Server {
	self: Environment =>

	private implicit val system = ActorSystem()
	private implicit val materializer = FlowMaterializer()

	import system.dispatcher

	def main(args: Array[String]) = {
		val interface = "localhost"
		val port = portFrom(args)
		implicit val askTimeout: Timeout = 500.millis
		val binding = IO(Http) ? Http.Bind(interface = interface, port = port)
		binding.onSuccess {
			case Http.ServerBinding(_, connectionStream) =>
				connectionHandler(connectionStream)
				println(s"Starting server at $interface:$port")
		}
		binding.onFailure {
			case e: AskTimeoutException =>
				println(s"An error occured while starting server at $interface:$port: ${e.getMessage}")
		}
	}

	private def portFrom(args: Array[String]): Int = {
		val arg = args.headOption.flatMap {
			case p if p.toInt > 0 && p.toInt < 65536 => Some(p.toInt)
			case _ => None
		}
		arg.getOrElse(8080)
	}

	private def connectionHandler(connectionStream: Publisher[Http.IncomingConnection]) =
		Flow(connectionStream).foreach {
			case Http.IncomingConnection(remoteAddress, requestProducer, responseConsumer) =>
				Flow(requestProducer).map(handler).produceTo(responseConsumer)
		}

	private def handler = (req: HttpRequest) =>
		try { requestHandler(req) }
		catch errorHandler

	private def requestHandler = router andThen run orElse notFoundRouter

	protected def notFoundRouter: PartialFunction[HttpRequest, HttpResponse] = {
		case _ => HttpResponse(404)
	}

	protected def errorHandler: PartialFunction[Throwable, HttpResponse] = {
		case e: Throwable => HttpResponse(500)
	}

	def router: PartialFunction[HttpRequest, Env[HttpResponse]]

}