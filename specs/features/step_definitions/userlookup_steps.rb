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
   sentry.willReturnUsersIfFound [valid_user id].to_java(User) # Java User[] array needed for varargs
end

Given /^something (?:terrible|awful|dreadful) happens$/ do
  # panic! this is syntactic sugar really.
end

Given /^SSO is down$/ do
   sentry.willReturnErrors
end

When /^(?:when )?I call userLookup.getUserByUserId\("([^\"]*)"\)$/ do |id|
  with_sso_running do
    @result = @userlookup.get_user_by_user_id id
  end
end

Then /^I should receive (an? \w*User object)$/ do |user_class|
  # Must be an exact match
  @result.class.should == user_class
end

StringArray = /(\[(?:"[^\"]*",\s*)*"[^\"]*"\])/
ListOfIds = /(IDs (?:"[^\"]*", ?)*"[^\"]*")/

Given /^there are users with #{ListOfIds}$/ do |user_ids|
  puts "Will return #{user_ids}"
  sentry.will_return_users_if_found user_ids.map { |id| valid_user id }.to_java(User)
end

Given /^the following users exist:$/ do |table|
  sentry.search_results = table.hashes.map do |hash|
    user = User.new
    user.user_id = hash["User ID"]
    user.last_name = hash["Surname"] if hash["Surname"]
    user
  end
end

When /^I search for users with an "([^\"]*)" of "([^\"]*)"$/ do |attribute, value|
  with_sso_running do
    @result = @userlookup.find_users_with_filter attribute => value
  end
end

Then /^I should receive the following User objects:$/ do |table|
  @result.size.should == table.hashes.size
  
end

When /^I call userLookup.getUsersByUserIds\(#{StringArray}\)$/ do |user_ids|
  with_sso_running do
    @result = @userlookup.get_users_by_user_ids user_ids
  end
end

Then /^I should receive (\d) User objects$/ do |count|
  @result.size.should == count.to_i
end

Then /^the key "([^\"]*)" should be (an? \w*User object)$/ do |user_id, user_type|
  @result[user_id].class.should == user_type
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

Transform /\[((?:"[^\"]*",\s*)*"[^\"]*")\]/ do |id_list|
  x = parse_csv id_list
  p x
  x
end

Transform /IDs ((?:"[^\"]*",\s*)*"[^\"]*")/ do |id_list|
  parse_csv id_list
end