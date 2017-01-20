/*
 * Created on 31-Mar-2005
 *
 */
package uk.ac.warwick.userlookup.webgroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SSOClientVersionLoader;
import uk.ac.warwick.userlookup.*;
import uk.ac.warwick.userlookup.HttpMethodWebService.GetMethodFactory;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.core.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class WarwickGroupsService implements GroupService {

	private static final String SERVER_ERROR_MESSAGE = "Error communicating with WebGroups server";

	public static final String TYPE = "WarwickGroups";

	private String _version;

	interface ExecuteAndParseEngine {
		void execute(final String urlPath, final ResultAwareWebServiceResponseHandler<?> handler) throws WebServiceException;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(WarwickGroupsService.class);

	private WebServiceTimeoutConfig _timeoutConfig;

	private String _serviceLocation;

	private ExecuteAndParseEngine engine = new ExecuteAndParseEngine() {

		public void execute(final String urlPath, final ResultAwareWebServiceResponseHandler<?> handler) throws WebServiceException {
			try {
				HttpMethodWebService arbitraryGroups = new HttpMethodWebService(new URL(urlPath),
						new GetMethodFactory(), getTimeoutConfig(), _version, null);
				arbitraryGroups.doRequest(Collections.<String,Object>emptyMap(), handler);
			} catch (final HandlerException e) {
				LOGGER.error("HandlerException", e);
			} catch (MalformedURLException e) {
				LOGGER.error("MalformedURLException: " + urlPath, e);
			}
		}
	};

	public WarwickGroupsService() {
		if (_version == null || _version.equals("")) {
			_version = SSOClientVersionLoader.getVersion();
		}
	}

	public WarwickGroupsService(final String serviceLocation) {
		this();
		_serviceLocation = serviceLocation;
	}

	public List<Group> getGroupsForUser(final String userId) throws GroupServiceException {
		assertServiceLocationExists();

		if (!StringUtils.hasText(userId) || hasInvalidCharacters(userId)) {
			return Collections.emptyList();
		}

		String urlPath = getServiceLocation() + "/query/user/" + encodePathElement(userId) + "/groups";
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<Group> getGroupsForDeptCode(final String deptCode) throws GroupServiceException {
		assertServiceLocationExists();

		if (!StringUtils.hasText(deptCode) || hasInvalidCharacters(deptCode)) {
			return Collections.emptyList();
		}

		String urlPath = getServiceLocation() + "/query/search/deptcode/" + encodePathElement(deptCode);
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<Group> getGroupsForQuery(final String search) throws GroupServiceException {
		assertServiceLocationExists();

		if (!StringUtils.hasText(search) || hasInvalidCharacters(search)) {
			return Collections.emptyList();
		}

		String urlPath = getServiceLocation() + "/query/search/name/" + encodePathElement(search);
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public final Group getGroupByName(String groupName) throws GroupNotFoundException, GroupServiceException {
		assertServiceLocationExists();
		if (!StringUtils.hasText(groupName) || hasInvalidCharacters(groupName)) {
			throw new GroupNotFoundException("(null)");
		}

		final String urlPath = getServiceLocation() + "/query/group/" + encodePathElement(groupName) + "/details";

		try {
			Collection<Group> groups = doQuery(urlPath);
			if (groups.isEmpty()) {
				LOGGER.warn("Group not found:" + groupName);
				throw new GroupNotFoundException(groupName);
			}
			return groups.iterator().next();
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}

	}

	public boolean isUserInGroup(final String userId, final String group) throws GroupServiceException {
		assertServiceLocationExists();
		if (!StringUtils.hasText(userId) || hasInvalidCharacters(userId)) {
			LOGGER.debug("User is [%s], so returning false for isInGroup", userId);
			return false;
		}
		if (!StringUtils.hasText(group) || hasInvalidCharacters(group)) {
			LOGGER.debug("Group is [%s], so returning false for isInGroup", group);
			return false;
		}

		final String urlPath = getServiceLocation() + "/query/user/" + encodePathElement(userId) + "/member/" + encodePathElement(group);

		try {
			BooleanResponseHandler handler = new BooleanResponseHandler();
			engine.execute(urlPath, handler);
			return handler.getResult();
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<String> getUserCodesInGroup(final String groupName) throws GroupServiceException {
		assertServiceLocationExists();
		try {
			return getGroupByName(groupName).getUserCodes();
		} catch (GroupNotFoundException e) {
			return Collections.emptyList();
		}
	}

	public List<Group> getRelatedGroups(final String groupName) throws GroupServiceException {
		assertServiceLocationExists();

		if (!StringUtils.hasText(groupName) || hasInvalidCharacters(groupName)) {
			return Collections.emptyList();
		}

		final String urlPath = getServiceLocation() + "/query/group/" + encodePathElement(groupName) + "/groups";

		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<String> getGroupsNamesForUser(final String userId) throws GroupServiceException {
		Collection<Group> groups = getGroupsForUser(userId);
		Set<String> groupNames = new HashSet<>();
		for (Group group : groups) {
			groupNames.add(group.getName());
		}
		return new ArrayList<>(groupNames);
	}

	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException, GroupServiceException {
		if (!StringUtils.hasText(name) || hasInvalidCharacters(name)) {
			throw new GroupNotFoundException(name);
		}
		String urlPath = getServiceLocation() + "/query/group/" + encodePathElement(name) + "/info";
		GroupsInfoXMLResponseHandler handler = new GroupsInfoXMLResponseHandler();
		try {
			engine.execute(urlPath, handler);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
		return handler.getGroupInfo();
	}

	/**
	 * Executes a query that returns a list of groups.
	 */
	private List<Group> doQuery(String urlPath) throws HttpMethodWebService.WebServiceException {
		GroupsXMLResponseHandler handler = new GroupsXMLResponseHandler();
		engine.execute(urlPath, handler);
		return new ArrayList<>(handler.getResult());
	}

	private String encodePathElement(String str) {
		try {
			return URLEncoder.encode(str.trim().replace('+',' '), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Sorry, your runtime is terrible.
			throw new RuntimeException("UTF-8 encoding not present in JRE");
		}
	}

	/**
	 * If the string has characters that are absolutely not allowed in
	 * any URL path parameters.
	 */
	private boolean hasInvalidCharacters(String input) {
		return input.contains("/") || input.contains("%2F");
	}

	private void assertServiceLocationExists() throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}
	}

	public final void setTimeoutConfig(final WebServiceTimeoutConfig timeoutConfig) {
		_timeoutConfig = timeoutConfig;
	}

	public final WebServiceTimeoutConfig getTimeoutConfig() {
		if (_timeoutConfig == null) {
			_timeoutConfig = new WebServiceTimeoutConfig(0, 0);
		}
		LOGGER.debug("Returning timeoutconfig: Connection=" + _timeoutConfig.getConnectionTimeout() + ", Data="
				+ _timeoutConfig.getDataTimeout());
		return _timeoutConfig;
	}

	public void setEngine(final ExecuteAndParseEngine engine) {
		this.engine = engine;
	}

	public final String getServiceLocation() {
		if (_serviceLocation == null) {
			return UserLookup.getConfigProperty("userlookup.groupservice.location");
		}
		return _serviceLocation;
	}

	public final void setServiceLocation(final String serviceLocation) {
		_serviceLocation = serviceLocation;
	}

	public final String getVersion() {
		return _version;
	}

	public final void setVersion(final String version) {
		_version = version;
	}

	public Map<String, Set<Cache<?, ?>>> getCaches() {
		return Collections.emptyMap();
	}

	public void clearCaches() {
	}
}
