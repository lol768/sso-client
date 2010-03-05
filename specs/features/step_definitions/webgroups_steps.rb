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

When /^I call groupService\.getGroupByName\("([^\"]*)"\)$/ do |name|
  using_webgroups do
    begin
      @result = group_service.get_group_by_name name
    rescue GroupNotFoundException => e
      @thrown_exception = e
    end
  end
end

When /^I call groupService\.isUserInGroup\("([^\"]*)", "([^\"]*)"\)$/ do |user_id, group_name|
  using_webgroups do
    @result = group_service.is_user_in_group user_id, group_name
  end
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