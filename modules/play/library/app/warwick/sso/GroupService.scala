package warwick.sso

import com.google.inject.{ImplementedBy, Inject}
import uk.ac.warwick.userlookup
import uk.ac.warwick.userlookup.webgroups.{GroupNotFoundException, GroupServiceException, WarwickGroupsService}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[GroupServiceImpl])
trait GroupService {

  def getWebGroup(groupName: GroupName): Try[Option[Group]]

  def isUserInGroup(usercode: Usercode, groupName: GroupName): Try[Boolean]

  def getGroupsForUser(usercode: Usercode): Try[Seq[Group]]

  def getGroupsInDepartment(department: Department): Try[Seq[Group]]

  def getGroupsForQuery(query: String): Try[Seq[Group]]
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
    Try(groupService.getGroupsForUser(usercode.string).asScala.map(Group.apply))

  override def getGroupsInDepartment(department: Department) =
    Try(groupService.getGroupsForDeptCode(department.code.getOrElse(throw new IllegalArgumentException("Department code is empty"))).asScala.map(Group.apply))

  override def getGroupsForQuery(query: String) =
    Try(groupService.getGroupsForQuery(query).asScala.map(Group.apply))
}

class UncachedGroupServiceImpl @Inject()(
  groupService: WarwickGroupsService
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
    Try(groupService.getGroupsForUser(usercode.string).asScala.map(Group.apply))

  override def getGroupsInDepartment(department: Department) =
    Try(groupService.getGroupsForDeptCode(department.code.getOrElse(throw new IllegalArgumentException("Department code is empty"))).asScala.map(Group.apply))

  override def getGroupsForQuery(query: String) =
    Try(groupService.getGroupsForQuery(query).asScala.map(Group.apply))
}
