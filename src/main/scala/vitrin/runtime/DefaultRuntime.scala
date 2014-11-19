package vitrin.runtime

import logging.Logging
import config.ConfigRuntime

import vitrin.ReadWrite
import vitrin.Monoid
import vitrin.Result
import vitrin.Success
import vitrin.Failure
import vitrin.Error

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait DefaultRuntime extends Runtime {
	self: Logging with Environment =>

	type Render[+A] = ReadWrite[Context, Log, A]

	def run[A](env: Render[A])(implicit ec: ExecutionContext): Future[Result[A]] = {
		val result = env.run(context)
		result onSuccess {
			case (log, _) => runLog(log)
		}
		result.map {
			case (_, value) => value
		}
	}

	def trace[A](msg: String)(implicit lm: Monoid[Log]): Render[Unit] = ReadWrite.write(Log(Trace(msg)))
	def debug[A](msg: String)(implicit lm: Monoid[Log]): Render[Unit] = ReadWrite.write(Log(Debug(msg)))
	def info[A](msg: String)(implicit lm: Monoid[Log]): Render[Unit] = ReadWrite.write(Log(Info(msg)))
	def warn[A](msg: String)(implicit lm: Monoid[Log]): Render[Unit] = ReadWrite.write(Log(Warn(msg)))
	def err[A](msg: String)(implicit lm: Monoid[Log]): Render[Unit] = ReadWrite.write(Log(Err(msg)))

	def config(path: String)(implicit lm: Monoid[Log], ec: ExecutionContext): Render[Option[String]] =
		ReadWrite.read { ctx => Future.successful(Success(ctx.config.get(path))) }

	def error(msg: String, cs: Option[Error] = None)(implicit lm: Monoid[Log]): Render[Unit] =
		ReadWrite { ctx => Future.successful((lm.zero, Failure(Error(msg, cs))))}

}