package nl.amc.ebioscience.rosemary.core.processing.transformers

import nl.amc.ebioscience.rosemary.core.processing._
import nl.amc.ebioscience.rosemary.models.{ Processing, Resource }
import nl.amc.ebioscience.processingmanager.types.messaging.StatusContainerMessage

object MockTransformer extends Transformer(Resource.getDefaultWebdavInstance) {

  def revealDecepticons(cybertronian: Cybertronian): Option[Map[String, String]] = None
  def transform(cybertronian: Cybertronian): Seq[IOInflatedConcretePort] = ???
  def getSpark(statusContainerMsg: StatusContainerMessage): Option[Processing] = ???
}
