package vitrin.http

import extractors._
import vitrin.runtime.Runtime
import vitrin.Result
import vitrin.Success
import vitrin.Failure

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
	self: Runtime =>

	protected implicit val system = ActorSystem("vitrin-server", com.typesafe.config.ConfigFactory.parseString("akka.loglevel=\"OFF\""))

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
				Flow(requestProducer).mapFuture(requestHandler).produceTo(responseConsumer)
		}

	private def requestHandler = router andThen run andThen (_ flatMap resultHandler) orElse notFoundRouter andThen (_ recoverWith exceptionHandler)

	private def successHandler: PartialFunction[Result[HttpResponse], Future[HttpResponse]] = {
		case Success(value) => Future.successful(value)
	}

	private def resultHandler = successHandler orElse errorHandler

	protected def notFoundRouter: PartialFunction[HttpRequest, Future[HttpResponse]] = {
		case _ => Future.successful(HttpResponse(404))
	}

	private def exceptionHandler: PartialFunction[Throwable, Future[HttpResponse]] = {
		case e: Throwable => Future.successful(HttpResponse(500))
	}

	def errorHandler: PartialFunction[Result[HttpResponse], Future[HttpResponse]] = {
		case Failure(error) => Future.successful(HttpResponse(500))
	}

	def router: PartialFunction[HttpRequest, Render[HttpResponse]]

}