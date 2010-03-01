Warwick = Java::uk.ac.warwick
User = Warwick.userlookup.User
UserLookup = Warwick.userlookup.UserLookup
TestSentryServer = Warwick.userlookup.TestSentryServer

Before do
  @userlookup = UserLookup.new
  @userlookup.getUserByUserIdCache.clear
  @userlookup.getUserByTokenCache.clear
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
   @sentry.willReturnUsersIfFound [user].to_java(User) # Java User[] array needed for varargs
end

Given /^something (?:terrible|awful|dreadful) happens$/ do
  # panic!
end

Given /^SSO is down$/ do
   @sentry.willReturnErrors
end

When /^(?:when )?I search for ID "([^\"]*)"$/ do |id|
  with_sso_running do
    @result = @userlookup.getUserByUserId id
  end
end

Then /^I should receive (an? \w*User) object$/ do |kind|
  @result.class.should == case kind
    when "an AnonymousUser"
      Warwick.userlookup.AnonymousUser
    when "an UnverifiedUser"
      Warwick.userlookup.UnverifiedUser
    when "a User"
      User
    else
      raise ArgumentError, "Unrecognised user kind"
    end
end

Then /^the property (.+) should return (true|false)$/ do |property,bool|
  @result.send("#{gettername property}").to_s.should == bool
end

def with_sso_running
  # run requires a Runnable, but JRuby will accept a block and make a Runnable proxy. Awesome!
  @sentry.run do
    yield
  end
end