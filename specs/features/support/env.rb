
# Can define our own methods in here that will be available
# in all step definitions. Our own personalised World.
class WarwickWorld
  
  # convert e.g. "foundUser" to "isFoundUser"
  def gettername(s)
    "is#{s.slice(0..0).upcase}#{s.slice(1..-1)}"
  end
  
end

World do
  WarwickWorld.new
end

Before do
  #puts "Using WarwickWorld"
end