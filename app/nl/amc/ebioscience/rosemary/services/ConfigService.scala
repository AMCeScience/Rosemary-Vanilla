package nl.amc.ebioscience.rosemary.services

import javax.inject._
import play.api.Configuration
import play.api.Logger
import scala.util.Try
import scala.reflect.runtime.universe.TypeTag

trait ConfigService {

  // Values that final classes will define:
  // Play API Configuration, usually injected
  protected val configuration: Configuration
  // The high-level path
  protected val subconfigPath: String
  // Required keys that should be present in the sub-configuration (defined by high-level path)
  protected val requiredKeys: Set[String]

  // Implementation

  private val subconfiguration = configuration.getConfig(subconfigPath).getOrElse(Configuration.empty)

  // Check to see all required keys are present in the sub-configuration (defined by high-level path)
  private val diffKeys = requiredKeys diff subconfiguration.keys
  if (diffKeys.nonEmpty) {
    val msg = "Not all configuration keys are present in 'application.conf'." +
      s"Try defining the following keys under '$subconfigPath': ${diffKeys.mkString(" , ")}"
    Logger.error(msg)
    throw ConfigurationException(msg)
  }

  private def getConfig[T](key: String, f: => Option[T])(implicit evT: TypeTag[T]): T = Try(f.get).recover {
    case _ =>
      val msg = s"Configuration key '$key' of type '${evT.tpe}' is not defined in 'application.conf' under '$subconfigPath'"
      Logger.error(msg)
      throw ConfigurationException(msg)
  }.get

  def getStringConfig(key: String) = getConfig(key, subconfiguration.getString(key, None))
  def getStringListConfig(key: String) = getConfig(key, subconfiguration.getStringList(key))
  def getIntConfig(key: String) = getConfig(key, subconfiguration.getInt(key))
  def getBooleanConfig(key: String) = getConfig(key, subconfiguration.getBoolean(key))
}

case class ConfigurationException(message: String) extends Exception(message)

@Singleton
class RosemaryConfigService @Inject() (injectedConfiguration: Configuration) extends ConfigService {
  protected lazy val configuration = injectedConfiguration
  protected lazy val subconfigPath = "rosemary"

  // Define the set of keys that application check for their existence when the application starts.
  protected lazy val requiredKeys = Set(
    "crypto.key",
    "webdav.host.default",
    "processingmanager.uri",
    "processingmanager.default.userid",
    "processingmanager.default.projectid")
}
