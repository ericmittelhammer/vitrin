package vitrin.http

import akka.http.model._
import HttpMethods._

object routes {

	object at {
		def unapply(request: HttpRequest): Option[(HttpMethod, String)] =
			Some((request.method, request.uri.path.toString))
	}

	implicit class PathContext(val context: StringContext) {
		object p {
			def apply(parts: Any*): String = context.s(parts:_*)
			def unapplySeq(s: String): Option[Seq[String]] = {
				val pattern = context.parts.mkString("([^/]+)").r
				pattern.unapplySeq(s)
			}
		}
	}

}