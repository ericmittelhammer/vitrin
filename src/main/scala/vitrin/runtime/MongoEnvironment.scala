package vitrin.runtime

import akka.actor.ActorSystem

import com.typesafe.config.Config

import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoConnection.ParsedURI
import reactivemongo.api.MongoConnectionOptions
import reactivemongo.api.MongoDriver
import reactivemongo.core.nodeset.Authenticate

import scala.concurrent.ExecutionContext

import vitrin.runtime.config.TypesafeConfig


object MongoEnvironment {
  private[MongoEnvironment] case class ConfigOption(val v: String) extends AnyVal
  case class VitrinMongoConnection(connection: MongoConnection, dbName: Option[String])
}

trait MongoEnvironment {
  private val systemConfig = TypesafeConfig.akkaConfig("mongo-system")
  private implicit val system = ActorSystem("mongo-system", systemConfig)
  implicit val ec: ExecutionContext
  
   
  private object MongoConnectionBuilder {
    
    import scala.collection.JavaConverters._

    implicit def ConfigString(key: MongoEnvironment.ConfigOption)(implicit config: Config): String = config.getString(key.v)
    implicit def ConfigInt(key: MongoEnvironment.ConfigOption)(implicit config: Config): Int = config.getInt(key.v)
    implicit def ConfigBoolean(key: MongoEnvironment.ConfigOption)(implicit config: Config): Boolean = config.getBoolean(key.v)
    implicit def ConfigStringList(key: MongoEnvironment.ConfigOption)(implicit config: Config): List[String] = config.getStringList(key.v).asScala.toList
    implicit def ConfigConfigList(key: MongoEnvironment.ConfigOption)(implicit config: Config): List[Config] = config.getConfigList(key.v).asScala.toList

    private def getConfig[A](key: String)(implicit configGetter: MongoEnvironment.ConfigOption => A, config: Config): Option[A] = {
      if (systemConfig.hasPath(key)) {
        Some(configGetter(MongoEnvironment.ConfigOption(key)))
      } else {
        None
      }
    }
    
    def build(): MongoEnvironment.VitrinMongoConnection = {
      implicit val config = systemConfig
      val dbO: Option[String] = getConfig("mongo.db")
      val userO: Option[String] = getConfig("mongo.user")
      val passwordO: Option[String] = getConfig("mongo.password")
      
      val connectTimeoutMS: Int = getConfig[Int]("mongo.connectTimeoutMS").getOrElse(0)
      val authSource: Option[String] = getConfig("mongo.authSource")
      val tcpNoDelay: Boolean = getConfig[Boolean]("mongo.tcpNoDelay").getOrElse(true)
      val keepAlive: Boolean = getConfig[Boolean]("mongo.keepAlive").getOrElse(true)
      val nbChannelsPerNode: Int = getConfig[Int]("mongo.nbChannelsPerNode").getOrElse(10)
      
      val hosts = getConfig[List[Config]]("mongo.hosts").getOrElse(List()).flatMap { c =>
        implicit val config = c 
        for {
          host <- getConfig[String]("host")
          port <- getConfig[Int]("port")
        } yield (host, port)
      }
      
      val ignoredOptions = getConfig[List[String]]("mongo.ignoredOptions").getOrElse(List())
      val options = MongoConnectionOptions(
          connectTimeoutMS = connectTimeoutMS,
          authSource = authSource,
          tcpNoDelay = tcpNoDelay,
          keepAlive = keepAlive,
          nbChannelsPerNode = nbChannelsPerNode)
      
      val authenticate = for {
        db <- dbO
        user <- userO
        password <- passwordO
      } yield Authenticate(user = user, password = password, db = db)
      
      val parameters = ParsedURI(
          authenticate = authenticate,
          db = dbO,
          hosts = hosts,
          ignoredOptions = ignoredOptions,
          options = options)
      val connection = (new MongoDriver).connection(parameters)
      MongoEnvironment.VitrinMongoConnection(connection, dbO)
    }
    
  }
  
  val mongo = MongoConnectionBuilder.build()
  
}