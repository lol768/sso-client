package warwick.sso

import uk.ac.warwick.util.core.StringUtils

import scala.collection.JavaConverters._

case class Usercode(string: String) {
  require(string != null && !string.isEmpty, "Usercode must not be empty")
}
case class UniversityID(string: String)
case class Name(first: Option[String], last: Option[String]) {
  def full: Option[String] = Option(Seq(first,last).flatten.mkString(" ")).filterNot(_.isEmpty)
}
case class Department(shortName: Option[String], name: Option[String], code: Option[String])

/**
 * A more Scala version of Userlookup's User.
 */
case class User(
  usercode: Usercode,
  universityId: Option[UniversityID],
  name: Name,
  email: Option[String],
  department: Option[Department],

  /**
   * Can be used to check for WBS users (== 'WBSLdap'), probably
   * amongst other things
   */
  userSource: Option[String],

  isStaffOrPGR: Boolean,
  isStaffNotPGR: Boolean,
  isStudent: Boolean,
  isAlumni: Boolean,

  /** Does this represent a user that exists? */
  isFound: Boolean,
  /**
   * If `isFound` and `isVerified` are both false,
   * this means there was some server problem looking up this
   * user and we can't be sure that they definitely don't exist.
   */
  isVerified: Boolean,
  /**
   * Matches the logindisabled attribute on the user.
   */
  isLoginDisabled: Boolean,

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

  def apply(u: uk.ac.warwick.userlookup.User): User = User(
    usercode = Usercode(u.getUserId),
    universityId = notEmptyOption(u.getWarwickId).map(UniversityID),
    name = Name(notEmptyOption(u.getFirstName), notEmptyOption(u.getLastName)),
    email = notEmptyOption(u.getEmail),
    department = Some(Department(notEmptyOption(u.getShortDepartment), notEmptyOption(u.getDepartment), notEmptyOption(u.getDepartmentCode))),

    userSource = notEmptyOption(u.getUserSource),

    isStaffOrPGR = u.isStaff,
    isStaffNotPGR = u.isStaff && !isPGR(u),
    isStudent    = u.isStudent,
    isAlumni = u.isAlumni,

    isFound = u.isFoundUser,
    isVerified = u.isVerified,
    isLoginDisabled = u.isLoginDisabled,

    rawProperties = u.getExtraProperties.asScala.toMap
  )

  def isPGR(u: uk.ac.warwick.userlookup.User): Boolean = {
    val attributes = u.getExtraProperties
    import attributes.get
    get("warwickitsclass") == "PG(R)" || get("warwickcategory") == "R"
  }

  def hasUsercode(user: uk.ac.warwick.userlookup.User): Boolean = StringUtils.hasText(user.getUserId)

  // A user that was not found by user look-up, but where you want to remember the usercode requested
  def unknown(usercode: Usercode): User = User(
    usercode = usercode,
    universityId = None,
    name = Name(None, None),
    email = None,
    department = None,
    userSource = None,
    isStaffOrPGR = false,
    isStaffNotPGR = false,
    isStudent = false,
    isAlumni = false,
    isFound = false,
    isVerified = true,
    isLoginDisabled = false,
    rawProperties = Map.empty
  )

}
