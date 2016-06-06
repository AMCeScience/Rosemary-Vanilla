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
