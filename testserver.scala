package vitrin

import extractors._
import akka.http.model._
import HttpMethods._

import scala.concurrent.Future

object testserver extends Server {
	def router = {
		case GET at p"/foo/bar/$x/lofasz" => HttpResponse(entity = x)
		case GET at p"/" => HttpResponse(entity = "index")
		case GET at p"/error" => throw new Exception("intentional error")
	}
}