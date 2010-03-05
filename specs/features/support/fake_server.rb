require 'webrick'
include WEBrick

module FakeServerHelper
  # options will get passed to server mount, and on to
  # the servlet's initialize method
  def use_server(server_class, *options)
    @fake_server = server_class
    @options = options
  end
  
  # All calls to webgroups should be within here, so that if we've
  # defined a fake server it will be running within the given block
  def using_webgroups(&block)
    if @fake_server 
      FakeServer.run @fake_server, @options do
        block.call
      end
    else
      yield
    end
  end  
  
  def server_path
    "http://localhost:#{FakeServer::Port}"
  end
end
World(FakeServerHelper)

Before do
  
end

class FakeServer < WEBrick::HTTPServlet::AbstractServlet

  Port = 26080
  
  def self.run(mount, *options)
    s = HTTPServer.new( :Port => Port )
    s.mount("/", mount, options)
    trap("INT"){ s.shutdown }
    begin
      Thread.new do
        s.start
      end
      yield
    ensure
      s.shutdown if s
    end
  end  
end

# For returning the same content
class StaticServer < FakeServer
  def initialize(server, content)
    super(server)
    @content = content
  end
  
  def do_GET(req,res)
    res.body = @content
  end
  alias do_POST do_GET
end

# Handy server that will return 500 errors
class DeadServer < FakeServer
  def do_POST(req, res)
    raise WEBrick::HTTPStatus::InternalServerError
  end
  alias do_GET do_POST
end
