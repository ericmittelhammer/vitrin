package vitrin.runtime

import vitrin.runtime.logging.Logging

import akka.http.model.HttpResponse

import redis.RedisCommands

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait RedisRuntime[Env <: DefaultEnvironment with RedisEnvironment] extends DefaultRuntime[Env] {
	self: Logging =>

	def withRedis[A](fn: RedisCommands => Future[A])(implicit ec: ExecutionContext): Process[A] =
		fromFuture { env => fn(env.redis) }
}