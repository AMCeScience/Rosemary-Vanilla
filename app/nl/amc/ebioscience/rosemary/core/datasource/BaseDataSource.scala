package nl.amc.ebioscience.rosemary.core.datasource

import dispatch._
import dispatch.Defaults._
import nl.amc.ebioscience.rosemary.models._
import play.api.Logger

/** General dispatch setup, methods, and configuration */
abstract class BaseDataSource(resource: Resource) {

  /** Credential of the current user for the resource */
  val userCredential: Option[Credential] = User.credentialFor(resource.id)

  val baseUri: String = resource.uri
  val baseReq: Req = url(baseUri)

  Http.configure(_.setAllowPoolingConnection(true).
    setCompressionEnabled(true).
    setConnectionTimeoutInMs(10000).
    setRequestTimeoutInMs(30000))

  /** with the Req.as method, username and password are only sent if the response handler indicates that */
  protected def auth(req: Req) = userCredential map { cred =>
    req.as(cred.username, cred.password)
  } getOrElse {
    Logger.debug(s"${User.current.email} has no credential for ${resource.name}, trying community credentials...")
    val communityCredential = for (user <- resource.username; pass <- resource.password) yield (user, pass)
    communityCredential match {
      case Some(tup) => req.as(tup._1, tup._2)
      case None => {
        Logger.debug(s"There is no community credential for ${resource.name}, trying without any credential...")
        req
      }
    }
  }
}
