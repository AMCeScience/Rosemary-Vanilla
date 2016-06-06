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
package nl.amc.ebioscience.rosemary.core

import nl.amc.ebioscience.rosemary.models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import dispatch.Req
import play.api.Logger
import dispatch._
import dispatch.Defaults._

object Tools {

  /** Run "body" in a Future that has the current user id */
  def future[T](body: => T) = {
    val userid = User.current_id.value
    Future {
      User.current_id.withValue(userid)(body)
    }
  }

  def query(req: Req) = {
    Logger.debug(s"Query External Service: ${req.url}")
    val futureResult = Http(req OK as.Response(x => x)).either
    futureResult.map(res =>
      res match {
        case Right(response) => {
          Logger.trace(s"External Service Response:\n# Headers: ${response.getHeaders}\n# ContentType: ${response.getContentType}\n# StatusCode: ${response.getStatusCode}\n# StatusText: ${response.getStatusText}\n# ResponseBody: ${response.getResponseBody}")
          if (response.getStatusCode == 200) Right(response.getResponseBody)
          else Left(new Throwable(s"StatusCode = ${response.getStatusCode}, ContentType = ${response.getContentType}"))
        }
        case Left(exception) => Left(exception)
      })
  }

  implicit class Slugify[T <: String](str: T) {
    def slugify = str.replaceAll("[^\\w-]", "-").replaceAll("-+", "-")
  }
}
