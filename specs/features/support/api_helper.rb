require 'json'

# Matches a dot, a method name, then an argument list.
# The argument list is parsed as a JSON array without the square brackets.
Transform /\.(\w+)\(([^)]*)\)/ do |method_name, argument_json|
  [method_name, JSON.parse("[#{argument_json}]")]
end

def i_call_a_method_on(bean_name)
  /^(?:when )?I call #{bean_name.to_s}(\.\w+\([^)]*\))$/
end