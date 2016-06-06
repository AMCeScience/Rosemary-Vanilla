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
