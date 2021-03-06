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
package nl.amc.ebioscience.rosemary.services

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.mvc.Http
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import nl.amc.ebioscience.rosemary.models.User

trait SecurityService {
  def getUserFromToken(token: String): Either[String, User.Id]
  def HasToken[A](p: BodyParser[A])(f: Request[A] => Result): Action[A]
  def HasTokenAsync[A](p: BodyParser[A])(f: Request[A] => Future[Result]): Action[A]
}

@Singleton
class RosemarySecurityService @Inject() (cacheApi: CacheApi) extends SecurityService with Controller {

  val AuthTokenHeader = "X-XSRF-TOKEN"

  def getUserFromToken(token: String) = {
    Logger.trace(Thread.currentThread().getId() + " || Token: " + token + " User: " + cacheApi.get[User.Id](token).toString)
    cacheApi.get[User.Id](token) match {
      case Some(user) => Right(user)
      case None       => Left("Invalid token")
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

  private def auth[A, R](request: Request[A], f: Request[A] => R, r: Result => R): R = {
    auth(request.headers) match {
      case Left(error) => r(Unauthorized(Json.obj("err" -> error)))
      case Right(userid) => User.current_id.withValue(Some(userid)) {
        Logger.trace("AUTHID: " + userid)
        f(request)
      }
    }
  }

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
