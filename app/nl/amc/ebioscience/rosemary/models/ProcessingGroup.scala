package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import com.mongodb.casbah.Imports._
import java.util.Date
import play.api.Logger

case class ProcessingGroup(
    name: String,
    initiator: User.Id,
    inputs: Seq[ParamOrDatum] = Seq.empty,
    outputs: Seq[ParamOrDatum] = Seq.empty,
    recipes: Set[Recipe.Id],
    executionDate: Date = new Date(),
    tags: Set[Tag.Id] = Set.empty, // To relate this processing to a workspace (WorkspaceTag) or send via a message (MessageTag)
    progress: Int = 0,
    statuses: Seq[Status] = Nil,
    id: ProcessingGroup.Id = new ProcessingGroup.Id,
    info: Info = new Info) extends ProcessingBase { // TODO Index and search ProcessingGroups

  lazy val processings = ProcessingGroup.processings.findByParentId(id).toList
}

object ProcessingGroup extends DefaultModelBase[ProcessingGroup]("processingGroups")
    with TagsQueries[ProcessingGroup] with ProcessingIOQueries[ProcessingGroup] {

  val processings = new DefaultModelChild[Processing]("processings")
}
