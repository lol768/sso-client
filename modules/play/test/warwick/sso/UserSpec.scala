package warwick.sso

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.userlookup.{AnonymousUser, UserBuilder}
import warwick.sso.UserType._

import scala.collection.JavaConverters._

/**
 * Created by nick on 14/10/15.
 */
class UserSpec extends PlaySpec {
  import TableDrivenPropertyChecks._

  type OldUser = uk.ac.warwick.userlookup.User

  "User" should {

    "convert user type from old User" in {

      val users = Table(
        ("Attributes", "UserType"),
        (Map("warwickitsclass" -> "PG(R)"), PostgradResearch),
        (Map("warwickcategory" -> "R"), PostgradResearch),
        (Map("warwickitsclass" -> "PG(R)", "warwickcategory" -> "T"), PostgradResearch), // itsclass takes priority
        (Map("warwickitsclass" -> "PG(T)", "warwickcategory" -> "R"), PostgradTeaching),
        (Map("warwickitsclass" -> "Staff", "warwickcategory" -> "X"), Staff),
        (Map("warwickitsclass" -> "PG(T)", "warwickcategory" -> "R", "urn:websignon:usertype"->"Alumni"), Alumni),
        (Map("warwickitsclass" -> "Student"), Student)
      )

      forAll(users) { (attrs, userType) =>
        val oldUser = new UserBuilder().populateUser(attrs.asJava)
        oldUser.setUserId("Something")
        val user = User(oldUser)
        user.userType must be (userType)
      }

    }

    "convert name from old user" in {
      val names = Table(
        ("first","last","full"),
        ("Jim", "Jimson", Some("Jim Jimson")),
        (null, "Jimson", Some("Jimson")),
        ("Jim", null, Some("Jim")),
        (null, null, None),
        ("", "Jimson", Some("Jimson")),
        ("Jim", "", Some("Jim")),
        ("", null, None)
      )
      forAll(names) { (first, last, full) =>
        val u = new OldUser("someone")
        u.setFirstName(first)
        u.setLastName(last)
        val user = User(u)
        user.name.full must be (full)
        user.name.first must be (Option(first).filterNot(_.isEmpty))
        user.name.last must be (Option(last).filterNot(_.isEmpty))
      }
    }

    "convert other properties from old user" in {
      val oldUser = new OldUser("jim")
      oldUser.setEmail("jim@example.com")
      oldUser.setDepartment("Chemistry")
      oldUser.setShortDepartment("Chem")
      oldUser.setDepartmentCode("CH")
      oldUser.setWarwickId("1503939")
      val user = User(oldUser)
      user.email must be (Some("jim@example.com"))
      user.department must be (Some(Department(Some("Chem"),Some("Chemistry"),Some("CH"))))
      user.universityId must be (Some(UniversityID("1503939")))
    }

    "handle missing properties from old user" in {
      val oldUser = new OldUser("jim")
      val user = User(oldUser)
      user.email must be (None)
      user.department must be (Some(Department(None,None,None)))
      user.universityId must be (None)
    }

    "reject missing usercode" in {
      intercept[IllegalArgumentException] {
        User(new AnonymousUser)
      }
    }

  }
}
