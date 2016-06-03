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
