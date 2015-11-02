package warwick.sso

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.userlookup.{AnonymousUser, UserLookupInterface}
import java.util.Arrays
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.collection.JavaConverters._

class UserLookupServiceImplSpec extends PlaySpec with MockitoSugar {

  "UserLookupService" should {

    val userlookup = mock[UserLookupInterface](withSettings().defaultAnswer(RETURNS_SMART_NULLS))
    val service = new UserLookupServiceImpl(userlookup)
    val usercodes = List(Usercode("heron"), Usercode("cuslaj"))
    val universityIds = List(UniversityID("heron"), UniversityID("1170836"))
    val heronUser = new uk.ac.warwick.userlookup.User("heron")
    heronUser.setWarwickId("heron")
    val ritchieUser = new uk.ac.warwick.userlookup.User("cuslaj")
    ritchieUser.setWarwickId("1170836")

    val anon = new AnonymousUser

    "searchUsers" in {
      val filters = Map("species" -> "foulBird")
      when (userlookup.findUsersWithFilter(filters.asJava, false)) thenReturn List(heronUser, anon).asJava

      service.searchUsers(filters, false).get must be(Seq(User(heronUser)))
    }

    "getUsers by usercode" in {
      when(userlookup.getUsersByUserIds(Arrays.asList("heron", "cuslaj"))) thenReturn {
        Map("heron" -> heronUser, "cuslaj" -> ritchieUser, "bob" -> anon).asJava
      }
      service.getUsers(usercodes).get must be(Map(Usercode("heron") -> User(heronUser), Usercode("cuslaj") -> User(ritchieUser)))
    }

    "getUsers by university ID" in {
      val ids = UniversityID("gleb") :: universityIds
      when(userlookup.getUserByWarwickUniId("heron", false)) thenReturn heronUser
      when(userlookup.getUserByWarwickUniId("1170836", false)) thenReturn ritchieUser
      when(userlookup.getUserByWarwickUniId("gleb", false)) thenReturn anon
      service.getUsers(ids).get must be(Map(
        UniversityID("heron") -> User(heronUser).copy(universityId = Some(UniversityID("heron"))),
        UniversityID("1170836") -> User(ritchieUser).copy(universityId = Some(UniversityID("1170836")))
      ))
    }

    //FIXME spec for unverified users returned from userlookup - should be a Try failure

  }
}
