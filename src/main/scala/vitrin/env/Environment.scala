package vitrin.env

trait Environment {
	type Env[+A]
	def run[A](env: Env[A]): A
}