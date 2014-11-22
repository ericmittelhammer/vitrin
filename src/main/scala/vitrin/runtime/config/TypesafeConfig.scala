package vitrin.runtime.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigException

object TypesafeConfig {
	private val config = ConfigFactory.load

	def apply() = config

	def akkaConfig(systemName: String) = try {
		config.getConfig(systemName).withFallback(akkaLoggingOff)
	} catch {
		case e: ConfigException => akkaLoggingOff
	}

	val akkaLoggingOff = ConfigFactory.parseString("akka.loglevel=\"OFF\"").withFallback(config)
}

class TypesafeConfig extends Config {
	def get(path: String) = try {
		Some(TypesafeConfig().getString(path))
	} catch {
		case _: ConfigException => None
	}
}