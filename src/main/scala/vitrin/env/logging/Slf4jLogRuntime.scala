package vitrin.env.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Slf4jLogRuntime(name: String) extends LogRuntime {
	private val logger = LoggerFactory.getLogger(name)

	def trace(msg: String) = logger.trace(msg)
	def debug(msg: String) = logger.debug(msg)
	def info(msg: String) = logger.info(msg)
	def warn(msg: String) = logger.warn(msg)
	def error(msg: String) = logger.error(msg)
}