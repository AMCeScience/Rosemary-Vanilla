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
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle

@Salat
sealed trait Notification extends BaseEntity with WithTags

/**
 * {actor} {action} {affected} to/from {workspace}
 *
 * @param actor ID of the user who added or removed somebody to/from a Workspace
 * @param action "added" or "removed"
 * @param affected ID of the user who is added or removed to/from a workspace
 * @param workspace ID of the `WorkspaceTag` to/from which somebody is added/removed
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class UserWorkspaceNotification(
  actor: User.Id,
  action: String,
  affected: User.Id,
  workspace: Tag.Id,
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info) extends Notification

/**
 * {actor} imported {summary} from {resource} to {workspace}, see {imported}
 *
 * @param actor ID of the user who initiated the import
 * @param resource ID of the resource from which the data is imported
 * @param workspace ID of the `WorkspaceTag` that the data is imported to
 * @param imported ID of the `SystemTag` that is added to the newly imported data
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class ImportNotification(
  actor: User.Id,
  resource: Resource.Id,
  workspace: Tag.Id,
  imported: Tag.Id,
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info // 1 project, 20 subjects, 20 image sessions, 80 scans, 160 files
  ) extends Notification

/**
 * {processing} generated {newdata}
 *
 * @param processing ID of the `ProcessingGroup` that generated the new data
 * @param newdata ID of the `SystemTag` that is added to the newly generated data
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class NewDataNotification(
  processing: ProcessingGroup.Id,
  newdata: Tag.Id,
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info) extends Notification

/**
 * {actor} {action} {processing}
 *
 * @param actor ID of the user who initiated the action
 * @param action "submitted", "resumed", or "aborted"
 * @param processing ID of the `ProcessingGroup` who is affected
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class UserProcessingNotification(
  actor: User.Id,
  action: String,
  processing: ProcessingGroup.Id,
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info) extends Notification

/**
 * {processing} status changed to {status}
 *
 * @param processing ID of the `ProcessingGroup` with the new status
 * @param status The new status
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class ProcessingNotification(
  processing: ProcessingGroup.Id,
  status: ProcessingLifeCycle.Value,
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info) extends BaseEntity with Notification

/**
 * {actor} sent a {message} [with {} data and {} processings] to {receivers}
 *
 * @param actor ID of the user who authored this message, n.b. if `None`, it is the System
 * @param message ID of the `MessageTag`
 * @param thread ID of the `Thread` that includes the message
 * @param receivers Set of IDs of the users to whom this message is sent directly, n.b. this might be different than watchers
 * @param tags Set of `WorkspaceTag` IDs for visibility of this notification
 * @param id ID of this `Notification`, system provided
 * @param info Metadata about this `Notification`
 */
case class MessageNotification(
  actor: Option[User.Id],
  message: Tag.Id,
  thread: Thread.Id,
  receivers: Set[User.Id],
  tags: Set[Tag.Id],
  id: Notification.Id = new Notification.Id,
  info: Info = new Info) extends Notification

/**
 * Notification companion object that contains database queries specific to the `notifications` collection.
 */
object Notification extends DefaultModelBase[Notification]("notifications") with TagsQueries[Notification] {

  collection.createIndex(("_id" $eq 1) ++ ("_t" $eq 1), "default_language" $eq "none")
}
