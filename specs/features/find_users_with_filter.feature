@userlookup
Feature: Find users by filter
  In order to provide search facilities to users
  As an application developer
  I want to be able to search for users based on attributes other than user ID
  
  Scenario: Search by surname
    Given the following users exist:
      | User ID | Surname |
      | cus001  | Jones   |
      | cus002  | Smith   |
      | cus003  | Jones   |
      | cus004  | Carbon  |
      | cus005  | Grep    |
    When I call userLookup.findUsersWithFilter({"sn"=>"Jones"})
    Then I should receive the following User objects:
      | User ID |
      | cus001  |
      | cus003  |
  
  Scenario: Search by surname with no results
    Given the following users exist:
      | User ID | Surname |
      | cus001  | Jones   |
      | cus002  | Smith   |
    When I call userLookup.findUsersWithFilter({"sn"=>"Crabapple"})
    Then I should receive an empty list
    
  Scenario: SSO is down
    Given the following users exist:
      | User ID | Surname |
      | cus001  | Jones   |
      | cus002  | Smith   |
    But SSO is down
    When I call userLookup.findUsersWithFilter({"sn"=>"Jones"})
    Then I should receive an empty list