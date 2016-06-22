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
package nl.amc.ebioscience.rosemary.services.processing.transformers.nsg

import play.api.Logger
import javax.inject._
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.mutable.HashMap
import org.bson.types.ObjectId
import nl.amc.ebioscience.rosemary.services.processing._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.core.datasource.Webdav
import nl.amc.ebioscience.rosemary.services.search.SearchWriter
import nl.amc.ebioscience.rosemary.services.CryptoService
import nl.amc.ebioscience.processingmanager.types.messaging.StatusContainerMessage

@Singleton
class TraculaTransformer @Inject() (searchWriter: SearchWriter)(
    implicit cryptoService: CryptoService) extends Transformer(Resource.getDefaultWebdavInstance) {

  // Port names
  val imageSessionPortName = "Image Session"
  val dtiPortName = "dmri"
  val mriPortName = "smri"
  val dtipreprocessingPortName = "dtipreprocessing"
  val freesurferPortName = "freesurfer_vis"
  val bedpostxPortName = "bedpostx"
  val traculaPortName = "tracall"
  val intermediateFreesurferPortName = "freesurfer_proc"

  /**
   * Tracula accepts Experiments that have at least one DTI Scan and one MRI Scan under them
   * These scans should have a replica on the Transformer Planet (Resource)
   */
  def revealDecepticons(cybertronian: Cybertronian): Option[Map[String, String]] = {
    val result = scala.collection.mutable.HashMap.empty[String, String]
    val tracula = cybertronian.application
    val dataPorts = cybertronian.dataPorts
    val paramPorts = cybertronian.paramPorts

    // Make sure each port has at least one data or parameter value, this is generic
    result ++= getMissingPorts(cybertronian)

    // Check the validity of input data on image session port according to the domain knowledge
    dataPorts.get(imageSessionPortName).map(_.foreach { datum =>
      // Check all data are an XNAT Experiment
      datum.getCategoryName match {
        case None => appendToMap(result, (imageSessionPortName -> s"Could not determine category of ${datum.name}!"))
        case Some(datumCat) =>
          if (datumCat != Tag.DatumCategories.Experiment.toString)
            appendToMap(result, (imageSessionPortName -> s"${datum.name} is not an XNAT Experiment!"))
          else {
            val scans = Datum.getChildren(datum).filter(isScan(_))

            val dtiScans = scans.filter(isDtiScan(_))
            // Acceptable DTI scans should have at least one resource with acceptable format and also with a replica on the planet
            val acceptableDtiScans = dtiScans.filter { dtiScan =>
              val acceptableResources = Datum.getChildren(dtiScan).filter(isResource(_)).filter(isDtiAcceptableFormat(_)).filter(hasReplicaOnThisPlanet(_))
              if (acceptableResources.size > 0) true else false
            }

            val mriScans = scans.filter(isMriScan(_))
            // Acceptable MRI scans should have at least one resource with acceptable format and also with a replica on the planet
            val acceptableMriScans = mriScans.filter { mriScan =>
              val acceptableResources = Datum.getChildren(mriScan).filter(isResource(_)).filter(isMriAcceptableFormat(_)).filter(hasReplicaOnThisPlanet(_))
              if (acceptableResources.size > 0) true else false
            }

            // Check each XNAT Experiment has at least one DTI and one MRI scan
            if (acceptableDtiScans.size < 1 || acceptableMriScans.size < 1)
              appendToMap(result, (imageSessionPortName -> s"Experiment ${datum.name} does not have at least one valid DTI and one valid MRI scan!"))
          }
      }
    })

    // Finally send the verdict
    if (result.isEmpty) None else Some(result.toMap)
  }

  /**
   * Transforms each Experiment into n*m Processings where
   * n is the number of eligible DTI scans, and m is the number of eligible MRI scans
   * For each Processing it also defines the location on which the results should be uploaded
   */
  def transform(cybertronian: Cybertronian): Seq[IOInflatedConcretePort] = {
    // TODO for each datum on port 1 generate n*m submission where n is the number of its DTI scans and m is the number of its MRI scans
    val application = cybertronian.application

    // Input ports known to the Rosemary Application
    val imageSessionAbstractPort = application.iPorts.find(_.name.equalsIgnoreCase(imageSessionPortName)).get

    // Input ports known to the Processing Manager
    val pmIPorts = application.pmApplication.iPorts
    val dtiAbstractPort = pmIPorts.find(_.name.equalsIgnoreCase(dtiPortName)).get
    val mriAbstractPort = pmIPorts.find(_.name.equalsIgnoreCase(mriPortName)).get

    // Output ports known to the Processing Manager
    val pmOPorts = application.pmApplication.oPorts
    val predtiAbstractPort = pmOPorts.find(_.name.equalsIgnoreCase(dtipreprocessingPortName)).get
    val bedpostxAbstractPort = pmOPorts.find(_.name.equalsIgnoreCase(bedpostxPortName)).get
    val intermediateFreesurferAbstractPort = pmOPorts.find(_.name.equalsIgnoreCase(intermediateFreesurferPortName)).get
    val freesurferAbstractPort = pmOPorts.find(_.name.equalsIgnoreCase(freesurferPortName)).get
    val traculaAbstractPort = pmOPorts.find(_.name.equalsIgnoreCase(traculaPortName)).get

    // For the output locations
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    val dateSuffix = dateFormatter.format(new Date())

    cybertronian.dataPorts.get(imageSessionPortName).get.map { xnatExperiment =>
      val imageSessionConcretePort = InflatedConcretePort(
        name = imageSessionAbstractPort.name,
        data = OnlyDatum(xnatExperiment))

      val scans = Datum.getChildren(xnatExperiment).filter(isScan(_))

      val dtiScans = scans.filter(isDtiScan(_))
      // (scan: Datum, resource: Datum)
      val dtiScansResources = dtiScans.flatMap { dtiScan =>
        val acceptableResources = Datum.getChildren(dtiScan).filter(isResource(_)).filter(isDtiAcceptableFormat(_)).filter(hasReplicaOnThisPlanet(_))
        // TODO if there is any preference in choosing a particular format implement it here
        if (acceptableResources.size > 0) Some(dtiScan -> acceptableResources.head) else None
      }

      val mriScans = scans.filter(isMriScan(_))
      // (scan: Datum, resource: Datum)
      val mriScansResources = mriScans.flatMap { mriScan =>
        val acceptableResources = Datum.getChildren(mriScan).filter(isResource(_)).filter(isMriAcceptableFormat(_)).filter(hasReplicaOnThisPlanet(_))
        // TODO if there is any preference in choosing a particular format implement it here
        if (acceptableResources.size > 0) Some(mriScan -> acceptableResources.head) else None
      }

      // n * m 
      for (dtiScanResource <- dtiScansResources; mriScanResource <- mriScansResources) yield {

        val dtiScan = dtiScanResource._1
        val dtiScanName = dtiScan.name.replaceAll(" ", "_")
        val dtiResource = dtiScanResource._2
        val dtiResourceName = dtiResource.name.replaceAll(" ", "_")
        val dtiResourceReplica = getReplicaOnThisPlanet(dtiResource)

        val mriScan = mriScanResource._1
        val mriScanName = mriScan.name.replaceAll(" ", "_")
        val mriResource = mriScanResource._2
        val mriResourceName = mriResource.name.replaceAll(" ", "_")
        val mriResourceReplica = getReplicaOnThisPlanet(mriResource)

        // Input ports
        val dtiConcretePort = InflatedConcretePort(
          name = dtiAbstractPort.name,
          data = ConcreteDatum(dtiResource, dtiResourceReplica))

        val mriConcretePort = InflatedConcretePort(
          name = mriAbstractPort.name,
          data = ConcreteDatum(mriResource, mriResourceReplica))

        // For creating empty directories for Processing Manager
        val webdav = new Webdav(planet)
        val appname = s"${application.name}_${dateSuffix}"
        val dtiBasePath = dtiResourceReplica.location.split('/').dropRight(1).mkString("/")
        webdav.createDirUnder(dtiBasePath, appname)
        val mriBasePath = mriResourceReplica.location.split('/').dropRight(1).mkString("/")
        webdav.createDirUnder(mriBasePath, appname)
        val traculaBasePath = mriResourceReplica.location.split('/').dropRight(2).mkString("/")
        webdav.createDirUnder(traculaBasePath, appname)

        // Output ports (with where to upload results)
        val predtiConcretePort = InflatedConcretePort(
          name = predtiAbstractPort.name,
          data = FutureDatum(planet.uri + s"/${dtiBasePath}/${appname}/predti__${dtiResourceName}.zip"))

        val bedpostxConcretePort = InflatedConcretePort(
          name = bedpostxAbstractPort.name,
          data = FutureDatum(planet.uri + s"/${dtiBasePath}/${appname}/bedpostx__${dtiResourceName}.zip"))

        val intermediateFreesurferConcretePort = InflatedConcretePort(
          name = intermediateFreesurferAbstractPort.name,
          data = FutureDatum(planet.uri + s"/${mriBasePath}/${appname}/intermediate_freesurfer__${mriResourceName}.zip"))

        val freesurferConcretePort = InflatedConcretePort(
          name = freesurferAbstractPort.name,
          data = FutureDatum(planet.uri + s"/${mriBasePath}/${appname}/freesurfer__${mriResourceName}.zip"))

        val traculaConcretePort = InflatedConcretePort(
          name = traculaAbstractPort.name,
          data = FutureDatum(planet.uri + s"/${traculaBasePath}/${appname}/tracula__${dtiResourceName}__${mriResourceName}.zip"))

        IOInflatedConcretePort(
          stems = Set(imageSessionConcretePort),
          inputs = Set(
            dtiConcretePort,
            mriConcretePort),
          outputs = Set(
            predtiConcretePort,
            intermediateFreesurferConcretePort,
            freesurferConcretePort,
            bedpostxConcretePort,
            traculaConcretePort))
      }
    }.flatten
  }

  /**
   * Take the information from the given StatusContainerMessage and update the respective Processing
   * in the data model. If it cannot find a Processing with respective StatusContainerMessage.id
   * it returns None
   */
  def getSpark(statusContainerMsg: StatusContainerMessage): Option[Processing] =
    Processing.findOneById(new ObjectId(statusContainerMsg.id)) map { processing =>
      Logger.debug(s"getting spark for ${processing.id}")

      val newConcretePorts = statusContainerMsg.processedOutputs.flatMap { nameUriMsgPart =>
        Logger.debug(s"processing this port: ${nameUriMsgPart}")

        processing.outputs.find(_.name == nameUriMsgPart.portName) match {

          case None => // it is a new output port
            val url = nameUriMsgPart.Uri
            Logger.debug(s"going to create a new concrete port for URI: ${url}")

            val processingGroupName = processing.processingGroup.map(_.name)
            val workspaceTags = Tag.findByIds(processing.tags).filter(_.isInstanceOf[WorkspaceTag]).map(_.asInstanceOf[WorkspaceTag])

            // Create a datum with XNAT Resource tag and add the url as its replica
            val resourceDatum = Datum(
              name = url.split('/').last.stripSuffix(".zip"),
              resource = Some(planet.id),
              pathOnResource = Some(url.stripPrefix(planet.uri)),
              replicas = Set(Replica(resource = planet.id, location = url.stripPrefix(planet.uri))),
              // creator = User.current.id, // TODO make creator an option, this will fail
              tags = workspaceTags.map(_.id) + Tag.getDatumCategory(Tag.DatumCategories.Resource.toString).id,
              info = Info(dict = Map("ProcessingName" -> Valunit(value = processingGroupName.getOrElse(processing.name))))).insert

            // Create a datum with XNAT Reconstruction tag and add the Resource datum as its child
            val reconDatum = Datum(
              name = url.split('/').dropRight(1).last,
              resource = Some(planet.id),
              children = Set(resourceDatum.id),
              pathOnResource = Some(url.stripPrefix(planet.uri).split('/').dropRight(1).mkString("/")),
              // creator = User.current.id, // TODO make creator an option, this will fail
              tags = workspaceTags.map(_.id) + Tag.getDatumCategory(Tag.DatumCategories.Reconstruction.toString).id,
              info = Info(dict = Map("ProcessingName" -> Valunit(value = processingGroupName.getOrElse(processing.name))))).insert

            // Index new data in the search engine
            searchWriter.add(resourceDatum)
            searchWriter.add(reconDatum)
            searchWriter.commit

            // Attach the Reconstruction datum to the correct datum (Scan or Experiment)
            val mriResourceDatumId = processing.inputs.find(_.name.equalsIgnoreCase(mriPortName)).flatMap(_.datum).map(_.datum)
            val mriScanDatum = mriResourceDatumId.flatMap(Datum.getSingleParent(_))
            val experimentDatum = mriScanDatum.flatMap(Datum.getSingleParent(_))
            val dtiResourceDatumId = processing.inputs.find(_.name.equalsIgnoreCase(dtiPortName)).flatMap(_.datum).map(_.datum)
            val dtiScanDatum = dtiResourceDatumId.flatMap(Datum.getSingleParent(_))
            nameUriMsgPart.portName match {
              case str if str.equalsIgnoreCase(dtipreprocessingPortName) | str.equalsIgnoreCase(bedpostxPortName) =>
                dtiScanDatum.map(_.addChild(reconDatum.id))
              case str if str.equalsIgnoreCase(freesurferPortName) =>
                mriScanDatum.map(_.addChild(reconDatum.id))
              case str if str.equalsIgnoreCase(traculaPortName) =>
                experimentDatum.map(_.addChild(reconDatum.id))
              case str if str.equalsIgnoreCase(intermediateFreesurferPortName) =>
                mriScanDatum.map(_.addChild(reconDatum.id))
              case str => Logger.error(s"Could not recognize Tracula port name: $str")
            }

            // Create a concrete port with the Xnat Resource datum and Replica (but don't store it in DB yet)
            val pf = ParamOrDatum(
              name = nameUriMsgPart.portName,
              param = None,
              datum = Some(DatumAndReplica(
                datum = resourceDatum.id,
                replica = Some(resourceDatum.replicas.head.id))))
            Some(pf)

          case Some(concretePort) =>
            Logger.debug(s"Concrete Port already exists for ${nameUriMsgPart.portName} with ${concretePort.datum.get}.")
            None
        }
      }

      // Update the Submission with the new information into the DB
      val newProgress = statusContainerMsg.statusDetails.progress.flatMap(toIntOpt(_))

      val newStatus = statusContainerMsg.lifeCycle

      val (utstatuses, uttags, utprogress) =
        processing.getToUpdateStatusesTagsProgress(newStatus, newProgress)

      processing.copy(
        outputs = processing.outputs ++ newConcretePorts,
        progress = utprogress,
        statuses = utstatuses,
        tags = uttags).update
    }

  private def toIntOpt(str: String): Option[Int] =
    try {
      Some(str.toInt)
    } catch {
      case e: Exception =>
        Logger.error(s"Could not convert $str to Integer")
        None
    }

  /**
   * Add a new String to a hashmap with an id (port id)
   * If the hashmap already contains the id the String is appended to the existing String
   */
  private def appendToMap(hashMap: HashMap[String, String], newItem: (String, String)): HashMap[String, String] =
    hashMap.get(newItem._1) match {
      case None           => hashMap += newItem
      case Some(oldValue) => hashMap += (newItem._1 -> s"$oldValue, ${newItem._2}")
    }

  /** Tests if the category tag of the datum is "Scan" */
  private def isScan(datum: Datum): Boolean = isCategory(datum, Tag.DatumCategories.Scan)
  /** Tests if the category tag of the datum is "Resource" */
  private def isResource(datum: Datum): Boolean = isCategory(datum, Tag.DatumCategories.Resource)

  /**
   * A "Scan" datum is DTI if its "Scan/type"
   * contains "DTI" or starts with "DIFF"
   * and "Scan/frames" > 125
   */
  private def isDtiScan(datum: Datum): Boolean =
    datum.info.dict.get(s"${Tag.DatumCategories.Scan.toString}/type") match {
      case Some(stype) =>
        val stv = stype.value
        if (stv.toUpperCase.contains("DTI") || stv.toUpperCase.startsWith("DIFF")) {
          datum.info.dict.get(s"${Tag.DatumCategories.Scan.toString}/frames") match {
            case Some(frames) =>
              if (frames.value.toInt > 124) {
                Logger.trace(s"Datum ${datum.id} is an acceptable DTI scan.")
                true
              } else {
                Logger.trace(s"Datum ${datum.id} has ${frames} frames that is less than 125.")
                false
              }
            case None =>
              Logger.warn(s"Datum ${datum.id} does not have any ${Tag.DatumCategories.Scan.toString}/frames information."); false
          }
        } else {
          Logger.trace(s"Datum ${datum.id} has type ${stype} that means it is not a DTI scan.")
          false
        }
      case None =>
        Logger.warn(s"Datum ${datum.id} does not have any ${Tag.DatumCategories.Scan.toString}/type information."); false
    }

  /**
   * A "Scan" datum is MRI if its "Scan/type"
   * contains "MPRAGE" or "ADNI" or "T1"
   */
  private def isMriScan(datum: Datum): Boolean = datum.info.dict.get(s"${Tag.DatumCategories.Scan.toString}/type") match {
    case Some(stype) =>
      val stv = stype.value
      if (stv.toUpperCase.contains("MPRAGE") || stv.toUpperCase.contains("ADNI") || stv.toUpperCase.contains("T1")) {
        Logger.trace(s"Datum ${datum.id} is an acceptable MRI scan.")
        true
      } else {
        Logger.trace(s"Datum ${datum.id} has type ${stype} that means it is not a MRI scan.")
        false
      }
    case None =>
      Logger.warn(s"Datum ${datum.id} does not have any ${Tag.DatumCategories.Scan.toString}/type information."); false
  }

  /**
   * A "Resource" datum is acceptable for DTI Preprocessing if
   * its "Resource/format" contains "DICOM" or "PARREC"
   */
  private def isDtiAcceptableFormat(datum: Datum) = isAcceptableFormat(datum, "DICOM", "PARREC")

  /**
   * A "Resource" datum is acceptable for Freesurfer if
   * its "Resource/format" contains "DICOM" or "NIFTI"
   */
  private def isMriAcceptableFormat(datum: Datum) = isAcceptableFormat(datum, "DICOM", "NIFTI")

  /**
   * A "Resource" datum is acceptable for an application if
   * its "Resource/format" contains "format1" or "format2"
   */
  private def isAcceptableFormat(datum: Datum, format1: String, format2: String): Boolean =
    datum.info.dict.get(s"${Tag.DatumCategories.Resource.toString}/format") match {
      case Some(format) =>
        if (format.value.toUpperCase.contains(format1) || format.value.toUpperCase.contains(format2)) true else false
      case None =>
        Logger.warn(s"Datum ${datum.id} does not have any ${Tag.DatumCategories.Resource.toString}/format information."); false
    }
}
