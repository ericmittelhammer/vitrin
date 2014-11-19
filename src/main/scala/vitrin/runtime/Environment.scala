package vitrin.runtime

trait Environment {

	type Context = DefaultContext

	val context: Context

}