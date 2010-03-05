require "spec/mocks"

# Sets up rspec mock() support, including verifying the
# checks at the end and resetting the system.
# I think at the moment, mismatched mock expectations
# won't cause a Scenario to fail. May need to check
# the result of verify_all.

Before do
  $rspec_mocks = Spec::Mocks::Space.new
end

After do
  begin
    $rspec_mocks.verify_all
  ensure
    $rspec_mocks.reset_all
  end
end