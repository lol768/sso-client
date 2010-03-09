@webgroups
Feature: Get groups by query
  
  Scenario: Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsForQuery("a")
    Then I should receive the empty GroupService.PROBLEM_FINDING_GROUPS list