package warwick.sso

object Users {
  /**
   * Creates a User, with most of the attributes having optional defaults.
   * If you pass just a Usercode, you will get an active user who isn't any
   * user type and has no other information.
   */
  def create(
    usercode: Usercode,
    universityId: Option[UniversityID] = None,
    name: Name = Name(None, None),
    email: Option[String] = None,
    department: Option[Department] = Some(Department(None, None, None)),

    staff: Boolean = false,
    student: Boolean = false,
    pgr: Boolean = false,
    alumni: Boolean = false,

    found: Boolean = true,
    verified: Boolean = true,
    disabled: Boolean = false,

    properties: Map[String,String] = Map.empty
  ): User =
    User(
      usercode = usercode,
      universityId = universityId,
      name = name,
      email = email,
      department = department,
      isStaffOrPGR = staff || pgr,
      isStaffNotPGR = staff,
      isStudent = student,
      isAlumni = alumni,
      isFound = found,
      isVerified = verified,
      isLoginDisabled = disabled,
      rawProperties = properties
    )

}