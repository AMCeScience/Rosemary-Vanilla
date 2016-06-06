package nl.amc.ebioscience.rosemary

import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat

/**
 * WIP not used currently
 */
class RosemaryApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new RosemaryComponents(context).application
  }
}

class RosemaryComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  lazy val router = Router.empty
  
  lazy val mongoContext = injector.instanceOf(classOf[MongoContext])
  lazy val playSalat = injector.instanceOf(classOf[PlaySalat])
}
