package vitrin.runtime.config

trait ConfigRuntime {
	def get(path: String): Option[String]
}