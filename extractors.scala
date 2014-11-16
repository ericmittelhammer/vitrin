package vitrin

import akka.http.model._
import HttpMethods._

object extractors {

	object at {
		def unapply(request: HttpRequest): Option[(HttpMethod, String)] =
			Some((request.method, request.uri.path.toString))
	}

	implicit class PathContext(val sc: StringContext) {
		object p {
			def apply(args: Any*): String = sc.s(args:_*)
			def unapplySeq(s: String): Option[Seq[String]] = {
				val regexp = sc.parts.mkString ("([^/]+)").r
				regexp.unapplySeq(s)
			}
		}
	}

}