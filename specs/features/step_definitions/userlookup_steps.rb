Warwick = Java::uk.ac.warwick
User = Warwick.userlookup.User
UserLookup = Warwick.userlookup.UserLookup
TestSentryServer = Warwick.userlookup.TestSentryServer

Before do
  @userlookup = UserLookup.new
  @sentry = TestSentryServer.new
  @userlookup.setSsosUrl @sentry.getPath
end

Given /^there (?:is|exists) no user with ID "([^\"]*)"$/ do |id|
   # test sentry will default to not finding a user.
end

Given /^there (?:is|exists) a user with ID "([^\"]*)"$/ do |id|
   user = User.new
   user.setUserId id
   user.setFoundUser true
   @sentry.willReturnUsers [user].to_java(User) # Java User[] array needed for varargs
end

Given /^SSO is down$/ do
   @sentry.willReturnErrors
end

When /^I search for ID "([^\"]*)"$/ do |id|
  with_sso_running do
    @result = @userlookup.getUserByUserId id
  end
end

Then /^I should receive an AnonymousUser object$/ do
  @result.class.should == Warwick.userlookup.AnonymousUser
end

Then /^the property (.+) should return (true|false)$/ do |property,bool|
  @result.send("#{gettername property}").to_s.should == bool
end

Then /^I should receive a User object$/ do
  @result.class.should == User
end

def gettername(s)
  "is#{s.slice(0..0).upcase}#{s.slice(1..-1)}"
end

def with_sso_running
  # run requires a Runnable, but JRuby will accept a block and make a Runnable proxy. Awesome!
  @sentry.run do
    yield
  end
end