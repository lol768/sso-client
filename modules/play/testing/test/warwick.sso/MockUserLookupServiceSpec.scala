package warwick.sso

import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}

import scala.util.{Success, Try}

class MockUserLookupServiceSpec extends PlaySpec {

  val config = Configuration.load(Environment.simple(), Map("config.resource" -> "mockuserlookupservice.conf"))

  val service = new MockUserLookupService(config)

  private def unwrapOpt[A](a: Try[Option[A]]): A =
    unwrap(a).getOrElse(fail("Expected a result but got " + a))

  private def unwrap[A](a: Try[A]): A =
    a.getOrElse(fail("Try was a Failure: " + a))

  private val matUsercode = Usercode("cuscav")
  private val matUniversityID = UniversityID("0672089")
  private val student = Usercode("u1234567")
  private val extUser = Usercode("mat.mannion@gmail.com")

  "MockUserLookupService" should {
    "return a user by usercode" in {
      unwrap(service.getUser(matUsercode)).universityId.get mustBe matUniversityID
    }

    "return a user by university ID" in {
      unwrap(service.getUsers(Seq(matUniversityID))).values.map(_.usercode) mustBe Seq(matUsercode)
    }

    "not fail when getting multiple missing usercodes/university IDs" in {
      unwrap(service.getUsers(Seq(Usercode("fiddlesticks")))) mustBe Map.empty
      unwrap(service.getUsers(Seq(UniversityID("fiddlesticks")))) mustBe Map.empty
    }

    "search for users" in {
      unwrap(service.searchUsers(Map("givenName" -> "mat*"))).map(_.usercode) mustBe Seq(matUsercode)
      unwrap(service.searchUsers(Map("cn" -> "cus"))).map(_.usercode) mustBe Nil
      unwrap(service.searchUsers(Map("cn" -> "CUSCAV"))).map(_.usercode) mustBe Seq(matUsercode)
      unwrap(service.searchUsers(Map("sn" -> "Mannion"))).map(_.usercode) mustBe Seq(matUsercode)
      unwrap(service.searchUsers(Map("warwickUniId" -> "0672089"))).map(_.usercode) mustBe Seq(matUsercode)
      unwrap(service.searchUsers(Map("cn" -> "mat*"))).map(_.usercode) mustBe Nil
      unwrap(service.searchUsers(Map("givenName" -> "student*", "sn" -> "student*"))).map(_.usercode) mustBe Seq(student)
      unwrap(service.searchUsers(Map("cn" -> "mat*"), includeDisabled = true)).map(_.usercode) mustBe Seq(extUser)
    }
  }

}