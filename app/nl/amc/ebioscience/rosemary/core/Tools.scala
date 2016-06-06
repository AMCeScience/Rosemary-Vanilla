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
