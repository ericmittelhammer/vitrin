package vitrin

sealed trait Result[+A] {
	def map[B](f: A => B): Result[B]
	def flatMap[B](f: A => Result[B]): Result[B]
}

case class Success[+A](value: A) extends Result[A] {
	def map[B](f: A => B) = Success(f(value))
	def flatMap[B](f: A => Result[B]) = f(value)
}

case class Failure[+A](error: Error) extends Result[A] {
	def map[B](f: A => B) = this.asInstanceOf[Result[B]]
	def flatMap[B](f: A => Result[B]) = this.asInstanceOf[Result[B]]
}
