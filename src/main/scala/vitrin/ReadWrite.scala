package vitrin

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class ReadWrite[Env, Log, +A](run: Env => Future[(Log, Result[A])]) {
	def map[B](f: A => B)(implicit ec: ExecutionContext): ReadWrite[Env, Log, B] = ReadWrite {
		env =>
			run(env).map {
				case (log, a) => (log, a map f)
			}
	}

	def flatMap[B](f: A => ReadWrite[Env, Log, B])(implicit lm: Monoid[Log], ec: ExecutionContext): ReadWrite[Env, Log, B] = ReadWrite {
		env =>
			run(env).flatMap {
				case (log1, Success(a)) =>
					f(a).run(env).map {
						case (log2, b) => (lm.append(log1, log2), b)
					}
				case (log1, fail) =>
					Future.successful((log1, fail.asInstanceOf[Failure[B]]))
			}
	}
}

object ReadWrite {
	def read[Env, Log, A](f: Env => Future[Result[A]])(implicit lm: Monoid[Log], ec: ExecutionContext): ReadWrite[Env, Log, A] =
		ReadWrite { env => f(env).map((lm.zero, _)) }

	def write[Env, Log](log: Log)(implicit lm: Monoid[Log]): ReadWrite[Env, Log, Unit] =
		ReadWrite { env => Future.successful((lm.append(lm.zero, log), Success())) }

	def error[Env, Log](msg: String, cs: Option[Error] = None)(implicit lm: Monoid[Log]): ReadWrite[Env, Log, Unit] =
		ReadWrite { env => Future.successful((lm.zero, Failure(Error(msg, cs))))}
}