package warwick.sso

import uk.ac.warwick.util.core.StringUtils

import scala.collection.JavaConverters._

case class Usercode(string: String) {
  require(string != null && !string.isEmpty, "Usercode must not be empty")
}
case class UniversityID(string: String)
case class Name(val first: Option[String], val last: Option[String]) {
  def full = Option(Seq(first,last).flatten.mkString(" ")).filterNot(_.isEmpty)
}
case class Department(shortName: Option[String], name: Option[String], code: Option[String])

sealed trait UserType
object UserType {
  /** Staff BUT NOT PG */
  case object Staff extends UserType
  /** Student BUT NOT PG */
  case object Student extends UserType
  case object PostgradTeaching extends UserType
  case object PostgradResearch extends UserType
  case object Alumni extends UserType
  case object Unrecognised extends UserType
}

/**
 * A more Scala version of Userlookup's User.
 */
case class User(
  usercode: Usercode,
  universityId: Option[UniversityID],
  name: Name,
  email: Option[String],
  department: Option[Department],
  userType: UserType,

  /**
   * Please don't use this to implement app logic.
   * If you find you have to use it, please make or request
   * a change to this User type to make whatever it is you're doing
   * a proper thing. It would only be acceptable to do this for some
   * user diagnostic screen where you want to display everything
   * received from SSO.
   */
  rawProperties: Map[String, String]
)

object User {

  import warwick.sso.utils.Strings._

  private[sso] def apply(u: uk.ac.warwick.userlookup.User): User = User(
    usercode = Usercode(u.getUserId),
    universityId = notEmptyOption(u.getWarwickId).map(UniversityID),
    name = Name(notEmptyOption(u.getFirstName), notEmptyOption(u.getLastName)),
    email = notEmptyOption(u.getEmail),
    department = Some(Department(notEmptyOption(u.getShortDepartment), notEmptyOption(u.getDepartment), notEmptyOption(u.getDepartmentCode))),
    userType = userTypeOf(u),

    rawProperties = u.getExtraProperties.asScala.toMap
  )

  def userTypeOf(u: uk.ac.warwick.userlookup.User): UserType = {
    import UserType._
    val attributes = u.getExtraProperties
    val legacyUserType = attributes.get("urn:websignon:usertype")

    if (legacyUserType == "Alumni" || attributes.get("alumni") == "true") {
      Alumni
    } else {
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
  }

  def hasUsercode(user: uk.ac.warwick.userlookup.User): Boolean = StringUtils.hasText(user.getUserId)

}
