package vitrin.runtime

import vitrin.runtime.logging.Logging

import reactivemongo.api.Collection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object MongoRuntime {
	class DatabaseMissingException(msg: String) extends Throwable(msg)
}

trait MongoRuntime[Env <: DefaultEnvironment with MongoEnvironment] extends DefaultRuntime[Env] {
 	self: Logging =>

	def withMongo[A](dbName: String, collectionName: String)(fn: Collection => Future[A])(implicit ec: ExecutionContext) = fromFuture { env =>
		val db = env.mongo.connection.db(dbName)
		val collection = db(collectionName)
		fn(collection)
	}

	def withMongo[A](collectionName: String)(fn: Collection => Future[A])(implicit ec: ExecutionContext) = fromFuture { env =>
		val dbName = env.mongo.dbName.getOrElse(throw new MongoRuntime.DatabaseMissingException("There is no default mongo database"))
		val db = env.mongo.connection(dbName)
		val collection = db(collectionName)
		fn(collection)
	}

}