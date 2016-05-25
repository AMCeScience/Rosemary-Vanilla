package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import scala.util.DynamicVariable
import com.mongodb.casbah.Imports._
import play.api.Logger

// TODO: Think about user roles: for global access and view, impersonation. Maybe put in Info?
case class User(
    email: String,
    password: String, // TODO: Hash Password
    name: String,
    approved: Boolean = false,
    active: Boolean = true,
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

  /** Adds or updates a credential to the [[nl.amc.ebioscience.rosemary.models.User]]
    * @param cred the [[nl.amc.ebioscience.rosemary.models.Credential]] to add or update
    */
  def addCredential(cred: Credential) =
    copy(credentials = cred :: credentials.filterNot(_.resource == cred.resource)).update
}

case class Credential(
  resource: Resource.Id,
  username: String,
  password: String)

object User extends DefaultModelBase[User]("users") {
  // TODO: uniqueness is applied for the combined index, which means it is possible to have multiple users with the same email! should be fixed...
  collection.ensureIndex(("email" -> 1, "_id" -> 1), "user_email", unique = true)

  /** DynamicVariable is used, when you need to do a computation within an enclosed scope,
    * where every thread has it's own copy of the variable's value.
    * Use method `current` instead of this
    */
  val current_id = new DynamicVariable[Option[User.Id]](None)
  /** Gets the current user, this is the method you should use mostly
    */
  def current: User = {
    current_id.value match {
      case Some(id) => findOne("_id" -> id) match {
        case Some(user) => user
        case None       => throw new Throwable("Unknown user defined in current thread context.")
      }
      case None => throw new Throwable("No user defined in current thread context.")
    }
  }

  def credentialFor(resource: Resource.Id): Option[Credential] =
    current.credentials.find(credential => 
      credential.resource.equals(resource) && !credential.username.isEmpty && !credential.password.isEmpty)

  def find(email: String): Option[User] = findOne(("email" -> email))

  def authenticate(email: String, password: String): Option[User] =
    findOne(("email" -> email, "password" -> password, "active" -> true, "approved" -> true))
}

object Role extends Enumeration {
  val Admin, TeamMember = Value
}
