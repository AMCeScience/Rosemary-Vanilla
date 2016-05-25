package nl.amc.ebioscience.rosemary.models.core

import com.novus.salat.Context
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat
import org.bson.types.ObjectId

/**
 * WIP: To replace DefaultModelBase
 */
class DefaultDAO[T <: BaseEntity](name: String, mctx: MongoContext, pls: PlaySalat)(
    implicit mot: Manifest[T],
    mid: Manifest[DefaultDAO.Id],
    ctx: Context = mctx.salatContext,
    ps: PlaySalat = pls) extends ModelBase[T, DefaultDAO.Id](name) {

  class DefaultChildDAO[T <: BaseEntity](name: String)(
    implicit mot: Manifest[T], mid: Manifest[DefaultDAO.Id])
      extends ModelChild[T, DefaultDAO.Id](name)

  type Id = DefaultDAO.Id
}

object DefaultDAO {
  type Id = ObjectId
}
        