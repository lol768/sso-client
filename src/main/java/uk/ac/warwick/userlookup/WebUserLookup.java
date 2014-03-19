/*
 * Created on 02-Dec-2003
 *  
 */
package uk.ac.warwick.userlookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;

/**
 * @author Kieran Shaw
 */
public class WebUserLookup implements UserLookupBackend {


	//The charset served by SSO sentry, and so the one we should specify
	//for reading the response.
	private static final String SENTRY_RESPONSE_CHARSET = "ISO-8859-1";

	private static final Logger LOGGER = Logger.getLogger(WebUserLookup.class);

	/**
	 * If making a batch request for users greater than this size,
	 * it will be partitioned into multiple web requests to avoid
	 * the request timing out. It could still time out, but it's much
	 * less likely.
	 */
	public static final int BATCH_USER_SIZE = Integer.parseInt(UserLookup.getConfigProperty("ssoclient.userlookup.batch.size"));
	
	private static final int CHECK_TOKEN = 1;

	private static final int TOKEN_OK = 1;

	private static final int LOGOUT = 3;

	private static final int CHECK_AUTH = 2;

	private static final int AUTH_OK = 2;

	private static final int USER_ID = 4;
	
	private static final int USERS_IDS = 5;

	private static final int USER_ID_OK = 4;
	
	private static final int CLEAR_GROUP = 6;

	private String _ssosUrl;
	
	private String _apiKey;

	private final WebServiceTimeoutConfig _timeoutConfig;

	private String _version;

	/**
	 * If no service url is passed in, it will look at UserLookup.getConfigProperty("userlookup.ssosUrl")
	 */
	public WebUserLookup(final String ssosUrl, final WebServiceTimeoutConfig config, final String version, final String apiKey) {
		_timeoutConfig = config;
		_ssosUrl = getSsosUrl(ssosUrl);
		_version = version;
		_apiKey = apiKey;
	}
	
	private String getSsosUrl(String providedUrl) {
		String s;
		if (providedUrl == null) {
			s = UserLookup.getConfigProperty("userlookup.ssosUrl", "https://websignon.warwick.ac.uk/sentry");
		} else {
			s = providedUrl;
		}
		if (s.indexOf("/sentry") == -1) {
			s += "/sentry";
		}
		return s;
	}

	public final User signIn(final String userId, final String pass) throws UserLookupException {
		LOGGER.debug("Trying signIn(" + userId + ",--password--)");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("requestType", String.valueOf(CHECK_AUTH));
		params.put("user", userId);
		params.put("pass", pass);
		Map<String,String> results = doSSO(params);
		LOGGER.debug("Got back results from SSO");
		if (getResultType(results) == AUTH_OK) {
			// populate user and return
			User returnUser = populateUser(results);
			return returnUser;
		}
		LOGGER.debug("Returning an AnonymousUser");
		return new AnonymousUser();
	}

	public final User getUserByToken(final String token) throws UserLookupException {
		if (LOGGER.isDebugEnabled()) LOGGER.debug("getUserByToken: " + token);
		User user;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("requestType", String.valueOf(CHECK_TOKEN));
		params.put("token", token);
		Map<String,String> results = doSSO(params);
		if (getResultType(results) == TOKEN_OK) {
			// populate user and return
			user = populateUser(results);
			user.setOldWarwickSSOToken(token);
			user.setFoundUser(true);
		} else {
			user = new AnonymousUser();
		}
		return user;
	}

	private static User populateUser(final Map<String,String> results) {
		return new UserBuilder().populateUser(results);
	}

	/**
	 * Find the value of key resultType in SSO's response, as an int for convenience
	 */
	private static int getResultType(final Map<String,String> results) {
		int rev;
		String res = getResult(results, "returnType");
		if (res == null) {
			LOGGER.warn("Got null returnType from SSO");
			return 0;
		}
		try {
			rev = Integer.parseInt(res);
		} catch (NumberFormatException n) {
			LOGGER.warn("Got bad returnType " + res + " from SSO");
			return 0;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Auth result:" + rev);
		}
		return rev;
	}

	/**
	 * After doing the SSO check, find out what was the value of a particular key in SSO's response
	 */
	private static String getResult(final Map<String,String> resultSet, final String key) {
		if (resultSet == null) {
			return null;
		}
		return resultSet.get(key);
	}

