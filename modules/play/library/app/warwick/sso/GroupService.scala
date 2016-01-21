package warwick.sso

import com.google.inject.{ImplementedBy, Inject}
import uk.ac.warwick.userlookup
import uk.ac.warwick.userlookup.webgroups.{GroupNotFoundException, GroupServiceException}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[GroupServiceImpl])
trait GroupService {

  def getWebGroup(groupName: GroupName): Try[Option[Group]]

  def isUserInGroup(usercode: Usercode, groupName: GroupName): Try[Boolean]

  def getGroupsForUser(usercode: Usercode): Try[Seq[Group]]

  def getGroupsInDepartment(department: Department): Try[Seq[Group]]

}

class GroupServiceImpl @Inject()(
  groupService: userlookup.GroupService
) extends GroupService {

  override def getWebGroup(groupName: GroupName) =
    try {
      val group = Group(groupService.getGroupByName(groupName.string))

      Success(Some(group))
    } catch {
      case _: GroupNotFoundException => Success(None)
      case e: GroupServiceException => Failure(e)
    }

  override def isUserInGroup(usercode: Usercode, groupName: GroupName) =
    Try(groupService.isUserInGroup(usercode.string, groupName.string))

  override def getGroupsForUser(usercode: Usercode) =
    Try(groupService.getGroupsForUser(usercode.string).map(Group.apply))

  override def getGroupsInDepartment(department: Department) =
    Try(groupService.getGroupsForDeptCode(department.code.getOrElse(throw new IllegalArgumentException("Department code is empty"))).map(Group.apply))

}
