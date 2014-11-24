package vitrin.runtime

import vitrin.runtime.config.TypesafeConfig
import akka.actor.ActorSystem
import redis.RedisClient

trait RedisEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("redis")
	private implicit val system = ActorSystem("redis", systemConfig)
	val redis = RedisClient()
}