@userlookup
Feature: Cache behaviour
  In order to improve stability and performance
  As an application developer
  I would like to cache user details in a predictable way
  
  Scenario: Get User by ID returns stale data and updates in the background
    Given there is a user with ID "cusxyz"
    When I call userLookup.setUserIdCacheTimeout(2)
    And I call userLookup.getUserByUserId("cusxyz")
    Then I should receive a User object
    
    Given there is no longer a user with ID "cusxyz"
    When I wait for 2 seconds
    And I call userLookup.getUserByUserId("cusxyz")
    Then I should receive a User object
    
    When I wait for 1 second
    And I call userLookup.getUserByUserId("cusxyz")
    Then I should receive an AnonymousUser object