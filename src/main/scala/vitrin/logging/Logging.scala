package vitrin.logging

import scalaz._
import Scalaz._

trait Logging {

	sealed trait LogEntry
	case class Trace(msg: String) extends LogEntry
	case class Debug(msg: String) extends LogEntry
	case class Info(msg: String) extends LogEntry
	case class Warning(msg: String) extends LogEntry
	case class Error(msg: String) extends LogEntry

	type Logger[+A] = Writer[List[LogEntry], A]

	def trace(msg: String) = List(Trace(msg)).tell
	def debug(msg: String) = List(Debug(msg)).tell
	def info(msg: String) = List(Info(msg)).tell
	def warn(msg: String) = List(Warning(msg)).tell
	def err(msg: String) = List(Error(msg)).tell

	val loggerRuntime: LoggerRuntime

	def runLogging[A](logger: Logger[A]): A = {
		val (log, value) = logger.run
		log.foreach {
			case Trace(msg) => loggerRuntime.trace(msg)
			case Debug(msg) => loggerRuntime.debug(msg)
			case Info(msg) => loggerRuntime.info(msg)
			case Warning(msg) => loggerRuntime.warn(msg)
			case Error(msg) => loggerRuntime.error(msg)
		}
		value
	}

}