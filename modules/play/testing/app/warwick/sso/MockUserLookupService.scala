package warwick.sso

import javax.inject.Inject
import play.api.{ConfigLoader, Configuration}

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

/**
  * Mock UserLookupService with static user data based on application Configuration.
  *
  * Only the usercode property is required.
  *
  * userlookup.test-data.users = [
  *   {
  *     usercode = "cuscav"
  *     universityId = "0672089"
  *     firstName = "Mathew"
  *     lastName = "Mannion"
  *     email = "M.Mannion@warwick.ac.uk"
  *     departmentShortName = "IT Services"
  *     departmentName = "Information Technology Services"
  *     departmentCode = "IN"
  *     userSource = "WarwickADS"
  *     staff = true
  *     extraProperties = {
  *       warwickattendancemode: "F"
  *     }
  *   }
  * ]
  */
class MockUserLookupService @Inject()(
 config: Configuration
) extends UserLookupService {

  lazy val users: Seq[User] = config.get[Seq[User]]("userlookup.test-data.users")

  implicit val userLoader: ConfigLoader[Seq[User]] = ConfigLoader { config => path => {
    config.getConfigList(path).asScala.map { gc =>
      val conf = Configuration(gc)
      Users.create(
        Usercode(conf.get[String]("usercode")),
        conf.getOptional[String]("universityId").map(UniversityID.apply),
        Name(conf.getOptional[String]("firstName"), conf.getOptional[String]("lastName")),
        conf.getOptional[String]("email"),
        Some(Department(conf.getOptional[String]("departmentShortName"), conf.getOptional[String]("departmentName"), conf.getOptional[String]("departmentCode"))),
        conf.getOptional[String]("userSource"),
        conf.getOptional[Boolean]("staff").getOrElse(false),
        conf.getOptional[Boolean]("student").getOrElse(false),
        conf.getOptional[Boolean]("pgr").getOrElse(false),
        conf.getOptional[Boolean]("pgt").getOrElse(false),
        conf.getOptional[Boolean]("undergraduate").getOrElse(false),
        conf.getOptional[Boolean]("alumni").getOrElse(false),
        conf.getOptional[Boolean]("found").getOrElse(true),
        conf.getOptional[Boolean]("verified").getOrElse(true),
        conf.getOptional[Boolean]("disabled").getOrElse(false),
        conf.getOptional[Map[String, String]]("extraProperties").getOrElse(Map.empty)
      )
    }
  }}

  private def findUser(usercode: Usercode): Option[User] = users.find(_.usercode == usercode)
  private def findUser(universityID: UniversityID): Option[User] = users.find(_.universityId.contains(universityID))

  override def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]] =
    Success(codes.flatMap(findUser(_).toSeq).map { u => u.usercode -> u }.toMap)

  override def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean): Try[Map[UniversityID, User]] =
    Success(ids.flatMap(findUser(_).toSeq).map { u => u.universityId.get -> u }.toMap)

  override def searchUsers(filters: Map[String, String], includeDisabled: Boolean): Try[Seq[User]] =
    Success(filters.toSeq.flatMap { case (name, rawValue) =>
      val (value, wildcard) =
        if (rawValue.endsWith("*")) (rawValue.substring(0, rawValue.length - 1), true)
        else (rawValue, false)

      def matches(attr: String) =
        if (wildcard) attr.toLowerCase.startsWith(value)
        else attr.equalsIgnoreCase(value)

      name.toLowerCase match {
        case "givenname" => users.filter(_.name.first.exists(matches))
        case "sn" => users.filter(_.name.last.exists(matches))
        case "warwickuniid" => users.filter(_.universityId.map(_.string).exists(matches))
        case "cn" => users.filter { u => matches(u.usercode.string) }
      }
    }.distinct.filter { u => includeDisabled || !u.isLoginDisabled })

  override def basicAuth(usercode: String, password: String): Try[Option[User]] =
    Success(findUser(Usercode(usercode)))

}