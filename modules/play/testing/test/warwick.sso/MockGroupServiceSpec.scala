package warwick.sso

import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}

import scala.util.{Success, Try}

class MockGroupServiceSpec extends PlaySpec {

  val config = Configuration.load(Environment.simple(), Map("config.resource" -> "mockgroupservice.conf"))

  val service = new MockGroupService(config)

  private def unwrap[A](a: Try[Option[A]]): A =
    a.getOrElse(fail("Try was a Failure: " + a))
      .getOrElse(fail("Expected a result but got " + a))

  private val nick = Usercode("cusebr")
  private val elab = GroupName("in-elab")
  private val moodle = GroupName("in-moodle")
  private val emptyGroup = GroupName("in-empty")

  "MockGroupService" should {
    "return a group" in {
      unwrap(service.getWebGroup(elab)).title.get mustBe ("E-lab")
    }

    "return None for missing group" in {
      service.getWebGroup(GroupName("fiddlesticks")) mustBe Success(None)
    }

    "check if user is in group" in {
      service.isUserInGroup(nick, elab) mustBe Success(true)
      service.isUserInGroup(nick, emptyGroup) mustBe Success(false)
    }

    "get a user's groups" in {
      service.getGroupsForUser(nick).get.map(_.name) mustBe Seq(elab, moodle)
    }
  }

}