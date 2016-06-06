package nl.amc.ebioscience.rosemary.models.core

import play.api.Application
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat
import com.novus.salat.Context

object Implicits {
  import scala.language.implicitConversions

  implicit val app: play.api.Application = play.api.Play.current

  private val mongoContextCache = Application.instanceCache[MongoContext]
  private val playSalatCache = Application.instanceCache[PlaySalat]

  implicit def mongoContext(implicit application: Application): MongoContext = mongoContextCache(application)
  implicit def salatContext(implicit application: Application): Context = mongoContext(application).salatContext
  implicit def playSalat(implicit application: Application): PlaySalat = playSalatCache(application)
}