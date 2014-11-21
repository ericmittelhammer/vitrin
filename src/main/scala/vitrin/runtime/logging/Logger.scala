package vitrin.runtime.logging

trait Logger {
	def trace(msg: String): Unit
	def debug(msg: String): Unit
	def info(msg: String): Unit
	def warn(msg: String): Unit
	def error(msg: String): Unit
}