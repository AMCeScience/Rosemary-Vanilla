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
package nl.amc.ebioscience.rosemary.services.processing

import nl.amc.ebioscience.rosemary.models.{ Datum, Replica }

/**
 * To capture information about input and output ports of processing entities
 */
sealed abstract class Container

/**
 * Port containing a Parameter
 *
 * @param constant value of the parameter
 */
case class Param(constant: String) extends Container

/**
 * Port containing a concrete [[models.Datum]] and a specific [[models.Replica]]
 *
 * @param datum The [[models.Datum]] used in this port
 * @param replica The specific [[models.Replica]] of that `Datum`
 */
case class ConcreteDatum(datum: Datum, replica: Replica) extends Container

/**
 * Port containing a URL for expected location of a result
 *
 * @param url Expected location of a result
 */
case class FutureDatum(url: String) extends Container

/**
 * Port containing only a [[models.Datum]] (without any `Replica`)
 *
 * @param datum The [[models.Datum]] used in this port
 */
case class OnlyDatum(datum: Datum) extends Container
