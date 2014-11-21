package vitrin.runtime

import vitrin.Result

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait Runtime {
	type Process[+A]
	def run[A](process: Process[A])(implicit ec: ExecutionContext): Future[Result[A]]
}