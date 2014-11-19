package vitrin

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class ReadWrite[Context, Log, +A](run: Context => Future[(Log, Result[A])]) {
	def map[B](f: A => B)(implicit ec: ExecutionContext): ReadWrite[Context, Log, B] = ReadWrite {
		ctx =>
			run(ctx).map {
				case (log, a) => (log, a map f)
			}
	}

	def flatMap[B](f: A => ReadWrite[Context, Log, B])(implicit lm: Monoid[Log], ec: ExecutionContext): ReadWrite[Context, Log, B] = ReadWrite {
		ctx =>
			run(ctx).flatMap {
				case (log1, Success(a)) =>
					f(a).run(ctx).map {
						case (log2, b) => (lm.append(log1, log2), b)
					}
				case (log1, fail) =>
					Future.successful((log1, fail.asInstanceOf[Failure[B]]))
			}
	}
}

object ReadWrite {
	def read[Context, Log, A](f: Context => Future[Result[A]])(implicit lm: Monoid[Log], ec: ExecutionContext): ReadWrite[Context, Log, A] =
		ReadWrite { context => f(context).map((lm.zero, _)) }

	def write[Context, Log](log: Log)(implicit lm: Monoid[Log]): ReadWrite[Context, Log, Unit] =
		ReadWrite { context => Future.successful((lm.append(lm.zero, log), Success())) }
}