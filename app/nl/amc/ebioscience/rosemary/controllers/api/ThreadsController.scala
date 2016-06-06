package nl.amc.ebioscience.rosemary.controllers.api

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.core.{ WebSockets, HelperTools }
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.services.SecurityService

@Singleton
class ThreadsController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  def queryId(id: Thread.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Thread.findOneById(id).map { thread =>
      Ok(threadAndItsMessagesToJson(thread))
    } getOrElse Conflict(s"Could not find thread_id $id")
  }

  case class MessageRequest(
      name: String, // subject
      body: String,
      dataids: Option[Set[Datum.Id]],
      processinggroupids: Option[Set[ProcessingGroup.Id]],
      processingids: Option[Set[Processing.Id]],
      receivers: Option[Set[User.Id]],
      thread: Option[Thread.Id],
      workspace: Option[Tag.Id]) {

    /** Checks the validity request content */
    def validate = {

      // check existence of Datum IDs
      val eitherData = dataids.map { dataIds =>
        for {
          datumId <- dataIds.toList
        } yield Datum.findOneById(datumId).map {
          d => Right(d.id)
        } getOrElse Left(s"Could not find datum_id $datumId")
      }

      // check existence of Processing IDs
      val eitherProcessingGroupss = processinggroupids.map { pgIds =>
        for {
          pgId <- pgIds.toList
        } yield ProcessingGroup.findOneById(pgId).map {
          p => Right(p.id)
        } getOrElse Left(s"Could not find ProcessingGroup $pgId")
      }

      // check existence of Processing IDs
      val eitherProcessings = processingids.map { pIds =>
        for {
          pId <- pIds.toList
        } yield Processing.findOneById(pId).map {
          p => Right(p.id)
        } getOrElse Left(s"Could not find Processing $pId")
      }

      // check existence of receivers' User IDs
      val eitherReceivers = receivers.map { recieverIds =>
        for {
          userId <- recieverIds.toList
        } yield User.findOneById(userId).map {
          u => Right(u.id)
        } getOrElse Left(s"Could not find user_id $userId")
      }

      // check existence of Thread ID
      val eitherThread = thread.map { threadId =>
        Thread.findOneById(threadId).map {
          t => Right(t.id)
        } getOrElse Left(s"Could not find thread_id $threadId")
      }

      // check existence of workspace Tag ID
      val eitherWorkspace = workspace.map { workspaceId =>
        Tag.findOneById(workspaceId).map {
          w => Right(w.id)
        } getOrElse Left(s"Could not find tag_id $workspaceId")
      }

      // concatenate all "Eithers"
      val eithersList = eitherData.getOrElse(Nil) :::
        eitherProcessings.getOrElse(Nil) :::
        eitherReceivers.getOrElse(Nil) :::
        List(eitherThread, eitherWorkspace).flatten

      // convert List[Either[String, Ids]] to Either[List[String], List[Ids]]
      HelperTools.evertEitherList(eithersList)
    }
  }
  object MessageRequest {
    implicit val messageRequestFmt = Json.format[MessageRequest]
  }

  def create = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[MessageRequest].fold(
      valid = { messageRequest =>
        messageRequest.validate match {
          case Right(_) => // this means every id exists
            val socket = WebSockets.getSocket
            val author = User.current.id
            val receivers = messageRequest.receivers.getOrElse(Set.empty)
            val countData = messageRequest.dataids.map(_.size).getOrElse(0)
            val countProcessing = messageRequest.processingids.map(_.size).getOrElse(0)
            val countProcessingGroup = messageRequest.processinggroupids.map(_.size).getOrElse(0)

            messageRequest.thread match {
              case Some(threadId) => // this message is an addition to a Thread
                val message = messageBuilder(messageRequest, author, countData, countProcessing, countProcessingGroup)
                Thread.addMessage(threadId, message.id)
                Thread.addWatchers(threadId, receivers + author)
                val notification = MessageNotification(
                  actor = Some(author),
                  message = message.id,
                  thread = threadId,
                  receivers = receivers,
                  tags = Set(Thread.getWorkspace(threadId).get),
                  info = Info(dict = Map(
                    "subject" -> Valunit(message.name),
                    "countData" -> Valunit(countData.toString),
                    "countProcessing" -> Valunit(countProcessing.toString),
                    "countProcessingGroup" -> Valunit(countProcessingGroup.toString)))).insert
                socket.map(_.send("notification", notification.toJson))
                Thread.findOneById(threadId).map { thread =>
                  Ok(threadAndItsMessagesToJson(thread))
                } getOrElse Conflict(s"Could not find thread_id $threadId")

              case None => messageRequest.workspace match {
                case Some(workspaceId) => // a new thread with a new message
                  val message = messageBuilder(messageRequest, author, countData, countProcessing, countProcessingGroup)
                  val thread = Thread(
                    watchers = receivers + author,
                    messages = List(message.id),
                    tags = Set(workspaceId)).insert
                  val notification = MessageNotification(
                    actor = Some(author),
                    message = message.id,
                    thread = thread.id,
                    receivers = receivers,
                    tags = Set(workspaceId),
                    info = Info(dict = Map(
                      "subject" -> Valunit(message.name),
                      "countData" -> Valunit(countData.toString),
                      "countProcessing" -> Valunit(countProcessing.toString),
                      "countProcessingGroup" -> Valunit(countProcessingGroup.toString)))).insert
                  socket.map(_.send("notification", notification.toJson))
                  Ok(threadAndItsMessagesToJson(thread))
                case None => BadRequest(Json.toJson(errorMaker("workspace", "error.path.missing")))
              }
            }
          case Left(errors) => Conflict(errors mkString (" , "))
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  case class ThreadListRequest(workspace: Tag.Id, page: Option[Int])
  object ThreadListRequest {
    implicit val threadRequestFmt = Json.format[ThreadListRequest]
  }

  def query = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[ThreadListRequest].fold(
      valid = { threadListRequest =>
        val workspace = threadListRequest.workspace
        val page = threadListRequest.page match {
          case Some(number) if number >= 0 => number
          case _                           => 0
        }
        val threads = Thread.getThreads(workspace, page)
        val result = threads.map(threadAndItsMessagesToJson(_))
        Ok(Json.toJson(result))
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  case class SubscriptionRequest(thread: Thread.Id, userids: Set[User.Id], action: String) {
    /** Checks the validity of request contents */
    def validate = {

      // check existence of Thread ID
      val eitherThread = Thread.findOneById(thread).map {
        t => Right(t.id)
      } getOrElse Left(s"Could not find thread_id $thread")

      // check existence of User IDs
      val eitherUserids = for {
        userId <- userids.toList
      } yield User.findOneById(userId).map {
        u => Right(u.id)
      } getOrElse Left(s"Could not find user_id $userId")

      // validate action command
      val eitherAction = action match {
        case a @ ("watch" | "unwatch") => Right(a)
        case _                         => Left("""Acceptable actions are "watch" and "unwatch".""")
      }
      val eithersList = eitherAction :: eitherUserids
      HelperTools.evertEitherList(eithersList)
    }
  }
  object SubscriptionRequest {
    implicit val subscriptionRequestFmt = Json.format[SubscriptionRequest]
  }

  def subscription = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[SubscriptionRequest].fold(
      valid = { subscriptionRequest =>
        subscriptionRequest.validate match {
          case Right(_) => // logically correct request
            subscriptionRequest.action match {
              case "watch"   => Thread.addWatchers(subscriptionRequest.thread, subscriptionRequest.userids)
              case "unwatch" => Thread.removeWatchers(subscriptionRequest.thread, subscriptionRequest.userids)
            }
            Redirect(s"/api/v1/threads/${subscriptionRequest.thread}")
          case Left(errors) => Conflict(errors mkString (" , "))
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  private def messageBuilder(mr: MessageRequest, author: User.Id, countData: Int, countProcessing: Int, countProcessingGroup: Int) = {
    val dict = Map(
      "countData" -> Valunit(countData.toString),
      "countProcessing" -> Valunit(countProcessing.toString),
      "countProcessingGroup" -> Valunit(countProcessingGroup.toString))
    val mt = MessageTag(
      name = mr.name,
      body = mr.body,
      rights = Personal(author),
      info = Info(dict = dict)).insert
    // tag attached data with the MessageTag
    mr.dataids.map { ds => Datum.tagEntities(ds, mt.id) }
    // tag attached Processings with the MessageTag
    mr.processingids.map { ps => Processing.tagEntities(ps, mt.id) }
    // tag attached ProcessingGroups with the MessageTag
    mr.processinggroupids.map { pgs => ProcessingGroup.tagEntities(pgs, mt.id) }
    mt
  }

  private def threadAndItsMessagesToJson(thread: Thread) =
    Json.obj("thread" -> thread.toJson,
      "messages" -> thread.getMessages.toJson)
}
