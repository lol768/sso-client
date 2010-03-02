
# Can define our own methods in here that will be available
# in all step definitions. Our own personalised World.
module WarwickHelper
  
  Warwick = Java::uk.ac.warwick
  TestSentryServer = Warwick.userlookup.TestSentryServer
  
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
  
end

World(WarwickHelper)

Before do
  #puts "Using WarwickWorld"
end