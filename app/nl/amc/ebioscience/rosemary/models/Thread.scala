package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import com.mongodb.casbah.Imports._

case class Thread(
    watchers: Set[User.Id], // Those who watch this thread and receive notification for every message posted
    messages: List[Tag.Id], // List of MessageTags in chronological order
    tags: Set[Tag.Id], // To relate this thread to a workspace (WorkspaceTag)
    id: Thread.Id = new Thread.Id,
    info: Info = new Info) extends BaseEntity with WithTags {

  def getMessages = Tag.findByIds(messages.toSet).toList.sortBy(_.info.created).reverse
}

object Thread extends DefaultModelBase[Thread]("threads") with TagsQueries[Thread] {

  def addMessage(threadId: Thread.Id, messageId: Tag.Id) = update(
    "_id" $eq threadId,
    $push("messages" -> messageId))

  def getMessages(threadId: Thread.Id) = Thread.findOneById(threadId).map(_.getMessages)

  /** Get at most 10 threads sorted in reverse chronological order
    * starting from the specified page
    */
  def getThreads(workspaceId: Tag.Id, page: Int) =
    findWithAnyTags(Set(workspaceId), page).toList

  /** Add the author and all subscribers to the watchers of the specified thread.
    * If they are already watching they won't be added again.
    */
  def addWatchers(threadId: Thread.Id, subscribers: Set[User.Id]) = update(
    "_id" $eq threadId,
    $addToSet("watchers") $each subscribers)

  /** Removes the specified unsubscribers from the watchers of the specified thread */
  def removeWatchers(threadId: Thread.Id, unsubscribers: Set[User.Id]) = update(
    "_id" $eq threadId,
    $pullAll("watchers" -> unsubscribers))

  /** Get the workspace id of a thread */
  // TODO implement better
  def getWorkspace(threadId: Thread.Id): Option[Tag.Id] =
    for (thread <- findOneById(threadId) if thread.tags.size == 1) yield thread.tags.head
}
