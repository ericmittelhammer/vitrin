package vitrin.runtime.logging

trait DefaultLogging extends Logging {
	val logger = new Slf4jLogger("vitrin-logger")
}