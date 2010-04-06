@webgroups
Feature: GroupService.isUserInGroup
  In order to make authentication decisions
  As an application developer
  I want to quickly check whether a user is a member of a group
  
  Scenario: Successful check
    Given that the group "in-testgroup" exists with these members:
      | User ID |
      | cus001  |
      | cus002  |
      | cus009  |
    When I call groupService.isUserInGroup("cus001", "in-testgroup")
    Then the result should be true
    When I call groupService.isUserInGroup("cus003", "in-testgroup")
    Then the result should be false
    
  Scenario: Webgroups is down
    Given Webgroups is down
    When I call groupService.isUserInGroup("cus001", "in-testgroup")
    Then a GroupServiceException should be thrown