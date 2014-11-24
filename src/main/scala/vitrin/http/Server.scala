package vitrin.http

import vitrin.runtime.Runtime
import vitrin.Result
import vitrin.Success
import vitrin.Failure
import vitrin.runtime.config.TypesafeConfig
import vitrin.runtime.logging.Logging

import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.AskTimeoutException

import akka.io.IO
import akka.http.Http
import akka.http.model._
import HttpMethods._

import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.FlowMaterializer

import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait Server {
	self: Runtime with Logging =>

	val name: String

	private def systemName = s"$name-server-system"
	private lazy val akkaConfig = TypesafeConfig.akkaConfig(systemName)
	private lazy implicit val system = ActorSystem.create(
		name = systemName,
		config = akkaConfig)

	implicit val executionContext: ExecutionContext

	def main(args: Array[String]) = {
		val interface = "localhost"
		val port = portFrom(args)
		implicit val askTimeout: Timeout = 500.millis
		val binding = IO(Http) ? Http.Bind(interface = interface, port = port)
		binding.onSuccess {
			case Http.ServerBinding(_, connectionStream) =>
				connectionHandler(connectionStream)
				logger.info(s"Starting server at $interface:$port")
		}
		binding.onFailure {
			case e: AskTimeoutException =>
				logger.error(s"An error occured while starting server at $interface:$port: ${e.getMessage}")
		}
	}

	private def portFrom(args: Array[String]): Int = {
		val arg = args.headOption.flatMap {
			case p if p.toInt > 0 && p.toInt < 65536 => Some(p.toInt)
			case _ => None
		}
		arg.getOrElse(8080)
	}

	private def connectionHandler(connectionStream: Publisher[Http.IncomingConnection]) = {
		implicit val materializer = FlowMaterializer()
		Source(connectionStream).foreach {
			case Http.IncomingConnection(remoteAddress, requestProducer, responseConsumer) =>
				Source(requestProducer).mapAsyncUnordered(requestHandler).to(Sink(responseConsumer)).run()
		}
	}

	private def requestHandler =
		router andThen run andThen (_ flatMap resultHandler) orElse notFoundRouter andThen (_ recoverWith exceptionHandler)

	def router: PartialFunction[HttpRequest, Process[HttpResponse]]

	private def resultHandler = successHandler orElse errorHandler

	private def successHandler: PartialFunction[Result[HttpResponse], Future[HttpResponse]] = {
		case Success(value) => Future.successful(value)
	}

	def errorHandler: PartialFunction[Result[HttpResponse], Future[HttpResponse]] = {
		case Failure(error) => Future.successful(HttpResponse(500))
	}

	protected def notFoundRouter: PartialFunction[HttpRequest, Future[HttpResponse]] = {
		case _ => Future.successful(HttpResponse(404))
	}

	private def exceptionHandler: PartialFunction[Throwable, Future[HttpResponse]] = {
		case e: Throwable => Future.successful(HttpResponse(500))
	}

}