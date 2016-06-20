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
import org.kohsuke.randname.RandomNameGenerator
import nl.amc.ebioscience.rosemary.services.CryptoService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class InitController @Inject() (cryptoService: CryptoService) extends Controller {

  def init = Action { implicit request =>
    playSalat.db().dropDatabase()

    // Create structure around the Datums
    initDB(request)

    // Inject Datums
    injectData

    // Inject Processings
    injectProcessings

    Redirect("/reindex")
  }

  private def initDB(request: Request[AnyContent]) = {
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
    val category_project = DatumCategoryTag(Tag.DatumCategories.Project.toString).insert
    val category_subject = DatumCategoryTag(Tag.DatumCategories.Subject.toString).insert
    val category_experiment = DatumCategoryTag(Tag.DatumCategories.Experiment.toString).insert
    val category_scan = DatumCategoryTag(Tag.DatumCategories.Scan.toString).insert
    val category_reconstruction = DatumCategoryTag(Tag.DatumCategories.Reconstruction.toString).insert
    val category_resource = DatumCategoryTag(Tag.DatumCategories.Resource.toString).insert
    //val category_file = DatumCategoryTag(Tag.DatumCategories.File.toString).insert

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

    // Information about the deployment
    val hp = request.headers.toMap.get("Host").get.head.split(":")
    Logger.debug(s"Host Information= ${hp mkString " : "}")

    // MongoDB Resource for files stored in GridFS
    Resource(
      name = "Local MongoDB",
      kind = ResourceKind.Mongodb,
      protocol = "http",
      host = hp.head,
      port = if (hp.length > 1) hp.last.toInt else 80,
      basePath = Some("/api/v1/download")).insert

    // XNAT Resources 
    Resource(
      name = "Xnat Central",
      kind = ResourceKind.Xnat,
      protocol = "https",
      host = "central.xnat.org").insert

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
      transformer = "mockTransformer").insert

    Logger.info("Done initialising the database")
  }

  private def injectData = {
    import nl.amc.ebioscience.rosemary.models.core.ValunitConvertors._
    Logger.info("Injecting datums into database...")

    // Get a user whose workspace we're going to fill
    val adminUser = User.find("admin@rosemary.ebioscience.amc.nl").get

    // Get the workspace
    val workspace = adminUser.getWorkspaceTagsHasAccess.filter(_.name == "Multi-user Workspace").head

    val projectTag = Tag.getDatumCategory(Tag.DatumCategories.Project.toString).id
    val subjectTag = Tag.getDatumCategory(Tag.DatumCategories.Subject.toString).id
    val experimentTag = Tag.getDatumCategory(Tag.DatumCategories.Experiment.toString).id
    val scanTag = Tag.getDatumCategory(Tag.DatumCategories.Scan.toString).id
    val reconstructionTag = Tag.getDatumCategory(Tag.DatumCategories.Reconstruction.toString).id
    val resourceTag = Tag.getDatumCategory(Tag.DatumCategories.Resource.toString).id

    val resource = Resource.getLocalMongoResource

    val rnd = new RandomNameGenerator()

    val project = Datum(
      name = s"MockProject",
      resource = Some(resource.id),
      tags = Set(workspace.id, projectTag),
      info = Info(
        dict = Map(
          "project/number" -> Random.nextInt(100).toString(),
          "project/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
          "project/who" -> rnd.next()).toValunit)).insert

    val subjects = for {
      subi <- 1 to 10
    } yield Datum(
      name = s"Subject${subi}",
      resource = Some(resource.id),
      tags = Set(workspace.id, subjectTag),
      info = Info(
        dict = Map(
          "subject/number" -> Random.nextInt(100).toString(),
          "subject/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
          "subject/who" -> rnd.next()).toValunit,
        inheritedDict = project.info.dict ++ project.info.inheritedDict,
        ascendents = project.info.ascendents ::: List(Catname(project.getCategoryName.getOrElse("unknown"), project.name)))).insert

    subjects.map { subject =>
      val experiments = for {
        expi <- 1 to Random.nextInt(3) + 1
      } yield Datum(
        name = s"${subject.name}_Experiment${expi}",
        resource = Some(resource.id),
        tags = Set(workspace.id, experimentTag),
        info = Info(
          dict = Map(
            "experiment/number" -> Random.nextInt(100).toString(),
            "experiment/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
            "experiment/who" -> rnd.next()).toValunit,
          inheritedDict = subject.info.dict ++ subject.info.inheritedDict,
          ascendents = subject.info.ascendents ::: List(Catname(subject.getCategoryName.getOrElse("unknown"), subject.name)))).insert

      experiments.map { experiment =>
        val scans = for {
          sci <- 1 to Random.nextInt(4) + 1
        } yield Datum(
          name = s"${experiment.name}_Scan${sci}",
          resource = Some(resource.id),
          tags = Set(workspace.id, scanTag),
          info = Info(
            dict = Map(
              "scan/number" -> Random.nextInt(100).toString(),
              "scan/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
              "scan/who" -> rnd.next()).toValunit,
            inheritedDict = experiment.info.dict ++ experiment.info.inheritedDict,
            ascendents = experiment.info.ascendents ::: List(Catname(experiment.getCategoryName.getOrElse("unknown"), experiment.name)))).insert

        scans.map { scan =>
          val resources = for {
            ri <- 1 to 2
          } yield Datum(
            name = s"${scan.name}_Resource${ri}",
            resource = Some(resource.id),
            tags = Set(workspace.id, resourceTag),
            info = Info(
              dict = Map(
                "resource/number" -> Random.nextInt(100).toString(),
                "resource/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
                "resource/who" -> rnd.next()).toValunit,
              inheritedDict = scan.info.dict ++ scan.info.inheritedDict,
              ascendents = scan.info.ascendents ::: List(Catname(scan.getCategoryName.getOrElse("unknown"), scan.name)))).insert

          scan.copy(children = scan.children ++ resources.map(_.id).toSet).update
        }

        val reconstructions = for {
          reci <- 1 to Random.nextInt(4) + 1
        } yield Datum(
          name = s"${experiment.name}_Reconstruction${reci}",
          resource = Some(resource.id),
          tags = Set(workspace.id, reconstructionTag),
          info = Info(
            dict = Map(
              "reconstruction/number" -> Random.nextInt(100).toString(),
              "reconstruction/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
              "reconstruction/who" -> rnd.next()).toValunit,
            inheritedDict = experiment.info.dict ++ experiment.info.inheritedDict,
            ascendents = experiment.info.ascendents ::: List(Catname(experiment.getCategoryName.getOrElse("unknown"), experiment.name)))).insert

        reconstructions.map { reconstruction =>
          val resources = for {
            ri <- 1 to 2
          } yield Datum(
            name = s"${reconstruction.name}_Resource${ri}",
            resource = Some(resource.id),
            tags = Set(workspace.id, resourceTag),
            info = Info(
              dict = Map(
                "resource/number" -> Random.nextInt(100).toString(),
                "resource/foobar" -> (if (Random.nextBoolean()) "foo" else "bar"),
                "resource/who" -> rnd.next()).toValunit,
              inheritedDict = reconstruction.info.dict ++ reconstruction.info.inheritedDict,
              ascendents = reconstruction.info.ascendents ::: List(Catname(reconstruction.getCategoryName.getOrElse("unknown"), reconstruction.name)))).insert

          reconstruction.copy(children = reconstruction.children ++ resources.map(_.id).toSet).update
        }

        experiment.copy(children = experiment.children ++ scans.map(_.id).toSet ++ reconstructions.map(_.id).toSet).update
      }

      subject.copy(children = subject.children ++ experiments.map(_.id).toSet).update
    }

    project.copy(children = project.children ++ subjects.map(_.id).toSet).update

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
    val experimentCategory = Tag.datumCategoriesNameMap(Tag.DatumCategories.Experiment.toString)
    val resourceCategory = Tag.datumCategoriesNameMap(Tag.DatumCategories.Resource.toString)

    // Get some Datums
    val experimentData = Datum.findWithAllTagsNoPage(Set(workspace.id, experimentCategory.id)).toSeq
    val resourceData = Datum.findWithAllTagsNoPage(Set(workspace.id, resourceCategory.id)).toSeq
    val scanResourceData = resourceData.filter(_.info.ascendents.find(_.category.equalsIgnoreCase(Tag.DatumCategories.Scan.toString)).nonEmpty)
    val reconstructionResourceData = resourceData.filter(_.info.ascendents.find(_.category.equalsIgnoreCase(Tag.DatumCategories.Reconstruction.toString)).nonEmpty)

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
            val datum = scanResourceData(Random.nextInt(scanResourceData.size))
            ParamOrDatum(
              name = abstractPort.name,
              param = if (abstractPort.kind == PortKind.Param) Some(Random.alphanumeric.take(10).mkString) else None,
              datum = if (abstractPort.kind == PortKind.File) Some(DatumAndReplica(datum = datum.id)) else None)
          },
          outputs = someOfTheOPorts.map { abstractPort =>
            val datum = reconstructionResourceData(Random.nextInt(reconstructionResourceData.size))
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