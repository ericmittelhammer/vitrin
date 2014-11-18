package vitrin.env.config

trait ConfigRuntime {
	def get(path: String): Option[String]
}