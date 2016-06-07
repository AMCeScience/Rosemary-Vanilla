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
package nl.amc.ebioscience.rosemary.controllers

import javax.inject._
import play.api.Logger
import play.api.mvc._
import scala.util.Random
import java.util.Date
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle
import nl.amc.ebioscience.rosemary.services.CryptoService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class InitController @Inject() (cryptoService: CryptoService) extends Controller {

  def init = Action {
    playSalat.db().dropDatabase()

    // Create structure around the Datums
    initDB

    // Inject Datums
    injectData

    // Inject Processings
    injectProcessings

    Redirect("/")
  }

  private def initDB = {
    Logger.info("Initialising the database...")

    // Admin User
    var adminUser = User("admin@rosemary.ebioscience.amc.nl", "secret", "Admin Admin", true, true, Role.Admin).hashPassword.insert
    val adminWorkspace = WorkspaceTag("Admin's Workspace", Membered(adminUser.id)).insert

    // Test users
    // Approved, enabled user, and their workspace
    var approvedUser = User("approved-user@rosemary.ebioscience.amc.nl", "secret", "Approved User", true).hashPassword.insert
    val approvedWorkspace = WorkspaceTag("Approved User's Workspace", Membered(approvedUser.id)).insert
    // Unapproved, enabled user, and their workspace
    var unapprovedUser = User("unapproved-user@rosemary.ebioscience.amc.nl", "secret", "Unapproved User", false).hashPassword.insert
    val unapprovedWorkspace = WorkspaceTag("Unapproved User's Workspace", Membered(unapprovedUser.id)).insert
    // Approved, disabled user, and their workspace
    var disabledUser = User("disabled-user@rosemary.ebioscience.amc.nl", "secret", "Disabled User", true, false).hashPassword.insert
    val disabledWorkspace = WorkspaceTag("Disabled User's Workspace", Membered(disabledUser.id)).insert
    // Shared workspace with one owner and two members
    val multiWorkspace = WorkspaceTag("Multi-user Workspace", Membered(approvedUser.id, Set(disabledUser.id, adminUser.id))).insert

    // Datum Category Tags
    val category_grandfather = DatumCategoryTag(Tag.DatumCategories.GrandFather.toString).insert
    val category_father = DatumCategoryTag(Tag.DatumCategories.Father.toString).insert
    val category_child = DatumCategoryTag(Tag.DatumCategories.Child.toString).insert

    // Processing Category Tags
    val processing_category_dataprocessing = ProcessingCategoryTag(Tag.ProcessingCategories.DataProcessing.toString).insert

    // Processing Status Tags
    val processing_status_tag_inpreparation = ProcessingStatusTag(ProcessingLifeCycle.InPreparation.toString).insert
    val processing_status_tag_stagein = ProcessingStatusTag(ProcessingLifeCycle.StageIn.toString).insert
    val processing_status_tag_submitting = ProcessingStatusTag(ProcessingLifeCycle.Submitting.toString).insert
    val processing_status_tag_inprogress = ProcessingStatusTag(ProcessingLifeCycle.InProgress.toString).insert
    val processing_status_tag_onhold = ProcessingStatusTag(ProcessingLifeCycle.OnHold.toString).insert
    val processing_status_tag_stageout = ProcessingStatusTag(ProcessingLifeCycle.StageOut.toString).insert
    val processing_status_tag_done = ProcessingStatusTag(ProcessingLifeCycle.Done.toString).insert
    val processing_status_tag_aborted = ProcessingStatusTag(ProcessingLifeCycle.Aborted.toString).insert
    val processing_status_tag_unknown = ProcessingStatusTag(ProcessingLifeCycle.Unknown.toString).insert

    // MongoDB Resource for files stored in GridFS
    Resource(
      name = "Local MongoDB",
      kind = ResourceKind.Mongodb,
      protocol = "http",
      host = "localhost",
      port = 80,
      basePath = Some("/api/v1/download")).insert

    // WebDAV Resource
    Resource(
      name = "WebDAV",
      kind = ResourceKind.Webdav,
      protocol = "http",
      host = "localhost",
      basePath = Some("/webdav/files"),
      username = Some("webdav"),
      password = Some(cryptoService.encrypt("secret"))).insert

    // Mock application
    val mockPmIPorts = Set(
      AbstractPort(name = "Parameter One", kind = PortKind.Param),
      AbstractPort(name = "Input Data", kind = PortKind.File))
    val mockPmOPorts = Set(
      AbstractPort(name = "Output Data", kind = PortKind.File))
    val mockPmApp = PMApplication(iPorts = mockPmIPorts, oPorts = mockPmOPorts)
    val mockUiIPorts = Set(
      AbstractPort(name = "Input Data", kind = PortKind.Data),
      AbstractPort(name = "Parameter One", kind = PortKind.Param))
    Application(
      name = "Mock",
      description = "This is a Mock application",
      version = Some("1.0"),
      platform = Some("Dirac"),
      iPorts = mockUiIPorts,
      pmApplication = mockPmApp,
      transformer = "nl.amc.ebioscience.rosemary.core.processing.transformers.MockTransformer").insert

    Logger.info("Done initialising the database")
  }

  private def injectData = {
    Logger.info("Injecting datums into database...")

    // Get a user whose workspace we're going to fill
    val adminUser = User.find("admin@rosemary.ebioscience.amc.nl").get

    // Get the workspace
    val workspace = adminUser.getWorkspaceTagsHasAccess.filter(_.name == "Multi-user Workspace").head

    val childTag = Tag.getDatumCategory(Tag.DatumCategories.Child.toString).id
    val fatherTag = Tag.getDatumCategory(Tag.DatumCategories.Father.toString).id
    val grandFatherTag = Tag.getDatumCategory(Tag.DatumCategories.GrandFather.toString).id

    val resource = Resource.getLocalMongoResource

    // Grandfather datums
    for (gfi <- 1 to 5) {
      val fatherIds = scala.collection.mutable.Set.empty[Datum.Id]

      // Father datums
      for (fi <- 1 to Random.nextInt(5)) {
        val childIds = scala.collection.mutable.Set.empty[Datum.Id]

        // Child datums
        for (ci <- 1 to Random.nextInt(5)) {
          val childDatum = Datum(
            name = s"ChildDatum_${ci}",
            resource = Some(resource.id),
            tags = Set(workspace.id, childTag)).insert

          childIds += childDatum.id
        }

        val fatherDatum = Datum(
          name = s"FatherDatum_${fi}",
          children = childIds.toSet,
          resource = Some(resource.id),
          tags = Set(workspace.id, fatherTag)).insert

        fatherIds += fatherDatum.id
      }

      Datum(
        name = s"GrandFatherDatum_${gfi}",
        children = fatherIds.toSet,
        resource = Some(resource.id),
        tags = Set(workspace.id, grandFatherTag)).insert
    }

    Logger.info("Done injecting datums into database")
  }

  private def injectProcessings = {
    Logger.info("Injecting processings into database...")

    // Get a user whose workspace we're going to fill
    val adminUser = User.find("admin@rosemary.ebioscience.amc.nl").get

    // Get the workspace
    val workspace = adminUser.getWorkspaceTagsHasAccess.filter(_.name == "Multi-user Workspace").head

    // Get Application
    val application = Recipe.findByType("Application").filter(_.name == "Mock").head.asInstanceOf[Application]

    // Get Tags
    val dataProcessingTag = Tag.getProcessingCategory(Tag.ProcessingCategories.DataProcessing.toString)
    val fatherCategory = Tag.datumCategoriesNameMap(Tag.DatumCategories.Father.toString)
    val childCategory = Tag.datumCategoriesNameMap(Tag.DatumCategories.Child.toString)

    // Get some Datums
    val resourceData = Datum.findWithAllTagsNoPage(Set(workspace.id, fatherCategory.id)).toSeq
    val experimentData = Datum.findWithAllTagsNoPage(Set(workspace.id, childCategory.id)).toSeq

    // Get Processing Statuses
    val seqStatuses = Seq(ProcessingLifeCycle.InProgress, ProcessingLifeCycle.OnHold, ProcessingLifeCycle.Aborted)
    val seqStatusTags = Seq(Tag.getProcessingStatusTag(ProcessingLifeCycle.InProgress.toString),
      Tag.getProcessingStatusTag(ProcessingLifeCycle.OnHold.toString),
      Tag.getProcessingStatusTag(ProcessingLifeCycle.Aborted.toString)).map(_.id)

    // Insert N processings with randomised metadata
    for (i <- 1 to 15) {
      val processingGroup = ProcessingGroup(
        name = s"Mock_${i}",
        initiator = adminUser.id,
        inputs = application.iPorts.toSeq.map { abstractPort =>
          val datum = experimentData(Random.nextInt(experimentData.size))
          ParamOrDatum(
            name = abstractPort.name,
            param = if (abstractPort.kind == PortKind.Param) Some(Random.alphanumeric.take(10).mkString) else None,
            datum = if (abstractPort.kind == PortKind.Data) Some(DatumAndReplica(datum = datum.id)) else None)
        },
        recipes = Set(application.id),
        tags = Set(workspace.id, dataProcessingTag.id, seqStatusTags(Random.nextInt(seqStatusTags.size))),
        progress = Random.nextInt(100),
        statuses = Seq(
          nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.InPreparation),
          nl.amc.ebioscience.rosemary.models.Status(seqStatuses(Random.nextInt(seqStatuses.size)), new Date(new Date().getTime + Random.nextInt(300) * 60000)))).insert

      // Insert M submissions with randomised metadata
      for (j <- 1 to Random.nextInt(20)) {

        val someOfTheOPorts = application.pmApplication.oPorts.filter(_ => Random.nextBoolean)
        val finished = application.pmApplication.oPorts.size == someOfTheOPorts.size
        val statuses = Seq(
          nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.InPreparation),
          nl.amc.ebioscience.rosemary.models.Status(seqStatuses(Random.nextInt(seqStatuses.size)), new Date(new Date().getTime + Random.nextInt(300) * 60000)))

        val processing = Processing(
          parentId = Some(processingGroup.id),
          name = s"Mock_${i}_${j}",
          initiator = adminUser.id,
          inputs = application.pmApplication.iPorts.map { abstractPort =>
            val datum = resourceData(Random.nextInt(resourceData.size))
            ParamOrDatum(
              name = abstractPort.name,
              param = if (abstractPort.kind == PortKind.Param) Some(Random.alphanumeric.take(10).mkString) else None,
              datum = if (abstractPort.kind == PortKind.File) Some(DatumAndReplica(datum = datum.id)) else None)
          },
          outputs = someOfTheOPorts.map { abstractPort =>
            val datum = resourceData(Random.nextInt(resourceData.size))
            ParamOrDatum(
              name = abstractPort.name,
              param = if (abstractPort.kind == PortKind.Param) Some(Random.alphanumeric.take(10).mkString) else None,
              datum = if (abstractPort.kind == PortKind.File) Some(DatumAndReplica(datum = datum.id)) else None)
          },
          recipes = Set(application.id),
          tags = Set(workspace.id, dataProcessingTag.id, seqStatusTags(Random.nextInt(seqStatusTags.size))),
          progress = if (finished) 100 else Random.nextInt(91),
          statuses = if (finished) statuses :+ nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.Done, new Date(new Date().getTime + 18000000 + Random.nextInt(300) * 60000)) else statuses)
        ProcessingGroup.processings.insert(processing)
      }
    }

    Logger.info("Done injecting processings into database")
  }

}