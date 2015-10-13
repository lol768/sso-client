package warwick.sso

import uk.ac.warwick.userlookup

case class Usercode(code: String)
case class UniversityId(id: String)
case class Name(val first: Option[String], val last: Option[String]) {
  def full = Option(Seq(first,last).flatten.mkString(" ")).filterNot(_.isEmpty)
}
case class Department(shortName: Option[String], name: Option[String], code: Option[String])

sealed trait UserType
object UserType {
  case object Staff extends UserType
  case object Student extends UserType
  case object PostgradTeaching extends UserType
  case object PostgradResearch extends UserType
  case object Unrecognised extends UserType
}

/**
 * A more Scala version of Userlookup's User.
 */
case class User(
  usercode: Usercode,
  universityId: Option[UniversityId],
  name: Name,
  email: Option[String],
  department: Option[Department],
  userType: UserType
)

object User {

  def userTypeOf(u: uk.ac.warwick.userlookup.User): UserType = {
    import UserType._
    val attributes = u.getExtraProperties
    (attributes.get("warwickitsclass"), attributes.get("warwickcategory")) match {
      case ("PG(R)", _) => PostgradResearch
      case ("PG(T)", _) => PostgradTeaching
      case ("Staff", _) => Staff
      case ("Student", _) => Student
      case (_, "R") => PostgradResearch
      case (_, "T") => PostgradTeaching
      case _ => Unrecognised
    }
  }

  private def apply(u: uk.ac.warwick.userlookup.User): User = User(
    usercode = Usercode(u.getUserId),
    universityId = Option(u.getWarwickId).map(UniversityId),
    name = Name(Option(u.getFirstName), Option(u.getLastName)),
    email = Option(u.getEmail),
    department = Some(Department(Option(u.getShortDepartment), Option(u.getDepartment), Option(u.getDepartmentCode))),
    userType = userTypeOf(u)
  )
}
