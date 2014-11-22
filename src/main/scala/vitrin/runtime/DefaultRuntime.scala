package vitrin.runtime

import logging.Logging

import vitrin.ReadWrite
import vitrin.Monoid
import vitrin.Result
import vitrin.Success
import vitrin.Error

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait DefaultRuntime extends Runtime {
	self: Logging =>

	type Env <: DefaultEnvironment
	val environment: Env

	type Process[+A] = ReadWrite[Env, Log, A]

	object Process {
		def read[A](f: Env => Future[Result[A]])(implicit ec: ExecutionContext) = ReadWrite.read[Env, Log, A](f)
		def write(log: Log)(implicit lm: Monoid[Log]) = ReadWrite.write[Env, Log](log)
		def error(msg: String, cs: Option[Error] = None) = ReadWrite.error[Env, Log](msg, cs)
	}

	def run[A](process: Process[A])(implicit ec: ExecutionContext): Future[Result[A]] = {
		val result = process.run(environment)
		result onSuccess {
			case (log, _) => runLog(log)
		}
		result.map {
			case (_, value) => value
		}
	}

	val name: String

	def trace(msg: String)(implicit lm: Monoid[Log]): Process[Unit] = Process.write(Log(Trace(msg)))
	def debug(msg: String)(implicit lm: Monoid[Log]): Process[Unit] = Process.write(Log(Debug(msg)))
	def info(msg: String)(implicit lm: Monoid[Log]): Process[Unit] = Process.write(Log(Info(msg)))
	def warn(msg: String)(implicit lm: Monoid[Log]): Process[Unit] = Process.write(Log(Warn(msg)))
	def err(msg: String)(implicit lm: Monoid[Log]): Process[Unit] = Process.write(Log(Err(msg)))

	def config(path: String)(implicit lm: Monoid[Log], ec: ExecutionContext): Process[Option[String]] =
		Process.read { env => Future.successful(Success(env.config.get(path))) }

}