package warwick.sso

import javax.inject.Inject

import com.google.inject.ImplementedBy
import uk.ac.warwick.userlookup.{UnverifiedUser, UserLookupInterface}
import uk.ac.warwick.userlookup
import scala.collection.JavaConverters._
import scala.util.{Success, Try}

import User.hasUsercode

@ImplementedBy(classOf[UserLookupServiceImpl])
trait UserLookupService {

  /**
   * @return A Try[Map] from usercode to user. If a code did not match a user,
   *      we don't return it. Unverified users (where there was an I/O error) will be
    *     a failed Try.
   */
  def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]]
  /**
    * @return A Try[Map] from Uni ID to user. If an ID did not match a user,
    *     we don't return it. Unverified users (where there was an I/O error) will be
    *     a failed Try.
    */
  def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false): Try[Map[UniversityID, User]]
  def searchUsers(filters: Map[String,String], includeDisabled: Boolean = false): Try[Seq[User]]

  /**
   * Makes a request to Websignon with the user's username and password.
   * This is for API access where an external user might be making an authorised
   * request without cookies.
   */
  def basicAuth(usercode: String, password: String): Try[Option[User]]

  def getUser(usercode: Usercode): Try[User] =
    getUsers(Seq(usercode)).map(users => users(usercode))

}

class UserLookupServiceImpl @Inject() (userLookupInterface: UserLookupInterface) extends UserLookupService {
  type OldUser = userlookup.User

  private def existsWithUsercode(user: OldUser) = user.isFoundUser && hasUsercode(user)

  override def getUsers(codes: Seq[Usercode]): Try[Map[Usercode, User]] = Try {
    userLookupInterface.getUsersByUserIds(codes.map(_.string).asJava)
      .asScala
      .toMap
      .flatMap {
        case (id, user) if existsWithUsercode(user) => Some(Usercode(id), User(user))
        case (id, user) if !user.isVerified => throw user.asInstanceOf[UnverifiedUser].getVerificationException
        case _ => None
      }
  }

  override def searchUsers(filters: Map[String,String], includeDisabled: Boolean = false) = Try {
    userLookupInterface.findUsersWithFilter(filters.mapValues(_.asInstanceOf[Object]).asJava, includeDisabled)
      .asScala
      .filter(existsWithUsercode)
      .map(User.apply)
  }

  override def getUsers(ids: Seq[UniversityID], includeDisabled: Boolean = false) = Try {
    userLookupInterface.getUsersByWarwickUniIds(ids.map(_.string).asJava, includeDisabled)
      .asScala
      .toMap
      .flatMap {
        case (idString, user) if existsWithUsercode(user) => Some(UniversityID(idString) -> User.apply(user))
        case (_, user) if !user.isVerified => throw user.asInstanceOf[UnverifiedUser].getVerificationException
        case _ => None
      }
  }

  override def basicAuth(usercode: String, password: String) = Try {
    Option(userLookupInterface.getUserByIdAndPassNonLoggingIn(usercode, password))
      .filter(existsWithUsercode)
      .map(User.apply)
  }

}
