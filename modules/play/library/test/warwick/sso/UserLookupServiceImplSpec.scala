package warwick.sso

import java.io.IOException

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.userlookup.{UnverifiedUser, AnonymousUser, UserLookupInterface}
import java.util.Arrays
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class UserLookupServiceImplSpec extends PlaySpec with MockitoSugar {

  type CoreUser = uk.ac.warwick.userlookup.User

  class Context {
    val userlookup = mock[UserLookupInterface](withSettings().defaultAnswer(RETURNS_SMART_NULLS))
    val service = new UserLookupServiceImpl(userlookup)
    val usercodes = List(Usercode("heron"), Usercode("cuslaj"))
    val universityIds = List(UniversityID("heron"), UniversityID("1170836"))
    val heronUser = new CoreUser("heron")
    heronUser.setFoundUser(true)
    heronUser.setWarwickId("heron")
    val ritchieUser = new CoreUser("cuslaj")
    ritchieUser.setFoundUser(true)
    ritchieUser.setWarwickId("1170836")

    val anon = new AnonymousUser
    anon.setUserId("nonexist") // anon user can have an ID set

    val error = new IOException("The error that caused an unverified user to be returned")
    val unverified = new UnverifiedUser(error)
  }
  
  "UserLookupService" should {

    "searchUsers" in new Context {
      val filters = Map("species" -> "foulBird")
      when (userlookup.findUsersWithFilter(filters.asJava, false)) thenReturn List(heronUser, anon).asJava

      service.searchUsers(filters, false).get must be(Seq(User(heronUser)))
    }

    "getUsers by usercode" in new Context {
      when(userlookup.getUsersByUserIds(Arrays.asList("heron", "cuslaj"))) thenReturn {
        Map("heron" -> heronUser, "cuslaj" -> ritchieUser, "bob" -> anon).asJava
      }
      service.getUsers(usercodes).get must be(Map(Usercode("heron") -> User(heronUser), Usercode("cuslaj") -> User(ritchieUser)))
    }

    "getUsers I/O error" in new Context {
      when (userlookup.getUsersByUserIds(Arrays.asList("ichabod"))) thenReturn {
        Map[String, CoreUser]("ichabod" -> unverified).asJava
      }

      service.getUsers(List(Usercode("ichabod"))) must be ('failure)
    }

    "getUsers by university ID" in new Context {
      val ids = UniversityID("gleb") :: universityIds
      when(userlookup.getUserByWarwickUniId("heron", false)) thenReturn heronUser
      when(userlookup.getUserByWarwickUniId("1170836", false)) thenReturn ritchieUser
      when(userlookup.getUserByWarwickUniId("gleb", false)) thenReturn anon
      service.getUsers(ids).get must be(Map(
        UniversityID("heron") -> User(heronUser).copy(universityId = Some(UniversityID("heron"))),
        UniversityID("1170836") -> User(ritchieUser).copy(universityId = Some(UniversityID("1170836")))
      ))
    }

    "basicAuth success" in new Context {
      when(userlookup.getUserByIdAndPassNonLoggingIn("api-tabula","12345")) thenReturn heronUser
      service.basicAuth("api-tabula", "12345").get.get.usercode must be (Usercode("heron"))
    }

    "basicAuth failure" in new Context {
      val anError = new RuntimeException("Argh!")
      when(userlookup.getUserByIdAndPassNonLoggingIn("api-tabula","12345")) thenThrow anError
      service.basicAuth("api-tabula", "12345") must be (Failure(anError))
    }

    "basicAuth no user" in new Context {
      when(userlookup.getUserByIdAndPassNonLoggingIn("api-tabula","12345")) thenReturn anon
      service.basicAuth("api-tabula", "12345") must be (Success(None))
    }

    //FIXME spec for unverified users returned from userlookup - should be a Try failure

  }
}
