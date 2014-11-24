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
		def get(url: String)(implicit ec: ExecutionContext): Future[HttpResponse] = {
			val uri = new java.net.URI(url)
			val host = uri.getHost
			val port = if (uri.getPort != -1) uri.getPort else 80
			val conn = ask(IO(Http), Http.Connect(host = host, port = port))(500.millis)
			conn.flatMap {
				case Http.OutgoingConnection(_, _, responsePublisher, requestSubscriber) =>
					val request = HttpRequest(GET, uri = s"${uri.getPath}?${uri.getQuery}")
					Source(Iterable((request, ()))).to(Sink(requestSubscriber)).run()
					val responses = Source(responsePublisher).fold(Seq.empty[HttpResponse])((acc, in) => in._1 +: acc)
					responses.map(_.headOption.getOrElse(throw new Exception("No response received")))
				case Status.Failure(e: Throwable) =>
					Future.failed(e)
			}
		}
	}
}