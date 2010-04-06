@webgroups
Feature: Get groups for a user
    
  Scenario: Get group when Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsForUser("cusabc")
    Then a GroupServiceException should be thrown
    
  Scenario: Get group names when Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupsNamesForUser("cusabc")
    Then a GroupServiceException should be thrown
