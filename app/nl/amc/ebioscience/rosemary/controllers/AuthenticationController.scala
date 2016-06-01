package nl.amc.ebioscience.rosemary.controllers

import javax.inject._
import scala.concurrent.duration._
import nl.amc.ebioscience.rosemary.models.User
import play.api.data.Form
import play.api.data.Forms.{ email, mapping, nonEmptyText }
import play.api.libs.json.Json
import play.api.i18n.{ MessagesApi, Messages, Lang }
import play.api.cache.CacheApi
import play.api.Configuration
import play.api.mvc._

@Singleton
class AuthenticationController @Inject() (configuration: Configuration, messagesApi: MessagesApi, cacheApi: CacheApi)
    extends Controller {

  private implicit def messages(implicit lang: Lang) = new Messages(lang, messagesApi)

  private val AuthTokenHeader = "X-XSRF-TOKEN"
  private val AuthTokenCookieKey = "XSRF-TOKEN"

  private lazy val CacheExpiration =
    configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 2 /* minutes */ ).seconds

  case class Login(email: String, password: String)

  private val loginForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText)(Login.apply)(Login.unapply))

  implicit class ResultWithToken(result: Result) {
    def withToken(token: (String, User.Id)): Result = {
      cacheApi.set(token._1, token._2, CacheExpiration)
      result.withCookies(Cookie(AuthTokenCookieKey, token._1, None, httpOnly = false))
    }

    def discardingToken(token: String): Result = {
      cacheApi.remove(token)
      result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey))
    }
  }

  /** Check credentials, generate token and serve it back as auth token in a Cookie */
  def login = Action(parse.json) { implicit request =>
    loginForm.bind(request.body).fold(
      formErrors => BadRequest(Json.obj("err" -> formErrors.errorsAsJson)),
      loginData => {
        User.authenticate(loginData.email, loginData.password) map { user =>
          val token = java.util.UUID.randomUUID().toString
          Ok(Json.obj(
            "id" -> user.id.toString(),
            "email" -> user.email,
            "name" -> user.name,
            "active" -> user.active,
            "approved" -> user.approved,
            "role" -> user.role.toString)).withToken(token -> user.id)
        } getOrElse NotFound(Json.obj("err" -> "User Not Found or Password Invalid"))
      })
  }

  /** Invalidate the token in the Cache and discard the cookie */
  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
      Redirect("/").discardingToken(token)
    } getOrElse BadRequest(Json.obj("err" -> "No Token"))
  }
}
