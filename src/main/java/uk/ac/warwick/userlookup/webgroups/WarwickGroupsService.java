/*
 * Created on 31-Mar-2005
 *
 */
package uk.ac.warwick.userlookup.webgroups;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.UserLookupVersionLoader;
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig;
import uk.ac.warwick.userlookup.HttpMethodWebService.GetMethodFactory;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;
import uk.ac.warwick.util.cache.Cache;

public class WarwickGroupsService implements GroupService {

	private static final String SERVER_ERROR_MESSAGE = "Error communicating with WebGroups server";

	public static final String TYPE = "WarwickGroups";

	private String _version;
	
	

	public static interface ExecuteAndParseEngine {
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
		// default constructor

		if (_version == null || _version.equals("")) {
			_version = UserLookupVersionLoader.getVersion();
		}

	}

	public WarwickGroupsService(final String serviceLocation) {
		_serviceLocation = serviceLocation;

		if (_version == null || _version.equals("")) {
			_version = UserLookupVersionLoader.getVersion();
		}
	}

	public List<Group> getGroupsForUser(final String userId) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}

		String urlPath = getServiceLocation() + "/query" + "/user/" + userId + "/groups";
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<Group> getGroupsForDeptCode(final String deptCode) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}

		String urlPath = getServiceLocation() + "/query/search/deptcode/" + deptCode;
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<Group> getGroupsForQuery(final String search) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}

		if (search == null || search.trim().equals("")) {
			return Collections.emptyList();
		}

		String urlPath = getServiceLocation() + "/query/search/name/" + search.trim();
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	/**
	 * @param urlPath
	 * @return
	 */
	private List<Group> doQuery(String urlPath) throws HttpMethodWebService.WebServiceException {
		GroupsXMLResponseHandler handler = new GroupsXMLResponseHandler();
		engine.execute(urlPath, handler);
		return new ArrayList<Group>(handler.getResult());
	}

	public final Group getGroupByName(String groupName) throws GroupNotFoundException, GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}
		if (groupName == null || groupName.trim().equals("")) {
			throw new GroupNotFoundException("(null)");
		}

		groupName = groupName.trim();
		groupName = groupName.replaceAll(" ", "%20");

		String urlPath = getServiceLocation() + "/query" + "/group/" + groupName + "/details";
		GroupsXMLResponseHandler handler = new GroupsXMLResponseHandler();
		try {
			engine.execute(urlPath, handler);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
		Collection<Group> groups = handler.getResult();
		if (groups.isEmpty()) {
			LOGGER.warn("Group not found:" + groupName);
			throw new GroupNotFoundException(groupName);
		}
		return (Group) groups.iterator().next();
	}

	public boolean isUserInGroup(final String userId, final String group) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}
		if ("".equals(userId)) {
			LOGGER.debug("User is blank, so returning false for isInGroup");
			return false;
		}

		String urlPath = getServiceLocation() + "/query" + "/user/" + userId + "/member/" + group;
		BooleanResponseHandler handler = new BooleanResponseHandler();
		try {
			engine.execute(urlPath, handler);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
		return handler.getResult();
	}

	public List<String> getUserCodesInGroup(final String groupName) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}

		Group group;
		try {
			group = getGroupByName(groupName);
		} catch (GroupNotFoundException e) {
			return Collections.emptyList();
		}

		return group.getUserCodes();
	}

	public List<Group> getRelatedGroups(final String groupName) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("URI to Webgroups is invalid - check configuration");
			throw new GroupServiceException("URI to Webgroups is invalid - check configuration");
		}

		String urlPath = getServiceLocation() + "/query" + "/group/" + groupName + "/groups";

		try {
			List<Group> groups = doQuery(urlPath);
			if (groups.isEmpty()) {
				return Collections.emptyList();
			}
			return groups;
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
	}

	public List<String> getGroupsNamesForUser(final String userId) throws GroupServiceException {
		Collection<Group> groups = getGroupsForUser(userId);
		Set<String> groupNames = new HashSet<String>();
		for (Group group : groups) {
			groupNames.add(group.getName());
		}
		return new ArrayList<String>(groupNames);
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

	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException, GroupServiceException {
		String urlPath = getServiceLocation() + "/query" + "/group/" + name + "/info";
		GroupsInfoXMLResponseHandler handler = new GroupsInfoXMLResponseHandler();
		try {
			engine.execute(urlPath, handler);
		} catch (WebServiceException e) {
			throw new GroupServiceException(SERVER_ERROR_MESSAGE, e);
		}
		return handler.getGroupInfo();
	}

	public Map<String, Set<Cache<?, ?>>> getCaches() {
		return Collections.emptyMap();
	}

	public void clearCaches() {
	}
}
