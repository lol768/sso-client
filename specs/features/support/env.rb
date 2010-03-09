require 'csv'

# Implement GroupService so you can then implement your method of choice, eg
#   mock = MockGroupService.new do
#     def getGroupByName(name)
#       raise "Surprise!"
#     end
#   end
class MockGroupService
  include Java::Uk.ac.warwick.userlookup.GroupService
end

# Can define our own methods in here that will be available
# in all step definitions. Our own personalised World.
module WarwickHelper

  # make the uk.* package available more easily
  def uk
    Java::Uk
  end
  
  def load_userlookup_classes
    java_import uk.ac.warwick.userlookup.User
    java_import uk.ac.warwick.userlookup.AnonymousUser
    java_import uk.ac.warwick.userlookup.UnverifiedUser
    java_import uk.ac.warwick.userlookup.UserLookup
    java_import uk.ac.warwick.userlookup.TestSentryServer
    java_import uk.ac.warwick.userlookup.GroupImpl
    java_import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException
  end
  
  def sentry
    @sentry ||= TestSentryServer.new
  end
  
  def userlookup
    if @userlookup.nil?
      @userlookup = UserLookup.new
      @userlookup.getUserByUserIdCache.clear
      @userlookup.getUserByTokenCache.clear
      @userlookup.setSsosUrl sentry.getPath
    end
    @userlookup
  end
  
  def group_service=(backend)
    @group_service_backend = backend
  end
  
  def group_service
    if @group_service.nil?
      userlookup.group_service_backend = @group_service_backend
      @group_service = userlookup.group_service
    end
    @group_service
  end
  
  # convert e.g. "foundUser" to "isFoundUser"
  def gettername(s)
    "is#{s.slice(0..0).upcase}#{s.slice(1..-1)}"
  end
  
  def with_sso_running
    # run requires a Runnable, but JRuby will accept a block and make a Runnable proxy. Awesome!
    sentry.run do
      yield
    end
  end
  
  def valid_user user_id
    user = User.new
    user.setUserId user_id
    user.setFoundUser true
    return user
  end
  
  def parse_csv csv
    csv.gsub(/["\s]/,"").split(",")
    #CSV::parse_line csv
  end
  

  
end

World(WarwickHelper)

Before do
  load_userlookup_classes
  # Ensure any in-memory caches are empty before starting a test.
  Java::Uk.ac.warwick.userlookup.cache.HashMapCacheStore.clearAll
end