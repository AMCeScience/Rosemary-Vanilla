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

trait Rights {
  def isOwner(id: User.Id): Boolean
  def isMember(id: User.Id): Boolean
  def hasAccess(id: User.Id) = isOwner(id) || isMember(id)
}

case class Everyone() extends Rights {
  def isOwner(id: User.Id) = false
  def isMember(id: User.Id) = true
}

case class Nobody() extends Rights {
  def isOwner(id: User.Id) = false
  def isMember(id: User.Id) = false
}

case class Personal(owner: User.Id) extends Rights {
  def isOwner(id: User.Id) = id == owner
  def isMember(id: User.Id) = false
}

case class Membered(owner: User.Id, members: Set[User.Id] = Set.empty) extends Rights {
  def isOwner(id: User.Id) = id == owner
  def isMember(id: User.Id) = members.contains(id)
  def addMember(id: User.Id) = if (owner != id) copy(members = members + id) else this
  def removeMember(id: User.Id) = copy(members = members - id)
}
