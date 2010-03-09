require 'spec/stubs/cucumber'

Given /^that the group "([^\"]*)" exists with these members:$/ do |group_name, table|
  @group_members_table = table # for later
  @group_member_ids = table.hashes.map {|hash| hash["User ID"] }
     
  group = GroupImpl.new
  group.name = group_name
  group.user_codes = @group_member_ids
  
  service = mock(:GroupService)
  service.stub(:getGroupByName).with(group_name).and_return(group)
  service.stub(:isUserInGroup) do |name, g|
    g.should == group_name
    group.user_codes.include? name
  end
  self.group_service = service
end

Given /^that the group "([^\"]*)" exists$/ do |group_name|
  group = GroupImpl.new
  group.name = group_name
  
  service = mock(:GroupService)
  service.should_receive(:getGroupByName).with(group_name).and_return(group)
  self.group_service = service
end

Given /^that the group "([^\"]*)" doesn't exist$/ do |group_name|
  use_server StaticServer, %{<groups></groups>}
  userlookup.group_service_location = server_path
end


Given /^Webgroups is down$/ do
  use_server DeadServer
  userlookup.group_service_location = server_path
end

When i_call_a_method_on :groupService do |invocation|
  method_name, args = invocation
  using_webgroups do
    begin
      @result = group_service.send method_name, *args.compact
    rescue GroupNotFoundException => e
      @thrown_exception = e
    end
  end
end

Then /^I should receive the empty GroupService\.PROBLEM_FINDING_GROUPS list$/ do
  @result.should == uk.ac.warwick.userlookup.GroupService.PROBLEM_FINDING_GROUPS
  @result.should be_empty
end

Then /^the result should be (true|false)$/ do |bool|
  @result.to_s.should == bool
end

Then /^I should receive a Group object containing those members$/ do
  @result.user_codes.should == @group_member_ids 
end

Then /^I should receive an empty Group object$/ do
  @result.user_codes.should be_empty
end

Then /^a GroupNotFoundException should be thrown$/ do
  @thrown_exception.cause.class.should == GroupNotFoundException
end