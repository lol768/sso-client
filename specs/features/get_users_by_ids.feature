Feature: Get multiple users by IDs
  In order to efficiently show the names of many users
  As an application developer
  I want to fetch details for many users at once
  
  Scenario: Searching where all users exist
    Given there are users with IDs "cus001", "cus002", "cus003"
    When I search for IDs "cus001", "cus002", "cus003"
    Then I should receive 3 User objects
      And 3 of them should be valid users
    
  Scenario: Searching where only some of the users exist
    Given there are users with IDs "cus001", "cus003"
    When I search for IDs "cus987", "cus001", "cus003"
    Then I should receive 3 User objects
      And the key "cus001" should be a User object
      And the key "cus003" should be a User object
      But the key "cus987" should be an AnonymousUser object
      
  Scenario: SSO is down
    Given SSO is down
    When I search for IDs "cus001", "cus002"
    Then I should receive 2 User objects
      But the key "cus001" should be an UnverifiedUser object
      And the key "cus002" should be an UnverifiedUser object