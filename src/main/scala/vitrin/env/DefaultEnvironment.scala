package vitrin.env

import logging.Logging
import config.ConfigRuntime

import scalaz._

trait DefaultEnvironment extends Environment with Logging {

	trait Context {
		val config: ConfigRuntime
	}
	val context: Context

	type Env[+A] = ReaderWriterState[Context, Log, Unit, A]

	def trace[A](msg: String): Env[Unit] = ReaderWriterState { (ctx, state) => (Log(Trace(msg)), (), state) }
	def debug[A](msg: String): Env[Unit] = ReaderWriterState { (ctx, state) => (Log(Debug(msg)), (), state) }
	def info[A](msg: String): Env[Unit] = ReaderWriterState { (ctx, state) => (Log(Info(msg)), (), state) }
	def warn[A](msg: String): Env[Unit] = ReaderWriterState { (ctx, state) => (Log(Warning(msg)), (), state) }
	def err[A](msg: String): Env[Unit] = ReaderWriterState { (ctx, state) => (Log(Error(msg)), (), state) }

	def withContext[A](f: Context => A)(implicit lm: Monoid[Log]): Env[A] = ReaderWriterState {
		(context, state) => (lm.zero, f(context), state)
	}

	def run[A](env: Env[A]): A = {
		val (log, value, _) = env.run(context, ())
		runLog(log)
		value
	}

	def getConfig(path: String)(implicit lm: Monoid[Log]): Env[Option[String]] = withContext(_.config.get(path))

}