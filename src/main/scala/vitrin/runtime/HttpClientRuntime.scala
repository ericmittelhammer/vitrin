package vitrin.runtime

import vitrin.runtime.logging.Logging

import akka.http.model.HttpResponse
import akka.http.model.HttpEntity

import scala.concurrent.ExecutionContext

trait HttpClientRuntime[Env <: DefaultEnvironment with HttpClientEnvironment] extends DefaultRuntime[Env] {
	self: Logging =>

	def httpGet(uri: String)(implicit ec: ExecutionContext): Process[HttpResponse] =
		fromFuture { env => env.http.get(uri) }

	def httpPost(uri: String, body: String)(implicit ec: ExecutionContext): Process[HttpResponse] =
		fromFuture { env => env.http.post(uri, HttpEntity(body)) }
}