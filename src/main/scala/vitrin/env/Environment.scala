package vitrin.env

import logging.Logging
import config.Config
import config.ConfigRuntime

import scalaz._

trait Environment extends Logging with Config {

	type Env[+A] = ReaderWriterState[ConfigRuntime, Log, Unit, A]

	def trace[A](msg: String): Env[Unit] = ReaderWriterState { (_, _) => (Log(Trace(msg)), (), ()) }
	def debug[A](msg: String): Env[Unit] = ReaderWriterState { (_, _) => (Log(Debug(msg)), (), ()) }
	def info[A](msg: String): Env[Unit] = ReaderWriterState { (_, _) => (Log(Info(msg)), (), ()) }
	def warn[A](msg: String): Env[Unit] = ReaderWriterState { (_, _) => (Log(Warning(msg)), (), ()) }
	def err[A](msg: String): Env[Unit] = ReaderWriterState { (_, _) => (Log(Error(msg)), (), ()) }

	def configured[A](f: ConfigRuntime => A)(implicit lm: Monoid[Log]): Env[A] = ReaderWriterState {
		(config, _) => (lm.zero, f(config), ())
	}

	def getConfig(path: String)(implicit lm: Monoid[Log]): Env[Option[String]] = configured(_.get(path))

	def run[A](env: Env[A]): A = {
		val (log, value, _) = env.run(configRuntime, ())
		runLog(log)
		value
	}

}