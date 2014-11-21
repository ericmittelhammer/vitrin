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

	def trace[A](msg: String)(implicit lm: Monoid[Log]): Process[Unit] = ReadWrite.write(Log(Trace(msg)))
	def debug[A](msg: String)(implicit lm: Monoid[Log]): Process[Unit] = ReadWrite.write(Log(Debug(msg)))
	def info[A](msg: String)(implicit lm: Monoid[Log]): Process[Unit] = ReadWrite.write(Log(Info(msg)))
	def warn[A](msg: String)(implicit lm: Monoid[Log]): Process[Unit] = ReadWrite.write(Log(Warn(msg)))
	def err[A](msg: String)(implicit lm: Monoid[Log]): Process[Unit] = ReadWrite.write(Log(Err(msg)))

	def config(path: String)(implicit lm: Monoid[Log], ec: ExecutionContext): Process[Option[String]] =
		ReadWrite.read { env => Future.successful(Success(env.config.get(path))) }

	def error(msg: String, cs: Option[Error] = None)(implicit lm: Monoid[Log]): Process[Unit] =
		ReadWrite.error(msg, cs)

}