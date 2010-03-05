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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupImpl;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.UserLookupVersionLoader;
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig;
import uk.ac.warwick.userlookup.HttpMethodWebService.GetMethodFactory;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;

public class WarwickGroupsService implements GroupService {

	public static final String TYPE = "WarwickGroups";

	private String _version;
	
	

	public static interface ExecuteAndParseEngine {
		void execute(final String urlPath, final ResultAwareWebServiceResponseHandler<?> handler) throws WebServiceException;
	}

	private static final Logger LOGGER = Logger.getLogger(WarwickGroupsService.class);

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

	public List<Group> getGroupsForUser(final String userId) {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning empty groups");
			return PROBLEM_FINDING_GROUPS;
		}

		String urlPath = getServiceLocation() + "/query" + "/user/" + userId + "/groups";
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			LOGGER.warn("Problem looking up groups: ", e);
			return PROBLEM_FINDING_GROUPS;
		}
	}

	public List<Group> getGroupsForDeptCode(final String deptCode) {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning empty groups");
			return PROBLEM_FINDING_GROUPS;
		}

		String urlPath = getServiceLocation() + "/query/search/deptcode/" + deptCode;
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			return PROBLEM_FINDING_GROUPS;
		}
	}

	public List getGroupsForQuery(final String search) {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning empty groups");
			return PROBLEM_FINDING_GROUPS;
		}

		if (search == null || search.trim().equals("")) {
			return Collections.EMPTY_LIST;
		}

		String urlPath = getServiceLocation() + "/query/search/name/" + search.trim();
		try {
			return doQuery(urlPath);
		} catch (WebServiceException e) {
			return PROBLEM_FINDING_GROUPS;
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
			LOGGER.warn("uri is invalid, so returning empty groups");
			throw new GroupServiceException("No service url provided");
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
			LOGGER.warn("Error fetching info about group "+groupName+", returning empty group", e);
			GroupImpl group = new GroupImpl();
			group.setVerified(false);
			group.setName(groupName);
			return group;
		}
		Collection<Group> groups = handler.getResult();
		if (groups.isEmpty()) {
			LOGGER.warn("Group not found:" + groupName);
			throw new GroupNotFoundException(groupName);
		}
		return (Group) groups.iterator().next();
	}

	public boolean isUserInGroup(final String userId, final String group) {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning  false for isInGroup");
			return false;
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
			return false;
		}
		return handler.getResult();
	}

	public List getUserCodesInGroup(final String groupName) throws GroupServiceException {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning empty groups");
			return Collections.EMPTY_LIST;
		}

		Group group;
		try {
			group = getGroupByName(groupName);
		} catch (GroupNotFoundException e) {
			return Collections.EMPTY_LIST;
		}

		return group.getUserCodes();
	}

	public List getRelatedGroups(final String groupName) {
		if (getServiceLocation() == null || getServiceLocation().trim().length() == 0) {
			LOGGER.warn("uri is invalid, so returning empty groups");
			return Collections.EMPTY_LIST;
		}

		String urlPath = getServiceLocation() + "/query" + "/group/" + groupName + "/groups";

		try {
			List groups = doQuery(urlPath);
			if (groups.isEmpty()) {
				return Collections.EMPTY_LIST;
			}
			return groups;
		} catch (WebServiceException e) {
			return Collections.EMPTY_LIST;
		}
	}

	public List getGroupsNamesForUser(final String userId) {
		Collection groups = getGroupsForUser(userId);

		Set groupNames = new HashSet();
		for (Iterator i = groups.iterator(); i.hasNext();) {
			Group group = (Group) i.next();
			groupNames.add(group.getName());
		}
		return new ArrayList(groupNames);
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

	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException {
		String urlPath = getServiceLocation() + "/query" + "/group/" + name + "/info";
		GroupsInfoXMLResponseHandler handler = new GroupsInfoXMLResponseHandler();
		try {
			engine.execute(urlPath, handler);
		} catch (WebServiceException e) {
			return null;
		}
		return handler.getGroupInfo();
	}
}
