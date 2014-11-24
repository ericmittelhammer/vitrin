package vitrin.runtime

import vitrin.runtime.config.TypesafeConfig

import akka.actor.ActorSystem
import akka.actor.Status
import akka.pattern.ask

import akka.io.IO
import akka.http.Http
import akka.http.model._
import HttpMethods._

import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.FlowMaterializer

import scala.collection.immutable.Iterable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait HttpClientEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("http-client")
	private implicit val system = ActorSystem("http-client", systemConfig)
	private implicit val materializer = FlowMaterializer()

	object http {
		case class Url(address: String) {
			private val uri = new java.net.URI(address)
			val connection = {
				val host = uri.getHost
				val port = if (uri.getPort != -1) uri.getPort else 80
				Http.Connect(host = host, port = port)
			}
			val requestUri = s"${uri.getPath}?${uri.getQuery}"
		}

		def get(address: String)(implicit ec: ExecutionContext): Future[HttpResponse] = {
			val url = Url(address)
			val req = HttpRequest(GET, uri = url.requestUri)
			connect(url) flatMap request(req)
		}

		def post(address: String, body: RequestEntity)(implicit ec: ExecutionContext): Future[HttpResponse] = {
			val url = Url(address)
			val req = HttpRequest(POST, uri = url.requestUri, entity = body)
			connect(url) flatMap request(req)
		}

		private def connect(url: Url) = ask(IO(Http), url.connection)(500.millis)

		private def request(req: HttpRequest)(implicit ec: ExecutionContext): Any => Future[HttpResponse] = {
			case Http.OutgoingConnection(_, _, responsePublisher, requestSubscriber) =>
				Source(Iterable((req, ()))).to(Sink(requestSubscriber)).run()
				val responses = Source(responsePublisher).fold(Seq.empty[HttpResponse])((acc, in) => in._1 +: acc)
				responses.map(_.headOption.getOrElse(throw new Exception("No response received")))
			case Status.Failure(e: Throwable) =>
				Future.failed(e)
		}
	}
}