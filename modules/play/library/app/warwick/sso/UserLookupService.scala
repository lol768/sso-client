package warwick.sso

import javax.inject.Inject

import com.google.inject.ImplementedBy
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.userlookup
import uk.ac.warwick.util.core.StringUtils
import scala.collection.JavaConverters._
import scala.util.Try

import User.hasUsercode

@ImplementedBy(classOf[UserLookupServiceImpl])
trait UserLookupService {

  /**
   * @return A Try[Map] from usercode to user. If a code did not match a user,
   * we don't return it.
   */
  def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]]
  def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false): Try[Map[UniversityID, User]]
  def searchUsers(filters: Map[String,String], includeDisabled: Boolean = false): Try[Seq[User]]

  /**
   * Makes a request to Websignon with the user's username and password.
   * This is for API access where an external user might be making an authorised
   * request without cookies.
   */
  def basicAuth(usercode: String, password: String): Try[Option[User]]
}

class UserLookupServiceImpl @Inject() (userLookupInterface: UserLookupInterface) extends UserLookupService {
  type OldUser = userlookup.User

  override def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]] = Try {
    userLookupInterface.getUsersByUserIds(codes.map(_.string).asJava)
      .asScala
      .toMap
      .flatMap {
      case (id, user: OldUser) if hasUsercode(user) => Some(Usercode(id), User(user))
      case _ => None
    }
  }

  override def searchUsers(filters: Map[String, String], includeDisabled: Boolean = false) = Try {
    userLookupInterface.findUsersWithFilter(filters.asJava, includeDisabled)
      .asScala
      .filter(hasUsercode)
      .map(User.apply)
  }

  override def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false) = Try {
    // TODO Websignon supports batch lookup by university ID - implement SSO-1472 to avoid making N requests
    ids.flatMap { id =>
      Option(userLookupInterface.getUserByWarwickUniId(id.string, includeDisabled)).filter(hasUsercode)
    }
    .map(User.apply)
    .flatMap { user =>
      user.universityId.map { id => (id -> user) }
    }
    .toMap
  }

  override def basicAuth(usercode: String, password: String) = Try {
    Option(userLookupInterface.getUserByIdAndPassNonLoggingIn(usercode, password))
      .filter(hasUsercode)
      .map(User.apply)
  }

}
