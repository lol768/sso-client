
Warwick = Java::uk.ac.warwick
User = Warwick.userlookup.User
UserLookup = Warwick.userlookup.UserLookup

Before do
  @userlookup = UserLookup.new
  @userlookup.getUserByUserIdCache.clear
  @userlookup.getUserByTokenCache.clear
  @userlookup.setSsosUrl sentry.getPath
end

Given /^there (?:is|exists) no user with ID "([^\"]*)"$/ do |id|
   # test sentry will default to not finding a user, so do nothing
end

Given /^there (?:is|exists) a user with ID "([^\"]*)"$/ do |id|
   user = User.new
   user.setUserId id
   user.setFoundUser true
   sentry.willReturnUsersIfFound [user].to_java(User) # Java User[] array needed for varargs
end

Given /^something (?:terrible|awful|dreadful) happens$/ do
  # panic!
end

Given /^SSO is down$/ do
   sentry.willReturnErrors
end

When /^(?:when )?I search for ID "([^\"]*)"$/ do |id|
  with_sso_running do
    @result = @userlookup.getUserByUserId id
  end
end

Then /^I should receive (an? \w*User object)$/ do |user_class|
  # Must be an exact match
  @result.class.should == user_class
end

ListOfIds = /(IDs (?:"[^\"]*", ?)*"[^\"]*")/

Given /^there are users with #{ListOfIds}$/ do |user_ids|
  puts "Will return #{user_ids}"
  sentry.willReturnUsersIfFound user_ids.map { |id| 
    user = User.new
    user.setUserId id
    user.setFoundUser true
    user
  }.to_java(User)
end

When /^I search for #{ListOfIds}$/ do |user_ids|
  with_sso_running do
    @result = @userlookup.getUsersByUserIds user_ids
  end
end

Then /^I should receive (\d) User objects$/ do |count|
  @result.size.should == count.to_i
end

Then /^the key "([^\"]*)" should be (an? \w*User object)$/ do |user_id, user_type|
  @result[user_id].class.should == user_type
end

And /^(?:only )?(\d) of them should be valid users$/ do |count|
  @result.values.select {|user|
    user.isFoundUser and user.isVerified
  }.size.should == count.to_i
end

And /^all of them should be valid users$/ do
  step "#{@result.size} of them should be valid users"
end

Then /^the property (.+) should return (true|false)$/ do |property,bool|
  @result.send("#{gettername property}").to_s.should == bool
end


# Transforms - converts a matched string into any other object

Transform /an? ((?:Anonymous|Unverified)?User) object/ do |kind|
  case kind
    when "AnonymousUser"
      Warwick.userlookup.AnonymousUser
    when "UnverifiedUser"
      Warwick.userlookup.UnverifiedUser
    when "User"
      User
    else
      raise ArgumentError, "Unrecognised user kind"
  end
end

Transform /IDs ((?:"[^\"]*", ?)*"[^\"]*")/ do |id_list|
  id_list.gsub(/[" ]/,'').split(',')
end