@webgroups
Feature: Get groups by query
  
  Scenario: Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsForQuery("a")
    Then a GroupServiceException should be thrown