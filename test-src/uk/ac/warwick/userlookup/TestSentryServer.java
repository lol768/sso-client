package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OnlyOnceErrorHandler;
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
	
	private static final Logger LOGGER = Logger.getLogger(TestSentryServer.class);
	
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

	private List<Map<String,String>> results = new ArrayList<Map<String,String>>();

	private UserResolver userResolver;

	private boolean onlyIfFound;
	
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
		
		List<String> requestedUserIds = Arrays.asList(req.getParameterValues("user"));
		
		boolean first = true;
		PrintWriter writer = res.getWriter();
		if (userResolver == null) {
			for (Map<String,String> attributes : results) {
				if (onlyIfFound && !requestedUserIds.contains(attributes.get("user"))) {
					continue;
				}
				if (!first) {
					writer.println("-------");
				}
				writeAttributes(writer, attributes);
				first = false;
			}
		} else {
			for (String userId : requestedUserIds) {
				if (!first) {
					writer.println("-------");
				}
				writeAttributes(writer, toMap(userResolver.getUserByUserId(userId)));
				first = false;
			}
		}
		req.setHandled(true);
	}

	private void writeAttributes(PrintWriter writer,
			Map<String, String> attributes) {
		for (Entry<String,String> entry : attributes.entrySet()) {
			writer.print(entry.getKey());
			writer.print("=");
			writer.println(entry.getValue());
		}
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
			LOGGER.debug("Server started");
			callback.run();
		} finally {
			LOGGER.debug("Server stopping");
			server.stop();
			server.destroy();
		}
	}

	/**
	 * The server will return these users regardless of what is requested.
	 */
	public void willReturnUsers(User... users) {
		brokenServer = false;
		userResolver = null;
		List<Map<String,String>> map = new ArrayList<Map<String,String>>();
		for (User user : users) {
			map.add(toMap(user));
		}
		setResults(map);
	}
	
	public void willReturnUsersIfFound(User...users) {
		willReturnUsers(users);
		onlyIfFound = true;
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

	/**
	 * Allows the test server to dynamically generate User results to use
	 * when 
	 */
	public void willReturnUsers(UserResolver userResolver) {
		this.userResolver = userResolver; 
	}

	/**
	 * The server will return all requested users as if they exist.
	 * For more complex requirements see {@link #willReturnUsers(UserResolver)}.
	 */
	public void willReturnAllUsers() {
		willReturnUsers(new UserResolver() {
			public User getUserByUserId(String uncheckedUserId) {
				User u = new User(uncheckedUserId);
				u.setFoundUser(true);
				return u;
			}
		});
	}
}
