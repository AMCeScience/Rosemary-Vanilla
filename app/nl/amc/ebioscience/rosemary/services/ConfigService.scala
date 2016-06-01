package nl.amc.ebioscience.rosemary.services

import javax.inject._
import play.api.Configuration
import play.api.Logger
import scala.util.Try

trait ConfigService {
  def getConfig(key: String): String
}

case class ConfigurationException(message: String) extends Exception(message)

@Singleton
class RosemaryConfigService @Inject() (configuration: Configuration) extends ConfigService {
  private val highlevelKey = "rosemary"
  private val rosemaryConf = configuration.getConfig(highlevelKey).getOrElse(Configuration.empty)

  // Define the set of keys that application check for their existence when the application starts.
  private val requiredKeys = Set(
    "crypto.key",
    "webdav.host.default")

  private val diffKeys = requiredKeys diff rosemaryConf.keys
  if (diffKeys.nonEmpty) {
    val msg = "Not all configuration keys are present in 'application.conf'." +
      s"Try defining the following keys under '$highlevelKey': ${diffKeys.mkString(" , ")}"
    Logger.error(msg)
    throw ConfigurationException(msg)
  }

  override def getConfig(key: String) = Try(rosemaryConf.getString(key, None).get).recover {
    case _ =>
      val msg = s"Configuration key '$key' is not defined in 'application.conf' under '$highlevelKey'"
      Logger.error(msg)
      throw ConfigurationException(msg)
  }.get
}
