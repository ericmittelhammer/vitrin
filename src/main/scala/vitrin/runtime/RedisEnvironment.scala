package vitrin.runtime

import vitrin.runtime.config.TypesafeConfig
import akka.actor.ActorSystem
import redis.RedisClient

trait RedisEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("redis-system")
	private implicit val system = ActorSystem("redis-system", systemConfig)
	val redis = RedisClient()

	def stopRedis = system.shutdown
}