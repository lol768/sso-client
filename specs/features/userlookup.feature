Feature: Get user by ID
  In order to show the names of other users,
  I want to be able to fetch their details from SSO
  
      
  Scenario: Search for existing user
    Given there is a user with ID "cusxyz"
    When I search for ID "cusxyz"
    Then I should receive a User object
      And the property foundUser should return true
      And the property verified should return true
    