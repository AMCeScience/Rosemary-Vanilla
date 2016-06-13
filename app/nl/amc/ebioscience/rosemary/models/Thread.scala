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
import com.mongodb.casbah.Imports._

/**
 * Thread keeps a series of `MessageTag`s and watchers
 *
 * @param watchers Set of user IDs of those who watch this thread specifically
 * @param messages List of [[MessageTag]] IDs in chronological order
 * @param tags WorkspaceTag IDs to attach this thread to a workspace
 * @param id ID of this Thread, system provided
 * @param info Metadata bout this Thread
 */
case class Thread(
    watchers: Set[User.Id],
    messages: List[Tag.Id],
    tags: Set[Tag.Id],
    id: Thread.Id = new Thread.Id,
    info: Info = new Info) extends BaseEntity with WithTags {

  /**
   * Get the messages in this thread in chronological order
   */
  def getMessages = Tag.findByIds(messages.toSet).toList.sortBy(_.info.created).reverse
}

/**
 * Thread companion object that contains database queries specific to the `threads` collection.
 */
object Thread extends DefaultModelBase[Thread]("threads") with TagsQueries[Thread] {

  def addMessage(threadId: Thread.Id, messageId: Tag.Id) = update(
    "_id" $eq threadId,
    $push("messages" -> messageId))

  def getMessages(threadId: Thread.Id) = Thread.findOneById(threadId).map(_.getMessages)

  /**
   * Get at most 10 threads sorted in reverse chronological order
   * starting from the specified page
   */
  def getThreads(workspaceId: Tag.Id, page: Int) =
    findWithAnyTags(Set(workspaceId), page).toList

  /**
   * Add the author and all subscribers to the watchers of the specified thread.
   * If they are already watching they won't be added again.
   */
  def addWatchers(threadId: Thread.Id, subscribers: Set[User.Id]) = update(
    "_id" $eq threadId,
    $addToSet("watchers") $each subscribers)

  /**
   * Removes the specified unsubscribers from the watchers of the specified thread
   */
  def removeWatchers(threadId: Thread.Id, unsubscribers: Set[User.Id]) = update(
    "_id" $eq threadId,
    $pullAll("watchers" -> unsubscribers))

  /**
   * Get the workspace id of a thread
   */
  def getWorkspace(threadId: Thread.Id): Option[Tag.Id] =
    // TODO implement better
    for (thread <- findOneById(threadId) if thread.tags.size == 1) yield thread.tags.head
}
