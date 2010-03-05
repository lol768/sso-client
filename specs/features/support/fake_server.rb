require 'webrick'
include WEBrick

module FakeServerHelper
  def use_server server_class
    @fake_server = server_class
  end
  
  # All calls to webgroups should be within here, so that if we've
  # defined a fake server it will be running within the given block
  def using_webgroups(&block)
    if @fake_server 
      puts "Going to use a fake server"
      FakeServer.run @fake_server do
        puts "Inside outer block"
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
  
  def self.run(mount)
    s = HTTPServer.new( :Port => Port )
    s.mount("/", mount)
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
  def do_GET(req,res)
    res.body = @@content
  end
  alias do_POST do_GET
  
  # Returns an anonymous servlet Class that will
  # always produce the given content. Pass the
  # result to the use_server method. 
  def self.producing(content)
    Class.new StaticServer do
      @@content = content
    end
  end
end

# Handy server that will return 500 errors
class DeadServer < FakeServer
  def do_POST(req, res)
    raise WEBrick::HTTPStatus::InternalServerError
  end
  alias do_GET do_POST
end
