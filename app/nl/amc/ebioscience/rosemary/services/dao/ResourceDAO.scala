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
package nl.amc.ebioscience.rosemary.services.dao

import javax.inject._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat.PlaySalat
import nl.amc.ebioscience.rosemary.services.{ MongoContext, ConfigService }
import nl.amc.ebioscience.rosemary.models.core.DefaultDAO
import nl.amc.ebioscience.rosemary.models.ResourceKind
import nl.amc.ebioscience.rosemary.models.Resource

class ResourceDAO @Inject() (configService: ConfigService, mctx: MongoContext, pls: PlaySalat)
    extends DefaultDAO[Resource]("resources", mctx, pls) {

  def findResourceByHostname(hostname: String) = findOne("host" $eq hostname)

  val defaultWebdavHost = configService.getStringConfig("webdav.host.default")
  /** Helper method to get a single WebDAV resource instance */
  def getDefaultWebdavInstance = findOne($and("kind" $eq ResourceKind.Webdav.toString, "host" $eq defaultWebdavHost)).get
  def getLocalMongoResource = findOne(("kind" $eq ResourceKind.Mongodb.toString)).get
}
