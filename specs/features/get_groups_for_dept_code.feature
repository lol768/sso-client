@webgroups
Feature: GroupService.getGroupsForDeptCode
  
  Scenario: Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsForDeptCode("IN")
    Then a GroupServiceException should be thrown