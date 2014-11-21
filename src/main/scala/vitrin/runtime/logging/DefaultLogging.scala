package vitrin.runtime.logging

import vitrin.runtime.DefaultRuntime

trait DefaultLogging extends Logging {
	self: DefaultRuntime =>

	val logger = new Slf4jLogger(name)
}