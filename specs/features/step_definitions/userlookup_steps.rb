
StringArray = /(\[(?:"[^\"]*",\s*)*"[^\"]*"\])/
ListOfIds = /(IDs (?:"[^\"]*", ?)*"[^\"]*")/

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

Given /^there are users with #{ListOfIds}$/ do |user_ids|
  sentry.will_return_users_if_found user_ids.map { |id| valid_user id }.to_java(User)
end

Given /^the following users exist:$/ do |table|
  sentry.search_results = table.hashes
end

# Single step for calling any userLookup method - a Transform parses the arguments as JSON
When i_call_a_method_on :userLookup do |invocation|
  method_name, args = invocation
  p method_name
  p args
  with_sso_running do
    @result = userlookup.send method_name, *args.compact
  end
end

When /^I search for anything$/ do
  with_sso_running do
    @result = userlookup.find_users_with_filter "sn" => "Jones"
  end
end


Then /^I should receive (an? \w*User object)$/ do |user_class|
  # Must be an exact match
  @result.class.should == user_class
end

Then /^I should receive the following User objects:$/ do |table|
  @result.size.should == table.hashes.size
end

Then /^I should receive an empty list$/i do
  @result.should be_empty
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
      AnonymousUser
    when "UnverifiedUser"
      UnverifiedUser
    when "User"
      User
    else
      raise ArgumentError, "Unrecognised user kind"
  end
end

## list of strings surrounded by square brackets
#Transform /(\[(?:"[^\"]*",\s*)*"[^\"]*"\])/ do |id_list|
#  JSON.parse id_list
#end

# list of strings starting "IDs " but with no brackets
Transform /IDs ((?:"[^\"]*",\s*)*"[^\"]*")/ do |id_list|
  JSON.parse "[#{id_list}]"
end