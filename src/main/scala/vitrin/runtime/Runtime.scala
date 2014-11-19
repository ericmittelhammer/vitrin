package vitrin.runtime

import vitrin.Result

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait Runtime {
	type Render[+A]
	def run[A](env: Render[A])(implicit ec: ExecutionContext): Future[Result[A]]
}