/*
 * Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
 * 
 * This program is semi-free software: you can redistribute it and/or modify it
 * under the terms of the Rosemary license. You may obtain a copy of this
 * license at:
 * 
 * https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * You should have received a copy of the Rosemary license
 * along with this program. If not, 
 * see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
 * 
 *        Project: https://github.com/AMCeScience/Rosemary-Vanilla
 *        AMC eScience Website: http://www.ebioscience.amc.nl/
 */
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
import play.api.Logger

@Singleton
class AuthenticationController @Inject() (configuration: Configuration, messagesApi: MessagesApi, cacheApi: CacheApi)
    extends Controller {

  private implicit def messages(implicit lang: Lang) = new Messages(lang, messagesApi)

  private val AuthTokenHeader = "X-XSRF-TOKEN"
  private val AuthTokenCookieKey = "XSRF-TOKEN"

  private lazy val CacheExpiration =
    configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 60 /* minutes */ ).seconds

  case class Login(email: String, password: String)

  private val loginForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText)(Login.apply)(Login.unapply))

  implicit class ResultWithToken(result: Result) {
    def withToken(token: (String, User.Id)): Result = {
      Logger.trace(s"Adding token to cache: $token with expiration $CacheExpiration")
      cacheApi.set(token._1, token._2, CacheExpiration)
      result.withCookies(Cookie(AuthTokenCookieKey, token._1, None, httpOnly = false))
    }

    def discardingToken(token: String): Result = {
      Logger.trace(s"Discarding token from cache: $token")
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
    Logger.trace(s"Recievned logout request: ${request.headers.toMap}")
    request.headers.get(AuthTokenHeader) map { token =>
      Redirect("/").discardingToken(token)
    } getOrElse BadRequest(Json.obj("err" -> "No Token"))
  }
}
