package vitrin.http

import akka.http.model._
import HttpMethods._
import scala.util.matching.Regex
import vitrin._

class Routing[F[_]](routes: (String, Regex => HttpRequest => F[HttpResponse])*) {

	private case class Route(path: List[String], handler: Regex => HttpRequest => F[HttpResponse])

	private case class Tree(nodes: Map[String, Tree], regex: String, handler: Option[Regex => HttpRequest => F[HttpResponse]]) {

		def insert(route: Route): Tree = {
			route.path match {
			  	case head :: tail =>
					val regexPart = head match {
						  case "$" => """(^/+)"""
						  case "#" => """(\d+)"""
						  case _ => head
					}
					nodes.get(head) match {
						case Some(subtree) =>
							Tree(nodes.updated(head, subtree.insert(Route(tail, route.handler))), regex, handler)
					  	case None =>
							Tree(nodes + (head -> Tree(Map.empty, (regex + """\/""" + regexPart), None).insert(Route(tail, route.handler))), regex, handler)
					}
			  	case Nil =>
					Tree(nodes, regex, Some(route.handler))
			}
		}

		def matchRoute(path: List[String]): Option[HttpRequest => F[HttpResponse]] =
			path match {
			case Nil =>
				handler.map(_(regex.r))
			case head :: tail =>
				nodes.get(head)
					.orElse(nodes.get("#"))
					.orElse(nodes.get("$"))
					.flatMap(_.matchRoute(tail))
			}

	  override def toString: String = {
		def toString0(path: String, node: Tree): Seq[String] = {
		  val nodeList =
			node.nodes flatMap {
				case (l, n) =>
					toString0(path + "/" + l, n)
			}
		  node.handler match {
			case Some(h) =>
			  path +: nodeList.toSeq
			case None =>
			  nodeList.toSeq
		  }
		}
		toString0("", this).mkString("\n")
	  }

	}

	private val parsedRoutes: Tree = {


	  val splitRoutes = routes map {
		case (path, fn) => Route(path.split('/').toList.drop(1), fn)
	  }

	  splitRoutes.foldLeft(Tree(Map.empty, "", None))(_ insert _)
	}

	val run: PartialFunction[HttpRequest, F[HttpResponse]] = Function unlift { request =>
		parsedRoutes.matchRoute(request.uri.path.toString.split('/').toList.drop(1)).map(_(request))
	}
}
