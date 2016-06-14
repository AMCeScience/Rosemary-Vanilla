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

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import scala.util.DynamicVariable
import com.mongodb.casbah.Imports._
import play.api.Logger
import org.mindrot.BCrypt

case class User(
    email: String,
    password: String,
    name: String,
    approved: Boolean = false,
    active: Boolean = true,
    // TODO: Think about user roles: for global access and view, impersonation. Maybe put in Info?
    role: Role.Value = Role.TeamMember,
    credentials: List[Credential] = Nil,
    id: User.Id = new User.Id,
    info: Info = new Info) extends BaseEntity {

  def getTagsOwned = Tag.findForUserAsOwner(id)
  def getTagsMembered = Tag.findForUserAsMember(id)
  def getTagsHasAccess = Tag.findForUserHasAccess(id)
  def getWorkspaceTagsHasAccess = Tag.findWorkspaceTagsUserHasAccess(id)
  def getUserTagsHasAccess = Tag.findUserTagsUserHasAccess(id)

  def toggleActive = copy(active = !active).update
  def toggleApproved = copy(approved = !approved).update

  /**
   * Adds or updates a credential to the [[nl.amc.ebioscience.rosemary.models.User]]
   * @param cred the [[nl.amc.ebioscience.rosemary.models.Credential]] to add or update
   */
  def addCredential(cred: Credential) =
    copy(credentials = cred :: credentials.filterNot(_.resource == cred.resource)).update

  def hashPassword = copy(password = BCrypt.hashpw(password, BCrypt.gensalt()))
}

case class Credential(
  resource: Resource.Id,
  username: String,
  password: String)

object User extends DefaultModelBase[User]("users") {
  // TODO: uniqueness is applied for the combined index, which means it is possible to have multiple users with the same email! should be fixed...
  collection.createIndex(
    ("email" $eq 1) ++ ("_id" $eq 1),
    ("name" $eq "user_email") ++ ("unique" $eq true))

  /**
   * DynamicVariable is used, when you need to do a computation within an enclosed scope,
   * where every thread has it's own copy of the variable's value.
   * Use method `current` instead of this
   */
  val current_id = new DynamicVariable[Option[User.Id]](None)
  /**
   * Gets the current user, this is the method you should use mostly
   */
  def current: User = {
    current_id.value match {
      case Some(id) => findOne("_id" $eq id) match {
        case Some(user) => user
        case None       => throw new Throwable("Unknown user defined in current thread context.")
      }
      case None => throw new Throwable("No user defined in current thread context.")
    }
  }

  def credentialFor(resource: Resource.Id): Option[Credential] =
    current.credentials.find(credential =>
      credential.resource.equals(resource) && !credential.username.isEmpty && !credential.password.isEmpty)

  def find(email: String): Option[User] = findOne("email" $eq email)

  def authenticate(email: String, password: String): Option[User] = {
    findOne($and("email" $eq email, "active" $eq true, "approved" $eq true)).map { user =>
      if (BCrypt.checkpw(password, user.password)) Some(user) else None
    }.flatten
  }
}

object Role extends Enumeration {
  val Admin, TeamMember = Value
}
