package nl.amc.ebioscience.rosemary.models.core

import org.bson.types.ObjectId
import com.novus.salat.Context
import se.radley.plugin.salat.PlaySalat
import nl.amc.ebioscience.rosemary.services.MongoContext
import play.api.Play

/**
 * Defines the default ModelBase
 * The default ModelBase has the Id type of ObjectId
 */
class DefaultModelBase[T <: BaseEntity](name: String)(
  implicit mot: Manifest[T],
  mid: Manifest[DefaultModelBase.Id],
  ctx: Context, //= Play.current.injector.instanceOf(classOf[MongoContext]).salatContext,
  ps: PlaySalat) // = Play.current.injector.instanceOf(classOf[PlaySalat]))
    extends ModelBase[T, DefaultModelBase.Id](name) {

  class DefaultModelChild[T <: BaseEntity](name: String)(
    implicit mot: Manifest[T], mid: Manifest[DefaultModelBase.Id])
      extends ModelChild[T, DefaultModelBase.Id](name)

  type Id = DefaultModelBase.Id
}

object DefaultModelBase {
  type Id = ObjectId
}
