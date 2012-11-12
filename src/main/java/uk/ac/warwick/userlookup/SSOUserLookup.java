package uk.ac.warwick.userlookup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.warwick.userlookup.HttpMethodWebService.GetMethodFactory;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;

/**
 * Finds users by filter by calling Websignon's API. The
 * API returns XML which this class converts to a List of 
 * User objects.
 */
final class SSOUserLookup implements UserFilter {

	private static final String USER_SEARCH_PATH = "/origin/api/userSearch.htm";

	//This should match the prefix that SSO expects.
	public static final String FILTER_PARAM_PREFIX = "f_";
	
	private static final Logger LOGGER = Logger.getLogger(SSOUserLookup.class);
	
	private String _ssosUrl;
	private String _apiKey;
	private WebServiceTimeoutConfig _timeoutConfig;
	private String _version;
	
	public SSOUserLookup(String ssosUrl, String apiKey) {
		this._ssosUrl = getSsosUrl(ssosUrl);
		this._apiKey = apiKey;
		if (_version == null || _version.equals("")) {
			_version = UserLookupVersionLoader.getVersion();
		}
	}
	
	/**
	 * @param returnDisabledUsers if true, returns all users found. If false, removes logindisabled=true users from the result.
	 * @return List[User]
	 */
	public List<User> findUsersWithFilter(Map<String,String> filterValues, boolean returnDisabledUsers)
			throws UserLookupException {
		String error;
		Exception exception;
		try {
			HttpMethodWebService service = new HttpMethodWebService(new URL(_ssosUrl), new GetMethodFactory(), getTimeoutConfig(), _version, _apiKey);
			Map<String,Object> parameters = new HashMap<String,Object>();
			for (Entry<String,String> entry : filterValues.entrySet()) {
				parameters.put(FILTER_PARAM_PREFIX + entry.getKey(), entry.getValue());
			}
			
			UserSearchResponseHandler handler = new UserSearchResponseHandler();
			service.doRequest(parameters, handler);
			List<User> allUsers = handler.getResult();
			List<User> users;
			
			if (returnDisabledUsers) {
				users = allUsers;
			} else {
				users = new LinkedList<User>();
				for (User u : allUsers) {
					if (!u.isLoginDisabled()) {
						users.add(u);
					}
				}
			}
			
			return users;
			
		} catch (MalformedURLException e) {
			exception = e;
			error = "Invalid SSOS url: " + _ssosUrl;
		} catch (HandlerException e) {
			exception = e;
			error = "Error reading XML response from SSOS";
		} catch (WebServiceException e) {
			exception = e;
			error = "Exception while contacting SSOS";
		}
		LOGGER.warn(exception);
		throw new UserLookupException(error, exception);
	}
	
	private String getSsosUrl(String providedUrl) {
		String s;
		if (providedUrl == null) {
			s = UserLookup.getConfigProperty("userlookup.ssosUrl");
		} else {
			s = providedUrl;
		}
		s = s.replaceFirst("(/origin)?/sentry", "");
		return s + USER_SEARCH_PATH;
	}
	
	public final WebServiceTimeoutConfig getTimeoutConfig() {
		if (_timeoutConfig == null) {
			_timeoutConfig = new WebServiceTimeoutConfig(0,0);
		}
		return _timeoutConfig;
	}
	
	

	static class UserSearchResponseHandler extends XMLResponseHandler<List<User>> {
		private List<User> _users;
		
		public UserSearchResponseHandler() {
			super(new UserSearchResponseParser());
		}

		public List<User> getResult() {
			return _users;
		}

		protected void collectResult(ContentHandler parser) {
			_users = ((UserSearchResponseParser)parser).getUsers();
		}
	}
	
	static class UserSearchResponseParser extends DefaultHandler {
		
		private Map<String,String> _currentUserAttributes;
		private List<User> _users = new ArrayList<User>();
		
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			if (name.equals("user")) {
				_currentUserAttributes = new HashMap<String,String>();
			} else if (name.equals("attribute")) {
				_currentUserAttributes.put(
						attributes.getValue("", "name"),
						attributes.getValue("", "value")
						);
			}
		}
		
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if (name.equals("user")) {
				User user = populateUserFromResult(_currentUserAttributes);
				if (user == null) {
					LOGGER.warn("Skipping null user (probably disabled account, but shouldn't be getting these");
				} else {
					_users.add(user);
				}
			}
		}
		
		public List<User> getUsers() {
			return _users;
		}
		
		/**
		 * TODO move this populating code somewhere else
		 */
		private User populateUserFromResult(final Map<String,String> attributes) {

			String userDn = getAttribute("dn", attributes);
			
			boolean loginDisabled = "true".equalsIgnoreCase(getAttribute("logindisabled", attributes));
			
//			if (getAttribute("logindisabled", attributes).equalsIgnoreCase("true")) {
//				LOGGER.debug(userDn + " has logindisabled attribute");
//				return null;
//			}

			LOGGER.debug("Populating user:" + userDn);

			User blankUser = new User();
			blankUser.setEmail(getAttribute("mail", attributes));
			blankUser.setFirstName(getAttribute("givenName", attributes));
			blankUser.setLastName(getAttribute("sn", attributes));
			blankUser.setUserId(getAttribute("cn", attributes));
			blankUser.setDepartment(getAttribute("ou", attributes));
			blankUser.setWarwickId(getAttribute("warwickuniid", attributes));
			blankUser.setDepartmentCode(getAttribute("warwickdeptcode", attributes));
			blankUser.setShortDepartment(getAttribute("deptshort", attributes));
			blankUser.setFoundUser(true);
			blankUser.setLoginDisabled(loginDisabled);
			
			/**
			 * Now we are fetching through SSO we can use the attributes it's set
			 * to determine staff/student, instead of looking at the DN
			 */
			blankUser.setStaff(getAttribute("staff", attributes).equals("true"));
			blankUser.setStudent(getAttribute("student", attributes).equals("true"));

			// blankUser.setGroupsLookup(_groupInfoService);

			return blankUser;
		}
		
		private String getAttribute(String key, Map<String,String> map) {
			if (map.containsKey(key)) {
				return map.get(key);
			} else {
				return "";
			}
		}
	}

}
