@webgroups
Feature: Get groups for department code
  
  Scenario: Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsForDeptCode("IN")
    Then I should receive the empty GroupService.PROBLEM_FINDING_GROUPS list