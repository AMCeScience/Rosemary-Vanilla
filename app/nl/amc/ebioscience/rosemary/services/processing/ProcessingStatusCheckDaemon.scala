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
package nl.amc.ebioscience.rosemary.services.processing

import javax.inject._
import nl.amc.ebioscience.rosemary.services.ConfigService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import akka.actor.{ ActorSystem, ActorRef }

@Singleton
class ProcessingStatusCheckDaemon @Inject() (
    @Named("processingStatusCheckActor") processingStatusCheckActor: ActorRef,
    configService: ConfigService,
    actorSystem: ActorSystem) {

  val startInterval = configService.getIntConfig("processingmanager.status.start.interval")
  val pullInterval = configService.getIntConfig("processingmanager.status.pull.interval")

  val cancellable = actorSystem.scheduler.schedule(startInterval.seconds, pullInterval.minutes, processingStatusCheckActor, "pmDaemon")
}
