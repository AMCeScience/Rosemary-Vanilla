package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import com.novus.salat.dao.SalatMongoCursor
import play.api.Logger
import com.mongodb.casbah.Imports._
import java.util.Date

/**
  * To capture information about a single unit of Processing
  * @param parentId ID of the <code>ProcessingGroup</code> that this <code>Processing</code> belongs to
  * @param name name of this <code>Processing</code>
  * @param initiator ID of the <code>User</code> that initiates this Processing
  * @param inputs Concrete input ports
  * @param outputs Concrete output ports
  * @param recipes Set of <code>Recipe</code>s that are used in this <code>Processing</code>
  * @param executionDate Timestamp that this <code>Processing</code> has been created
  * @param tags ID of the <code>Tag</code>s such as <code>WorkspaceTag</code>,
  * <code>MessageTag</code>, and <code>UserTag</code>
  * @param progress To show an estimated percentage of the progress
  * @param statuses Sequence of events in the <code>Status</code>es
  * @param id ID of this <code>Processing</code>
  * @param info <code>Info</code> about this <code>Processing</code>
  *
  * @see nl.amc.ebioscience.rosemary.models.ProcessingGroup
  */
case class Processing(
    parentId: Option[ProcessingGroup.Id] = None,
    name: String,
    initiator: User.Id,
    inputs: Set[ParamOrDatum] = Set.empty,
    outputs: Set[ParamOrDatum] = Set.empty,
    recipes: Set[Recipe.Id],
    executionDate: Date = new Date(),
    tags: Set[Tag.Id] = Set.empty,
    progress: Int = 0,
    statuses: Seq[Status] = Nil,
    id: Processing.Id = new Processing.Id,
    info: Info = new Info) extends ProcessingBase {

  lazy val processingGroup = parentId.map { parentId =>
    ProcessingGroup.findOneById(parentId) match {
      case Some(pg) => pg
      case None     => throw new Throwable(s"ProcessingGroup $parentId is not known for Processing $id.")
    }
  }
}

/**
  * Also see [[nl.amc.ebioscience.rosemary.models.ProcessingGroup]]
  * This collection is used as the child collection for ProcessingGroup.
  */
object Processing extends DefaultModelBase[Processing]("processings")
    with TagsQueries[Processing] with ProcessingIOQueries[Processing] {
}
