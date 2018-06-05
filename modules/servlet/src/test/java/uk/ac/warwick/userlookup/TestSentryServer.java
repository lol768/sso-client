package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSentryServer.class);
	
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
	
	private Map<String,String> columnToLdapMappings;
	
	private List<Map<String,String>> lookupResults = new ArrayList<Map<String,String>>();

	private List<Map<String,String>> results = new ArrayList<Map<String,String>>();

	private String successType = "4";

	private UserResolver userResolver;

	private boolean onlyIfFound;

	private Server server;
	
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
		
		

		if (req.getRequestURI().startsWith("/sentry")) {
			List<String> requestedUserIds = Arrays.asList(req.getParameterValues("user"));
			handleSentry(req, res, requestedUserIds);
		} else if (req.getRequestURI().startsWith("/origin/api/userSearch")) {			
			Map<String,String[]> filter = new HashMap<>();
			for (Object key : EnumerationUtils.toList(req.getParameterNames())) {
				String name = (String)key;
				if (name.startsWith("f_")) {
					String attribute = name.substring(2);
					String[] values = req.getParameterValues(name);
					filter.put(attribute, values);
					LOGGER.info(String.format("Received filter attribute %s=%s", attribute, StringUtils.join(values, ",")));
					//System.out.println(attribute + "=" + value);
				}
			}
			
			PrintWriter out = res.getWriter();
			out.println("<users>");
			
			for (Map<String,String> result : lookupResults) {
				for (Entry<String,String[]> entry : filter.entrySet()) {
					String columnName = entry.getKey();
					if (result.containsKey(columnName) &&
							filterMatches(entry.getValue(), result.get(columnName))) {
						out.println(" <user>");
						for (Entry<String,String> attr : result.entrySet()) {
							out.println("  <attribute name=\""+attr.getKey()+"\" value=\""+attr.getValue()+"\" />");
						}
						out.println(" </user>");
					}
				}
			}
			out.println("</users>");
			
			req.setHandled(true);
		}
	}

	/**
	 * Used for the pretend attribute search. Assumes for the moment that
	 * it will match any attribute that begins with the search value.
	 */
	private boolean filterMatches(String[] filterValues, String attributeValue) {
		return Arrays.stream(filterValues).anyMatch( filterValue -> {
			if (filterValue.endsWith("*")) {
				return (attributeValue.startsWith(filterValue.substring(0, filterValue.length() - 1)));
			} else {
				return attributeValue.equals(filterValue);
			}
		});
	}

	private void handleSentry(Request req, HttpServletResponse res,
			List<String> requestedUserIds) throws IOException {
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
	
	public void startup() throws Exception {
		if (server != null) {
			throw new IllegalStateException("server already created");
		}
		server = new Server(port);
		server.setStopAtShutdown(true);
		server.setHandler(this);
		server.start();
		LOGGER.debug("Server started");
	}
	
	public void shutdown() throws Exception {
		LOGGER.debug("Server stopping");
		server.stop();
		server.destroy();
		server = null;
	}
	
	public String getPath() {
		return "http://127.0.0.1:"+port;
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
	
	public void willNotReturnUsers(String... ids) {
		Iterator<Map<String, String>> it = results.iterator();
		while (it.hasNext()) {
			Map<String, String> next = it.next();
			if (ArrayUtils.contains(ids, next.get("user"))) {
				it.remove();				
			}
		}
		onlyIfFound = true;
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
			results.put("returnType", successType);
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
	 * Provides maps of attributes to be searched across when doing a user
	 * search.
	 * 
	 * @param results List of maps. Each map is a user's attributes. The key
	 * 	should either be an LDAP compatible name, OR a column name that has
	 *  a mapping to LDAP in {@link #getColumnToLdapMappings()}
	 */
	public void setSearchResults(List<Map<String,String>> results) {
		List<Map<String,String>> translated = new ArrayList<Map<String,String>>();
		for (Map<String,String> map : results) {
			Map<String,String> translatedMap = new HashMap<String, String>();
			for (Entry<String,String> entry : map.entrySet()) {
				String newKey = entry.getKey();
				if (getColumnToLdapMappings().containsKey(newKey)) {
					newKey = getColumnToLdapMappings().get(entry.getKey());
				}
				translatedMap.put(newKey, entry.getValue());
			}
			translated.add(translatedMap);
		}
		this.lookupResults = translated;
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

	/**
	 * Maps friendly names to LDAP names for calling {@link #setSearchResults(List)}
	 */
	public final Map<String, String> getColumnToLdapMappings() {
		if (columnToLdapMappings == null) {
			columnToLdapMappings = new HashMap<String, String>();
			columnToLdapMappings.put("User ID","cn");
			columnToLdapMappings.put("Surname","sn");
		}
		return columnToLdapMappings;
	}

	public void setSuccessType(String t) {
		this.successType = t;
	}
}
