package vitrin.runtime

import vitrin.runtime.config.TypesafeConfig

import akka.actor.ActorSystem

import com.typesafe.config.{Config => TSConfig}

import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoConnection.ParsedURI
import reactivemongo.api.MongoConnectionOptions
import reactivemongo.api.MongoDriver
import reactivemongo.core.nodeset.Authenticate

object MongoEnvironment {
	case class ConfigOption(val v: String) extends AnyVal
	case class Config(val v: TSConfig) extends AnyVal
	case class VitrinMongoConnection(connection: MongoConnection, dbName: Option[String])
}

trait MongoEnvironment {
	private val systemConfig = TypesafeConfig.akkaConfig("mongo-system")
	private implicit val system = ActorSystem("mongo-system", systemConfig)
	implicit val mongoConfig = MongoEnvironment.Config(TypesafeConfig().getConfig("mongo"))

 	private object MongoConnectionBuilder {
    	import scala.collection.JavaConverters._

	    implicit private def ConfigString(config: TSConfig, key: String): String = config.getString(key)
	    implicit private def ConfigInt(config: TSConfig, key: String): Int = config.getInt(key)
	    implicit private def ConfigBoolean(config: TSConfig, key: String): Boolean = config.getBoolean(key)
	    implicit private def ConfigStringList(config: TSConfig, key: String): List[String] = config.getStringList(key).asScala.toList
	    implicit private def ConfigConfigList(config: TSConfig, key: String): List[TSConfig] = config.getConfigList(key).asScala.toList

		private def getConfig[A](key: String)(implicit configGetter: (TSConfig, String) => A, config: MongoEnvironment.Config): Option[A] =
			if (config.v.hasPath(key)) Some(configGetter(config.v, key))
			else None

		private def getConfig[A](config: TSConfig, key: String)(implicit configGetter: (TSConfig, String) => A): Option[A] =
			getConfig(key)(configGetter, MongoEnvironment.Config(config))

		def build(): MongoEnvironment.VitrinMongoConnection = {
			val dbO: Option[String] = getConfig("db")
			val userO: Option[String] = getConfig("user")
			val passwordO: Option[String] = getConfig("password")
			val connectTimeoutMS: Int = getConfig[Int]("connectTimeoutMS").getOrElse(0)
			val authSource: Option[String] = getConfig("authSource")
			val tcpNoDelay: Boolean = getConfig[Boolean]("tcpNoDelay").getOrElse(true)
			val keepAlive: Boolean = getConfig[Boolean]("keepAlive").getOrElse(true)
			val nbChannelsPerNode: Int = getConfig[Int]("nbChannelsPerNode").getOrElse(10)

			val hosts = getConfig[List[TSConfig]]("hosts").getOrElse(List()).flatMap { c =>
				for {
					host <- getConfig[String](c, "host")
					port <- getConfig[Int](c, "port")
				} yield (host, port)
			}

			val ignoredOptions = getConfig[List[String]]("ignoredOptions").getOrElse(List())

			val options = MongoConnectionOptions(
				connectTimeoutMS = connectTimeoutMS,
				authSource = authSource,
				tcpNoDelay = tcpNoDelay,
				keepAlive = keepAlive,
				nbChannelsPerNode = nbChannelsPerNode
			)

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
				options = options
			)

			val connection = (new MongoDriver).connection(parameters)

			MongoEnvironment.VitrinMongoConnection(connection, dbO)
		}
	}

	lazy val mongo = MongoConnectionBuilder.build()
}