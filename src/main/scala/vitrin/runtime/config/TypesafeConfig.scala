package vitrin.runtime.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigException

class TypesafeConfig extends Config {
	private val config = ConfigFactory.load

	def get(path: String) = try {
		Some(config.getString(path))
	} catch {
		case _: ConfigException => None
	}
}