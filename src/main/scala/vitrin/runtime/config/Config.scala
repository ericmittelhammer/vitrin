package vitrin.runtime.config

trait Config {
	def get(path: String): Option[String]
}