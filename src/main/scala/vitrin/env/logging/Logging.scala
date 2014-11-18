package vitrin.env.logging

import scalaz._

trait Logging {

	sealed trait LogEntry
	case class Trace(msg: String) extends LogEntry
	case class Debug(msg: String) extends LogEntry
	case class Info(msg: String) extends LogEntry
	case class Warning(msg: String) extends LogEntry
	case class Error(msg: String) extends LogEntry

	type Log = List[LogEntry]
	object Log {
		def apply(entry: LogEntry) = List(entry)
	}
	implicit object LogMonoid extends Monoid[Log] {
		def zero = List.empty[LogEntry]
		def append(f1: Log, f2: => Log) = f1 ::: f2
	}

	val logRuntime: LogRuntime
	def runLog(log: Log): Unit =
		log.foreach {
			case Trace(msg) => logRuntime.trace(msg)
			case Debug(msg) => logRuntime.debug(msg)
			case Info(msg) => logRuntime.info(msg)
			case Warning(msg) => logRuntime.warn(msg)
			case Error(msg) => logRuntime.error(msg)
		}

}