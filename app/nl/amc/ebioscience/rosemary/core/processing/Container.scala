package nl.amc.ebioscience.rosemary.core.processing

import nl.amc.ebioscience.rosemary.models.{ Datum, Replica }

sealed abstract class Container

case class Param(constant: String) extends Container
case class ConcreteDatum(datum: Datum, replica: Replica) extends Container
case class FutureDatum(url: String) extends Container
case class OnlyDatum(datum: Datum) extends Container
