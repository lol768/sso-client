package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Starts an embedded HTTP server that will respond as though
 * it were the SSO userlookup sentry.
 * 
 * <pre>
 * UserLookup userLookup = new UserLookup();
 * TestSentryServer sentry = new Sentry();
 * userLookup.setSsosUrl( sentry.getPath() );
 * 
 * sentry.willReturnUsers( user("jim"), user("ron") );
 * sentry.run(new Runnable() { ... TEST HERE! ... });
 * </pre>
 */
public class TestSentryServer extends AbstractHandler {
	private int requests;
	
	// Port should be free unless you are running a Call of Duty server
	private int port = 28960;
	
	// If true, simulate a broken server and return 500s for everything
	private boolean brokenServer;
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private List<Map<String,String>> results;
	
	public void setResults(List<Map<String, String>> results) {
		this.results = results;
	}

	public int getRequestCount() {
		return requests;
	}
	
	public void clearResults() {
		this.results.clear();
	}

	public void handle(String target, Request req, HttpServletRequest hreq, HttpServletResponse res)
			throws IOException, ServletException {
		requests++;
		
		if (brokenServer) {
			throw new ServletException("Server configured to break");
		}
		
		boolean first = true;
		PrintWriter writer = res.getWriter();
		for (Map<String,String> attributes : results) {
			if (!first) {
				writer.println("-------");
			}
			for (Entry<String,String> entry : attributes.entrySet()) {
				writer.print(entry.getKey());
				writer.print("=");
				writer.println(entry.getValue());
			}
			first = false;
		}
		req.setHandled(true);
	}
	
	
	
	/**
	 * Starts a local server accessible for the duration of the
	 * given callback. As soon as the callback finishes, the server
	 * is always shut down. The callback is where your testing
	 * happens.
	 */
	public void run(Runnable callback) throws Exception {
		runServer(port, this, callback);
	}
	
	public String getPath() {
		return "http://127.0.0.1:"+port+"/";
	}
	
	public static void runServer(int port, Handler handler, Runnable callback)
			throws Exception {
		Server server = new Server(port);
		try {
			server.setHandler(handler);
			server.start();
			System.out.println("Server started");
			callback.run();
		} finally {
			System.out.println("Server stopping");
			server.stop();
			server.destroy();
		}
	}

	public void willReturnUsers(User... users) {
		brokenServer = false;
		List<Map<String,String>> results = new ArrayList<Map<String,String>>();
		for (User user : users) {
			results.add(toMap(user));
		}
		setResults(results);
	}
	
	public void willReturnErrors() {
		brokenServer = true;
	}
	
	private Map<String,String> toMap(User user) {
		Map<String,String> results = new HashMap<String, String>();
		results.put("user", user.getUserId());
		if (user.isFoundUser()) {
			results.put("name", user.getFullName());
			results.put("returnType", "4");
		} else if (user.isVerified()) {
			results.put("returnType", "54");
		} else {
			throw new IllegalArgumentException("there's no representation for an unverified user, the whole point is that the server didn't return a response!");
		}
		return results;
	}
}
