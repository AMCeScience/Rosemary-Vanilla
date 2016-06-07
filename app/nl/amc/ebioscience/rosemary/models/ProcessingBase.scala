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
package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._
import java.util.Date
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle

@Salat
trait ProcessingBase extends Searchable {
  val initiator: User.Id
  val inputs: Iterable[ParamOrDatum]
  val outputs: Iterable[ParamOrDatum]
  val recipes: Set[Recipe.Id]
  val executionDate: Date
  val progress: Int
  val statuses: Seq[Status]

  def lastStatus = statuses.lastOption.map(_.status)

  /**
   * Get updated values of [[statuses]], [[progress]], and [[tags]] based on the new status and progress
   * to use in the copy method to update an instance of subclasses
   */
  def getToUpdateStatusesTagsProgress(newStatus: ProcessingLifeCycle.Value, newProgress: Option[Int]) = {
    val lastStatusOpt = statuses.lastOption
    val secondLastStatusOpt = statuses.dropRight(1).lastOption

    val lastAndSecondLastStatuses = for (lastStatus <- lastStatusOpt; secondLastStatus <- secondLastStatusOpt)
      yield (lastStatus, secondLastStatus)

    val (toUpdateStatuses, toUpdateTags) = lastAndSecondLastStatuses match {
      case Some((l, sl)) if (l.status == sl.status && l.status == newStatus) =>
        (statuses.dropRight(1) :+ Status(newStatus), tags)
      case _ => (statuses :+ Status(newStatus),
        tags -- getProcessingStatusTags.map(_.id) + Tag.getProcessingStatusTag(newStatus.toString).id)
    }

    val toUpdateProgress =
      if (newProgress.isDefined && newProgress.get > progress)
        newProgress.get
      else progress

    (toUpdateStatuses, toUpdateTags, toUpdateProgress)
  }
}

/**
  * To Store a single status event
  * @param status State according to <code>StatusState</code>
  * @param time Timestamp of this status
  */
case class Status(
  status: ProcessingLifeCycle.Value,
  time: Date = new Date())

/**
  * @param name The identifying (unique) name of the port (uniqueness is not checked)
  * @param param The concrete value, if the port is of kind <code>PortKind.Param</code>
  * @param datum The concrete replica if the port is of kind <code>PortKind.File</code>
  */
case class ParamOrDatum(
  name: String,
  param: Option[String] = None,
  datum: Option[DatumAndReplica] = None)

case class DatumAndReplica(
  datum: Datum.Id,
  replica: Option[Replica.Id] = None)

trait ProcessingIOQueries[T <: ProcessingBase] {
  this: DefaultModelBase[T] =>

  def findByIorO(input: Datum.Id, output: Datum.Id) =
    find($or(("inputs.datum.datum" $eq input), ("outputs.datum.datum" $eq output))).toList

  def findByIandO(input: Datum.Id, output: Datum.Id) =
    find($and(("inputs.datum.datum" $eq input), ("outputs.datum.datum" $eq output))).toList

  def findByI(input: Datum.Id) = find("inputs.datum.datum" $eq input).toList

  def findByO(output: Datum.Id) = find("outputs.datum.datum" $eq output).toList
}
