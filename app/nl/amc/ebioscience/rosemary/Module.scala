package nl.amc.ebioscience.rosemary

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import java.time.Clock

import nl.amc.ebioscience.rosemary.actors._
import nl.amc.ebioscience.rosemary.services._
import nl.amc.ebioscience.rosemary.services.dao._

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()

    // Services
    bind(classOf[Security]).to(classOf[RosemarySecurity])
    // Set RosemaryConfig as the implementation for Config when the application starts.
    // This will check if every necessary Config value is in place.
    bind(classOf[Config]).to(classOf[RosemaryConfig]).asEagerSingleton()
    // Set KeyCrypto (based on Keyczar) as the implementation for Crypto.
    bind(classOf[Crypto]).to(classOf[KeyCrypto])

    // Akka actors
    bindActor[ConnectionParentActor]("connectionParentActor")
    bindActorFactory[ConnectionActor, ConnectionActor.Factory]

    // Ask Guice to create a singleton instance of MongoContext containing the context as implicit value 
    bind(classOf[MongoContext])
    bind(classOf[ResourceDAO])
  }

}
