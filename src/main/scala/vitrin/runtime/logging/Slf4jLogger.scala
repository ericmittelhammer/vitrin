package vitrin.runtime.logging

import org.slf4j.LoggerFactory

class Slf4jLogger(name: String) extends Logger {
	private lazy val slf4j = LoggerFactory.getLogger(name)

	def trace(msg: String) = slf4j.trace(msg)
	def debug(msg: String) = slf4j.debug(msg)
	def info(msg: String) = slf4j.info(msg)
	def warn(msg: String) = slf4j.warn(msg)
	def error(msg: String) = slf4j.error(msg)
}