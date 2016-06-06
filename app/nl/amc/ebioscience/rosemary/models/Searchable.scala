package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import com.novus.salat.annotations._

@Salat
trait Searchable extends WithTags {
  val name: String
}

object Searchable {
  type Id = DefaultModelBase.Id
} 
