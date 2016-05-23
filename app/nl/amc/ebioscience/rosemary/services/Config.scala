package nl.amc.ebioscience.rosemary.services

import javax.inject._
import play.api.Configuration
import play.api.Logger
import scala.util.Try

trait Config {
  def getConfig(key: String): String
}

case class ConfigurationException(message: String) extends Exception(message)

@Singleton
class RosemaryConfig @Inject() (configuration: Configuration) extends Config {
  private val rosemaryConf = configuration.getConfig("rosemary").getOrElse(Configuration.empty)
  private val requiredKeys = Set(
    "crypto.key")

  private val diffKeys = requiredKeys diff rosemaryConf.keys
  if (diffKeys nonEmpty) throw ConfigurationException("Not all configuration keys are present in 'application.conf'." +
    s"Try defining the following keys under 'rosemary': ${diffKeys.mkString(" , ")}")

  override def getConfig(key: String) = Try(rosemaryConf.getString(key, None).get).recover {
    case _ =>
      Logger.error(s"Key $key is not defined in application.conf under 'rosemary'")
      ""
  }.get
}
