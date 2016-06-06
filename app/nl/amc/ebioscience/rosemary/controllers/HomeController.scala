package nl.amc.ebioscience.rosemary.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import nl.amc.ebioscience.rosemary.models.Resource
import nl.amc.ebioscience.rosemary.models.ResourceKind
import nl.amc.ebioscience.rosemary.services.dao.ResourceDAO
import com.mongodb.casbah.WriteConcern

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (resourceDao: ResourceDAO) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    val r = Resource(
      name = "Xnat Central",
      kind = ResourceKind.Webdav,
      protocol = "https",
      host = "central.xnat.org")

    resourceDao.save(r)

    Ok(nl.amc.ebioscience.rosemary.views.html.index("Your new application is ready."))
  }

}
