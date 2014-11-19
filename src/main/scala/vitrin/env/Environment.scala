package vitrin.env

import vitrin.Result

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait Environment {
	type Env[+A]
	def run[A](env: Env[A])(implicit ec: ExecutionContext): Future[Result[A]]
}