/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public class AttributeAuthorityResponseFetcherImpl extends AbstractSAMLFetcher implements AttributeAuthorityResponseFetcher {

	private static final Log LOGGER = LogFactory.getLog(AttributeAuthorityResponseFetcherImpl.class);
	
	protected AttributeAuthorityResponseFetcherImpl() {
		// default empty constructor
	}

	public AttributeAuthorityResponseFetcherImpl(final SSOConfiguration config) {
		super(config);
	}

	public final String getProxyTicket(final SAMLSubject subject, final String resource) throws SSOException {
		SAMLResponse response = getSAMLResponse(subject, resource);
		Properties attributes = getAttributesFromResponse(response);
		return getValueFromAttribute(SSOToken.PROXY_TICKET_TYPE, attributes);
	}

	public final User getUserFromSubject(final SAMLSubject subject) throws SSOException {
		SAMLResponse response = getSAMLResponse(subject);
		Properties attributes = getAttributesFromResponse(response);
		User user = createUserFromAttributes(attributes);

		LOGGER.info("Returning user " + user.getFullName() + "(" + user.getUserId() + ") from SAMLSubject");
		return user;
	}

	@Override
    protected String getEndpointLocation() {
        return getConfig().getString("origin.attributeauthority.location");
    }

	/**
	 * @param samlResp
	 * @return
	 */
	private User createUserFromAttributes(final Properties attributes) {
		User user = new User();
		user.setUserId(getValueFromAttribute("cn", attributes));
		user.setLastName(getValueFromAttribute("sn", attributes));
		user.setFirstName(getValueFromAttribute("givenName", attributes));
		user.setWarwickId(getValueFromAttribute("warwickuniid", attributes));
		user.setDepartmentCode(getValueFromAttribute("warwickdeptcode", attributes));
		user.setDepartment(getValueFromAttribute("ou", attributes));
		user.setShortDepartment(getValueFromAttribute("deptshort", attributes));
		user.setEmail(getValueFromAttribute("mail", attributes));

		user.setUserType(getValueFromAttribute("urn:websignon:usertype", attributes));

		if ("true".equals(getValueFromAttribute("staff", attributes))) {
			user.setStaff(true);
		}
		if ("true".equals(getValueFromAttribute("student", attributes))) {
			user.setStudent(true);
		}

		if (getValueFromAttribute("urn:websignon:loggedin", attributes) != null
				&& getValueFromAttribute("urn:websignon:loggedin", attributes).equals("true")) {
			user.setIsLoggedIn(true);
		}

		if (getValueFromAttribute("logindisabled", attributes) != null
				&& Boolean.valueOf(getValueFromAttribute("logindisabled", attributes)).booleanValue()) {
			user.setLoginDisabled(true);
		}

		user.setFoundUser(true);

		// dump all attributes as keys and strings into extraproperties map on the user
		Iterator<?> it = attributes.keySet().iterator();
		while (it.hasNext()) {
			String attrName = (String) it.next();
			String value = getValueFromAttribute(attrName, attributes);
			LOGGER.debug("Adding " + attrName + "=" + value + " to users extra properties");
			user.getExtraProperties().put(attrName, value);
		}

		return user;
	}

}
