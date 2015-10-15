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
   * @return A Map from usercode to user. If a code did not match a user,
   */
  def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]]
  def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false): Try[Map[UniversityID, User]]
  def searchUsers(filters: Map[String,String], includeDisabled: Boolean = false): Try[Seq[User]]

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

  override def searchUsers(filters: Map[String, String], includeDisabled: Boolean = false): Try[Seq[User]] = Try {
    userLookupInterface.findUsersWithFilter(filters.asJava, includeDisabled)
      .asScala
      .filter(hasUsercode)
      .map(User.apply)
  }

  override def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false): Try[Map[UniversityID, User]] = Try {
    ids.flatMap { id =>
      // TODO Websignon supports batch lookup by university ID - implement SSO-1472 to avoid making N requests

      // Maybe a user with a usercode
      val u = Option(userLookupInterface.getUserByWarwickUniId(id.string, includeDisabled))
        .filter(hasUsercode).map(User.apply)

      for {
        user <- u
        id <- user.universityId
      } yield (id -> user)
    }.toMap
  }

}
