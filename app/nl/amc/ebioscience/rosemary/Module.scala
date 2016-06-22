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
package nl.amc.ebioscience.rosemary

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.libs.concurrent.AkkaGuiceSupport
import java.time.Clock

import nl.amc.ebioscience.rosemary.actors._
import nl.amc.ebioscience.rosemary.services._
import nl.amc.ebioscience.rosemary.services.dao._
import nl.amc.ebioscience.rosemary.services.search._
import nl.amc.ebioscience.rosemary.services.processing._
import nl.amc.ebioscience.rosemary.services.processing.transformers._
import nl.amc.ebioscience.rosemary.services.processing.transformers.nsg._

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
    bind(classOf[SecurityService]).to(classOf[RosemarySecurityService])
    // Set RosemaryConfig as the implementation for Config when the application starts.
    // This will check if every necessary Config value is in place.
    bind(classOf[ConfigService]).to(classOf[RosemaryConfigService]).asEagerSingleton()
    // Set KeyCrypto (based on Keyczar) as the implementation for Crypto.
    bind(classOf[CryptoService]).to(classOf[RosemaryCryptoService])

    // Akka actors
    bindActor[ConnectionParentActor]("connectionParentActor")
    bindActorFactory[ConnectionActor, ConnectionActor.Factory]
    bindActor[ProcessingStatusCheckActor]("processingStatusCheckActor")

    // Ask Guice to create a singleton instance of MongoContext containing the context as implicit value
    bind(classOf[MongoContext])
    bind(classOf[ResourceDAO])

    bind(classOf[ProcessingManagerClient])
    bind(classOf[ProcessingHelper])
    bind(classOf[ProcessingStatusCheckDaemon]).asEagerSingleton()
    bind(classOf[Transformer]).annotatedWith(Names.named("mockTransformer")).to(classOf[MockTransformer])
    bind(classOf[Transformer]).annotatedWith(Names.named("traculaTransformer")).to(classOf[TraculaTransformer])

    bind(classOf[SearchWriter]).asEagerSingleton()
    bind(classOf[SearchReader])
  }

}
