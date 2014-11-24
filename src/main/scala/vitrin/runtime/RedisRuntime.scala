package vitrin.runtime

import vitrin.runtime.logging.Logging

import akka.http.model.HttpResponse

import redis.RedisCommands

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait RedisRuntime extends DefaultRuntime {
	self: Logging =>

	type Env <: DefaultEnvironment with RedisEnvironment

	def withRedis[A](fn: RedisCommands => Future[A])(implicit ec: ExecutionContext): Process[A] =
		fromFuture { env => fn(env.redis) }
}