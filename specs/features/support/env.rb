require 'csv'

# Can define our own methods in here that will be available
# in all step definitions. Our own personalised World.
module WarwickHelper
  
  Warwick = Java::uk.ac.warwick
  TestSentryServer = Warwick.userlookup.TestSentryServer
  User = Warwick.userlookup.User
  UserLookup = Warwick.userlookup.UserLookup
  
  def sentry
    @sentry ||= TestSentryServer.new
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
  #puts "Using WarwickWorld"
end