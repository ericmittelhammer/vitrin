package vitrin.env

import logging.Logging
import config.ConfigRuntime

import vitrin.ReadWrite
import vitrin.Monoid

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait DefaultEnvironment extends Environment with Logging {

	trait Context {
		val config: ConfigRuntime
	}
	val context: Context

	type Env[+A] = ReadWrite[Context, Log, A]

	def run[A](env: Env[A])(implicit ec: ExecutionContext): Future[A] = {
		val result = env.run(context)
		result onSuccess {
			case (log, _) => runLog(log)
		}
		result.map {
			case (_, value) => value
		}
	}

	def trace[A](msg: String)(implicit lm: Monoid[Log]): Env[Unit] = ReadWrite.write(Log(Trace(msg)))
	def debug[A](msg: String)(implicit lm: Monoid[Log]): Env[Unit] = ReadWrite.write(Log(Debug(msg)))
	def info[A](msg: String)(implicit lm: Monoid[Log]): Env[Unit] = ReadWrite.write(Log(Info(msg)))
	def warn[A](msg: String)(implicit lm: Monoid[Log]): Env[Unit] = ReadWrite.write(Log(Warning(msg)))
	def err[A](msg: String)(implicit lm: Monoid[Log]): Env[Unit] = ReadWrite.write(Log(Error(msg)))

	def getConfig(path: String)(implicit lm: Monoid[Log], ec: ExecutionContext): Env[Option[String]] =
		ReadWrite.read { ctx => Future.successful(ctx.config.get(path)) }

}