@webgroups
Feature: WebgroupService.getGroupByName
  In order to give users access to Webgroups information
  As an application developer
  I need to be able to query the Webgroups server
 
  Scenario: Get group by name
    Given that the group "in-testgroup" exists with these members:
      | User ID |
      | cus001  |
      | cus002  |
      | cus009  |
    When I call groupService.getGroupByName("in-testgroup")
    Then I should receive a Group object containing those members
 
  Scenario: Get group that doesn't exist
    Given that the group "in-testgroup" doesn't exist
    When I call groupService.getGroupByName("in-testgroup")
    Then a GroupNotFoundException should be thrown
    
  Scenario: Get group when Webgroups is down
    Given Webgroups is down
    When I call groupService.getGroupByName("in-testgroup")
    Then I should receive an empty Group object
    
