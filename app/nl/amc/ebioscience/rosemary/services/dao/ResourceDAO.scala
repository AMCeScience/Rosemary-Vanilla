package nl.amc.ebioscience.rosemary.services.dao

import javax.inject._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat.PlaySalat
import nl.amc.ebioscience.rosemary.services.{ MongoContext, Config }
import nl.amc.ebioscience.rosemary.models.core.DefaultDAO
import nl.amc.ebioscience.rosemary.models.ResourceKind
import nl.amc.ebioscience.rosemary.models.Resource

class ResourceDAO @Inject() (rosemaryConfig: Config, mctx: MongoContext, pls: PlaySalat)
    extends DefaultDAO[Resource]("resources", mctx, pls) {

  def findResourceByHostname(hostname: String) = findOne("host" $eq hostname)

  val defaultWebdavHost = rosemaryConfig.getConfig("webdav.host.default")
  /** Helper method to get a single WebDAV resource instance */
  def getDefaultWebdavInstance = findOne($and("kind" $eq ResourceKind.Webdav.toString, "host" $eq defaultWebdavHost)).get
  def getLocalMongoResource = findOne(("kind" $eq ResourceKind.Mongodb.toString)).get
}
