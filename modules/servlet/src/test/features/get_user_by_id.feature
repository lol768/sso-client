@userlookup
Feature: Get user by ID
  In order to show the names of other users
  As an application developer
  I want to be able to fetch their details from SSO
      
  Scenario: Search for existing user
    Given there is a user with ID "cusxyz"
    When I call userLookup.getUserByUserId("cusxyz")
    Then I should receive a User object
      And the property foundUser should return true
      And the property verified should return true
      
  Scenario: Search for nonexistant user
    Given there is no user with ID "xyz999"
    When I call userLookup.getUserByUserId("xys999")
    Then I should receive an AnonymousUser object
      And the property foundUser should return false
      And the property verified should return true
  
  Scenario: Search when SSO is down
    Given SSO is down
    When I call userLookup.getUserByUserId("cusxyz")
    Then I should receive an UnverifiedUser object
      And the property foundUser should return false
      And the property verified should return false
    
  Scenario: Searching for cached user when SSO is down
    Given there is a user with ID "cus123"
    When I call userLookup.getUserByUserId("cus123")
    Then I should receive a User object
    Given something terrible happens
      And SSO is down
    When I call userLookup.getUserByUserId("cus123")
    Then I should receive a User object
    But when I call userLookup.getUserByUserId("cus567")
    Then I should receive an UnverifiedUser object
