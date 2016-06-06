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
package nl.amc.ebioscience.rosemary.core

object HelperTools {

  /** Converts List[Either[A, B]] into Either[List[A], List[B]]
    * Right is the only option, which means this method discards rights if there is at least one left
    */
  def evertEitherList[A, B](eithersList: Iterable[Either[A, B]]): Either[Iterable[A], Iterable[B]] =
    eithersList.partition(_.isLeft) match {
      case (Nil, rs) => Right(for (Right(r) <- rs) yield r)
      case (ls, _)   => Left(for (Left(l) <- ls) yield l)
    }

  def evertEitherListRights[A, B](eithersList: Iterable[Either[A, B]]): Either[Iterable[A], Iterable[B]] =
    eithersList.partition(_.isLeft) match {
      case (_, rs) => Right(for (Right(r) <- rs) yield r)
    }

  def evertEitherListLefts[A, B](eithersList: Iterable[Either[A, B]]): Either[Iterable[A], Iterable[B]] =
    eithersList.partition(_.isLeft) match {
      case (ls, _) => Left(for (Left(l) <- ls) yield l)
    }

  def evertEitherListBoth[A, B](eithersList: Iterable[Either[A, B]]): (Iterable[A], Iterable[B]) =
    eithersList.partition(_.isLeft) match {
      case (ls, rs) => (for (Left(l) <- ls) yield l, for (Right(r) <- rs) yield r)
    }
}
