Warwick = Java::uk.ac.warwick
User = Warwick.userlookup.User
UserLookup = Warwick.userlookup.UserLookup
TestSentryServer = Warwick.userlookup.TestSentryServer

Before do
  @userlookup = UserLookup.new
  @sentry = TestSentryServer.new
end

Given /^there (?:is|exists) no user with ID "([^\"]*)"$/ do |id|
   pending # express the regexp above with the code you wish you had
end

Given /^there (?:is|exists) a user with ID "([^\"]*)"$/ do |id|
   user = User.new
   user.setUserId id
   user.setFoundUser true
   @sentry.willReturnUsers user
end

When /^I search for ID "([^\"]*)"$/ do |id|
  pending # express the regexp above with the code you wish you had
end

Then /^I should receive an AnonymousUser object$/ do
  pending # express the regexp above with the code you wish you had
end

Then /^the property (.+) should return (true|false)$/ do |property,bool|
  @user.send("is#{property.gsub}").to_s.should == bool
end

Then /^I should receive a User object$/ do
  pending # express the regexp above with the code you wish you had
end