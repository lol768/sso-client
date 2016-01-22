package warwick.sso

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.userlookup
import uk.ac.warwick.userlookup.GroupImpl
import uk.ac.warwick.userlookup.webgroups.GroupServiceException

import scala.collection.JavaConversions._
import scala.util.Success

class GroupServiceSpec extends PlaySpec with MockitoSugar {

  "GroupService" should {
    val underlyingGroupService = mock[userlookup.GroupService]
    val groupService = new GroupServiceImpl(underlyingGroupService)

    val elabGroup = new GroupImpl()
    elabGroup.setName("in-elab")
    elabGroup.setTitle("ITS Web Team")
    elabGroup.setUserCodes(Seq("alice", "bob"))
    elabGroup.setOwners(Seq("eve"))
    elabGroup.setDepartment("IT Services")
    elabGroup.setDepartmentCode("IN")
    elabGroup.setLastUpdatedDate(new DateTime(2016, 1, 1, 9, 30).toDate)
    elabGroup.setVerified(true)

    val otherGroup = new GroupImpl()
    otherGroup.setName("in-other")
    otherGroup.setVerified(true)

    val ITServices = Department(
      shortName = None,
      name = Some("IT Services"),
      code = Some("IN")
    )

    "getWebGroup" in {
      when(underlyingGroupService.getGroupByName("in-elab")).thenReturn(elabGroup)

      val group = groupService.getWebGroup(GroupName("in-elab")).get.get

      group.name mustBe GroupName("in-elab")
      group.title mustBe Some("ITS Web Team")
      group.members mustBe Seq(Usercode("alice"), Usercode("bob"))
      group.owners mustBe Seq(Usercode("eve"))
      group.department mustBe ITServices
      group.updatedAt mustBe new DateTime(2016, 1, 1, 9, 30)

      group.contains(Usercode("alice")) mustBe true
      group.contains(Usercode("bob")) mustBe true
      group.contains(Usercode("eve")) mustBe true
      group.contains(Usercode("imposter")) mustBe false
    }

    "getGroupsForUser" in {
      when(underlyingGroupService.getGroupsForUser("alice")).thenReturn(Seq(elabGroup, otherGroup))

      val groups = groupService.getGroupsForUser(Usercode("alice")).get

      groups.map(_.name) mustBe Seq(GroupName("in-elab"), GroupName("in-other"))
    }

    "getGroupsInDepartment" in {
      groupService.getGroupsInDepartment(Department(None, None, None)).isFailure mustBe true

      when(underlyingGroupService.getGroupsForDeptCode("IN")).thenReturn(Seq(elabGroup, otherGroup))

      val groups = groupService.getGroupsInDepartment(ITServices).get

      groups.map(_.name) mustBe Seq(GroupName("in-elab"), GroupName("in-other"))
    }

    "check isUserInGroup" in {
      when(underlyingGroupService.isUserInGroup("alice", "in-elab")).thenThrow(classOf[GroupServiceException])
      groupService.isUserInGroup(Usercode("alice"), GroupName("in-elab")).isSuccess mustBe false

      reset(underlyingGroupService)
      when(underlyingGroupService.isUserInGroup("alice", "in-elab")).thenReturn(true)
      groupService.isUserInGroup(Usercode("alice"), GroupName("in-elab")) mustBe Success(true)

      reset(underlyingGroupService)
      when(underlyingGroupService.isUserInGroup("alice", "in-elab")).thenReturn(false)
      groupService.isUserInGroup(Usercode("alice"), GroupName("in-elab")) mustBe Success(false)
    }
  }

}
