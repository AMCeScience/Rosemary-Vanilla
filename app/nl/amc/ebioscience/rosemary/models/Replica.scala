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
import com.novus.salat.annotations._

/**
  * @param location Relative location, for example "data/project/file.txt"
  * @param uri Do not use this, it is overwritten by [[WithReplica.getReplicas]] method
  */
case class Replica(
  resource: Resource.Id,
  location: String,
  originalName: Option[String] = None,
  contentType: Option[String] = None,
  hash: Option[String] = None,
  uri: Option[String] = None,
  valid: Boolean = true,
  id: Replica.Id = new Replica.Id,
  info: Info = new Info) extends BaseEntity

object Replica {
  type Id = DefaultModelBase.Id
}

@Salat
trait WithReplica {
  val replicas: Set[Replica]

  def getReplicas = {
    val resourceUris = Resource.findByIds(replicas.map(_.resource)).map { r => (r.id, r.uri) }.toMap
    replicas.map { rep => rep.copy(uri = Some(s"${resourceUris(rep.resource)}/${rep.location}")) }
  }

  def getReplica(resource: Resource.Id) = replicas.find(_.resource == resource)
}
