package vitrin.runtime

import vitrin.runtime.logging.Logging

import akka.http.model.HttpResponse

import scala.concurrent.ExecutionContext

trait HttpClientRuntime extends DefaultRuntime {
	self: Logging =>

	type Env <: DefaultEnvironment with HttpClientEnvironment

	def httpGet(uri: String)(implicit ec: ExecutionContext): Process[HttpResponse] =
		fromFuture { env => env.http.get(uri) }
}