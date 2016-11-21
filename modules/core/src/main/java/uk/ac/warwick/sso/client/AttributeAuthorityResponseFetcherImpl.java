/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;
import uk.ac.warwick.userlookup.AdapterUserBuilder;
import uk.ac.warwick.userlookup.User;

import java.util.Properties;

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

    public static User createUserFromAttributes(final Properties attributes) {
        SAMLUserAdapter samlUserAdapter = new SAMLUserAdapter(attributes);

        return AdapterUserBuilder.buildUser(samlUserAdapter);
    }

}
