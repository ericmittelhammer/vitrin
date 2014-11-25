package vitrin.runtime

import reactivemongo.api.Collection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import vitrin.runtime.logging.Logging

trait MongoRuntime extends DefaultRuntime {
  self: Logging =>
    
  type Env <: DefaultEnvironment with MongoEnvironment
  
  def withMongo[A](dbName: String, collectionName: String)(fn: Collection => Future[A])(implicit ec: ExecutionContext) = {
    fromFuture { env =>
      val db = env.mongo.connection.db(dbName)
      val collection = db(collectionName)
      fn(collection)
    }
  }
  
  def withMongo[A](collectionName: String)(fn: Collection => Future[A])(implicit ec: ExecutionContext) = {
    fromFuture { env =>
      val dbName = env.mongo.dbName.getOrElse(throw new MongoRuntime.DatabaseMissingException("There is no default mongo database"))
      val db = env.mongo.connection(dbName)
      val collection = db(collectionName)
      fn(collection)
    }
  }
  
}

object MongoRuntime {
  class DatabaseMissingException(msg: String) extends Throwable(msg)
}