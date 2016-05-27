package nl.amc.ebioscience.rosemary.controllers

import javax.inject._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import nl.amc.ebioscience.rosemary.models.User
import play.api.cache.Cache
import play.api.data.Form
import play.api.data.Forms.{ email, mapping, nonEmptyText }
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{ Action, BodyParser, Controller, Cookie, DiscardingCookie, Request, Result }
import play.api.Logger
import play.api.mvc._
import play.mvc.Http
import play.api.i18n.{ MessagesApi, Messages, Lang }
import play.api.cache.CacheApi
import play.api.Configuration

trait Security { self: Controller =>

  //  implicit val app: play.api.Application = play.api.Play.current

  val AuthTokenHeader = "X-XSRF-TOKEN"
  val AuthTokenCookieKey = "XSRF-TOKEN"

  private def auth[A, R](request: Request[A], f: Request[A] => R, r: Result => R): R = {
    auth(request.headers) match {
      case Left(error) => r(Unauthorized(Json.obj("err" -> error)))
      case Right(userid) => User.current_id.withValue(Some(userid)) {
        Logger.trace("AUTHID: " + userid)
        f(request)
      }
    }
  }

  def getUserFromToken(token: String): Either[String, User.Id]

  private def auth(headers: Headers): Either[String, User.Id] = {
    headers.get(AuthTokenHeader) match {
      case Some(token) => getUserFromToken(token)
      case None => {
        readBasicAuthentication(headers) match {
          case Some(Left(error)) => Left(error)
          case Some(Right(credentials)) => {
            val (user, password) = credentials
            User.authenticate(user, password) match {
              case Some(user) => Right(user.id)
              case None       => Left("Incorrect user and/or password")
            }
          }
          case None => Left("No valid authentication found")
        }
      }
    }
  }

  def HasToken[A](p: BodyParser[A] = parse.anyContent)(f: Request[A] => Result): Action[A] =
    Action(p) { implicit request =>
      auth[A, Result](request, f, (r: Result) => r)
    }

  def HasTokenAsync[A](p: BodyParser[A] = parse.anyContent)(f: Request[A] => Future[Result]): Action[A] =
    Action.async(p) { implicit request =>
      auth[A, Future[Result]](request, f, (r: Result) => Future(r))
    }

  private def readBasicAuthentication(headers: Headers): Option[Either[String, (String, String)]] = {
    headers.get(Http.HeaderNames.AUTHORIZATION).map { header =>
      val BasicHeader = "Basic (.*)".r
      header match {
        case BasicHeader(base64) => {
          import org.apache.commons.codec.binary.Base64
          val decodedBytes = Base64.decodeBase64(base64.getBytes)
          val credentials = new String(decodedBytes).split(":", 2)
          if (credentials.length != 2) {
            Left("Invalid basic authentication")
          } else {
            val (user, password) = (credentials(0), credentials(1))
            Right((user, password))
          }
        }
        case _ => Left("Invalid Authorization header")
      }
    }
  }
}

@Singleton
class Authenticate @Inject() (configuration: Configuration, messagesApi: MessagesApi, cacheApi: CacheApi)
    extends Security with Controller {

  implicit def messages(implicit lang: Lang) = new Messages(lang, messagesApi)
  
  def getUserFromToken(token: String) = {
    Logger.trace(Thread.currentThread().getId() + " || Token: " + token + " User: " + cacheApi.get[User.Id](token).toString)
    cacheApi.get[User.Id](token) match {
      case Some(user) => Right(user)
      case None       => Left("Invalid token")
    }
  }

  lazy val CacheExpiration =
    configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 2 /* minutes */ ).seconds

  case class Login(email: String, password: String)

  val loginForm = Form(
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
