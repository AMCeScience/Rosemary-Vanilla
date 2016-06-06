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
package nl.amc.ebioscience.rosemary.core.processing

import nl.amc.ebioscience.rosemary.models.{ Application, Datum, Tag, Replica, Processing, Resource, PortKind }
import nl.amc.ebioscience.processingmanager.types.messaging.StatusContainerMessage
import play.api.Logger

/**
  * This class wraps [[controllers.api.Processings.SubmitRequest]] information
  * to avoid multiple database queries.
  */
case class Cybertronian(
  application: Application,
  dataPorts: Map[String, Seq[Datum]],
  paramPorts: Map[String, Seq[String]])

case class InflatedConcretePort(
  name: String,
  data: Container)

case class IOInflatedConcretePort(
  stems: Set[InflatedConcretePort],
  inputs: Set[InflatedConcretePort],
  outputs: Set[InflatedConcretePort])

// TODO generalize with Case Classes with constructor instead of objects
// See this : http://stackoverflow.com/questions/12122939/generating-a-class-from-string-and-instantiating-it-in-scala-2-10
/** @param planet is the resource on which Datum replicas should and will be stored */
abstract class Transformer(val planet: Resource) {

  /**
    * Checks the validity of the processing request according to the domain information
    * For example, the Tracula application accepts Experiments (image sessions) that contain exactly
    * two scans: one of type DTI and another of type MRI.
    * @return Some Map of Port IDs and their corresponding error messages. None if everything was OK.
    */
  def revealDecepticons(cybertronian: Cybertronian): Option[Map[String, String]]

  /**
    * Transforms a user request (wrapped in [[Cybertronian]]) into a Sequence of [[IOInflatedConcretePort]]
    * This transformation requires domain knowledge too, for example, for the Tracula application,
    * three experiments are converted into 3 Processings, each with two scans as its input files.
    * @return Sequence of [[IOInflatedConcretePort]] to construct [[ProcessingMessage]] and send to the [[ProcessingManagerClient]]
    */
  def transform(cybertronian: Cybertronian): Seq[IOInflatedConcretePort]

  /**
    * Get information from a [[processingmanager.types.messaging.StatusContainerMessage]]
    * and updates the related Processing in the model based on that. It also updates the status.
    * @return The updated [[models.Processing]] if it could find a related Processing to update, None otherwise
    */
  def getSpark(statusContainerMsg: StatusContainerMessage): Option[Processing]

  // Helper functions

  protected def hasReplicaOnThisPlanet(datum: Datum): Boolean = datum.getReplica(planet.id).isDefined
  protected def getReplicaOnThisPlanet(datum: Datum): Replica = datum.getReplica(planet.id).get
  /**
    * Make sure each port has at least one data or parameter value, this is generic
    */
  protected def getMissingPorts(cybertronian: Cybertronian): Map[String, String] = {
    val result = scala.collection.mutable.HashMap.empty[String, String]
    val application = cybertronian.application
    val dataPorts = cybertronian.dataPorts
    val paramPorts = cybertronian.paramPorts

    application.iPorts.foreach { abstractPort =>
      abstractPort.kind match {
        case PortKind.Data =>
          dataPorts.get(abstractPort.name) match {
            case None      => result += (abstractPort.name -> s"${abstractPort.name} is missing data!")
            case Some(seq) => if (seq.size < 1) result += (abstractPort.name -> s"${abstractPort.name} has an empty data set!")
          }
        case PortKind.Param =>
          paramPorts.get(abstractPort.name) match {
            case None      => result += (abstractPort.name -> s"${abstractPort.name} is missing values!")
            case Some(seq) => if (seq.size < 1) result += (abstractPort.name -> s"${abstractPort.name} has an empty value set!")
          }
      }
    }

    result.toMap
  }

  /**
    * Generic function that tests if the category tag of
    * the datum is equal to a specific DatumCategory
    */
  protected def isCategory(datum: Datum, cat: Tag.DatumCategories.Value): Boolean =
    datum.getCategoryName match {
      case Some(datumCat) => if (datumCat == cat.toString) true else false
      case None =>
        Logger.warn(s"Could not determine category of ${datum.name}!"); false
    }
}