	public final User getUserById(final String userId) throws UserLookupException {
		User user;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getUserById: " + userId);
		}
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("requestType", new Integer(USER_ID).toString());
		params.put("user", userId);
		Map<String,String> results = doSSO(params);
		user = parsePropertiesToUser(results);
		return user;
	}

	private User parsePropertiesToUser(Map<String,String> results) {
		User user;
		if (getResultType(results) == USER_ID_OK) {
			// populate user and return
			user = populateUser(results);
			user.setFoundUser(true);
		} else {
			user = new AnonymousUser();
		}
		return user;
	}
	
	/**
	 * Gets users by ID, and returns them in a map of ID->User.
	 * 
	 * Any users not found are not in the map, even as anonymous users.
	 */
	public Map<String, User> getUsersById(List<String> userIds) throws UserLookupException {
		if (userIds.size() <= BATCH_USER_SIZE) {
			return doGetUsersById(userIds);
		} else {
			Map<String,User> allBatches = new HashMap<String, User>();
			for (int start = 0; start<userIds.size(); start += BATCH_USER_SIZE) {
				int end = Math.min(start + BATCH_USER_SIZE, userIds.size());
				List<String> sublist = userIds.subList(start, end);
				allBatches.putAll(doGetUsersById(sublist));
			}
			return allBatches;
		}
	}

	private Map<String, User> doGetUsersById(List<String> userIds)
			throws UserLookupException {
		Map<String,User> users = new HashMap<String,User>();
		LOGGER.debug("getUsersById, " + userIds.size() + " users");
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("requestType", String.valueOf(USERS_IDS));
		params.put("user", userIds);
		List<Map<String,String>> resultsList = doSSOMulti(params);
		for (Map<String,String> results : resultsList) {
			User user = parsePropertiesToUser(results);
			users.put(user.getUserId(), user);
		}
		return users;
	}

	private Map<String,String> doSSO(final Map<String,Object> params) throws UserLookupException {
		HttpMethodWebService service = new HttpMethodWebService(getServer(), new HttpMethodWebService.PostMethodFactory(),
				_timeoutConfig, _version, _apiKey);

		// inner class to process the response from SSO
		WebServiceResponseHandlerImplementation responseHandler = new WebServiceResponseHandlerImplementation();

		try {
			service.doRequest(params, responseHandler);
		} catch (WebServiceException e) {
			throw new UserLookupException("Exception connecting to SSO Service", e);
		} catch (HandlerException e) {
			throw new UserLookupException("Exception processing results from SSO Service", e);
		}
		return responseHandler.getResults();
	}
	
	private List<Map<String,String>> doSSOMulti(final Map<String,Object> params) throws UserLookupException {
		/*
		 * It's important that PostMethodFactory is used here, because checking for a lot of users
		 * will result in a very long parameters list that might be too much for a GET query string.
		 */
		HttpMethodWebService service = new HttpMethodWebService(getServer(), new HttpMethodWebService.PostMethodFactory(),
				_timeoutConfig, _version, _apiKey);

		// inner class to process the response from SSO
		WebServiceResponseHandlerListImplementation responseHandler = new WebServiceResponseHandlerListImplementation();

		try {
			service.doRequest(params, responseHandler);
		} catch (WebServiceException e) {
			throw new UserLookupException("Exception connecting to SSO Service", e);
		} catch (HandlerException e) {
			throw new UserLookupException("Exception processing results from SSO Service", e);
		}
		return responseHandler.getResultsList();
	}
	

	/**
	 * Returns the SSO server URL in use.
	 * 
	 * @return URL
	 */
	private URL getServer() throws UserLookupException {
		try {
			return new URL(_ssosUrl);
		} catch (MalformedURLException e1) {
			LOGGER.error("userlookup.ssosUrl is not a valid URL; its value is " + _ssosUrl, e1);
			throw new UserLookupException("Invalid single-sign-on service url", e1);
		}
	}

	public final void signOut(final String token) throws UserLookupException {
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("requestType", new Integer(LOGOUT).toString());
		params.put("token", token);
		doSSO(params);
	}

	public final User getUserByUserIdAndPassNonLoggingIn(final String usercode, final String password) throws UserLookupException {
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("requestType", new Integer(CHECK_AUTH).toString());
		params.put("user", usercode);
		params.put("pass", password);
		params.put("skiplogin", "true");
		Map<String, String> results = doSSO(params);
		if (getResultType(results) == AUTH_OK) {
			// populate user and return
			User returnUser = populateUser(results);
			return returnUser;
		}
		return new AnonymousUser();
	}
	
	final static class WebServiceResponseHandlerImplementation extends 
			ClearGroupResponseHandler implements
			HttpMethodWebService.WebServiceResponseHandler {
		private final Map<String,String> resultSet;
		
		private WebServiceResponseHandlerImplementation() {
			this.resultSet = new HashMap<String, String>();
		}
		
		public final void processResults(final InputStream fromServer) throws HttpMethodWebService.HandlerException {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(fromServer, Charset.forName(SENTRY_RESPONSE_CHARSET)));
				while (true) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					String name = line.substring(0, line.indexOf("="));
					String value = line.substring(line.indexOf("=") + 1, line.length());
					resultSet.put(name, value);
				}
			} catch (IOException e) {
				throw new HttpMethodWebService.HandlerException(e);
			}
		}
		
		public Map<String,String> getResults() { return resultSet; }
	}
	
	final static class WebServiceResponseHandlerListImplementation extends
			ClearGroupResponseHandler implements
			HttpMethodWebService.WebServiceResponseHandler {
		private final List<Map<String,String>> resultSet;
		
		private WebServiceResponseHandlerListImplementation() {
			this.resultSet = new ArrayList<Map<String,String>>();
		}
		
		public final void processResults(final InputStream fromServer) throws HttpMethodWebService.HandlerException {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(fromServer, Charset.forName(SENTRY_RESPONSE_CHARSET)));
				Map<String,String> p = new HashMap<String,String>();
				while (true) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					if (line.startsWith("---")) {
						this.resultSet.add(p);
						p = new HashMap<String,String>();
					} else {
						int equals = line.indexOf("=");
						if (equals > 0 && equals <= line.length()) {
							String name = line.substring(0, equals);
							String value = line.substring(equals + 1, line.length());
							p.put(name, value);
						}
					}
				}
				if (!p.isEmpty()) {
					this.resultSet.add(p);
				}
			} catch (IOException e) {
				throw new HttpMethodWebService.HandlerException(e);
			}
		}
		
		public List<Map<String,String>> getResultsList() { return resultSet; }
	}	

	public boolean getSupportsBatchLookup() {
		return true;
	}
	
	public final void requestClearWebGroup(final String groupName) throws UserLookupException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("requestClearWebGroup: " + groupName);
		}
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("requestType", new Integer(CLEAR_GROUP).toString());
		params.put("group", groupName);
		doSSO(params);
	}
}