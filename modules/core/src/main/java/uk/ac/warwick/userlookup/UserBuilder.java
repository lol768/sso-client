package uk.ac.warwick.userlookup;

import org.opensaml.SAMLAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SAMLUserAdapter;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public final class UserBuilder {
    public static final Logger LOGGER = LoggerFactory.getLogger(UserBuilder.class);

    /**
     * @deprecated Use populateUserFromSentry
     * @see UserBuilder#populateUserFromSentry
     */
    @Deprecated
    public User populateUser(final Map<String, String> results) {
        return populateUserFromSentry(results);
    }

    public User populateUserFromSentry(final Map<String, String> results) {
        SentryUserAdapter sentryUserAdapter = new SentryUserAdapter(results);
        return this.populateUsingAdapter(results, sentryUserAdapter);
    }

    public User populateUserFromSAML(final Map<String, String> results) {
        SAMLUserAdapter samlUserAdapter = new SAMLUserAdapter(results);
        return this.populateUsingAdapter(results, samlUserAdapter);
    }

    private User populateUsingAdapter(final Map<String, String> results, final AbstractUserAttributesAdapter adapter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating user from SSO results");
            for (Entry<String, String> entry : results.entrySet()) {
                LOGGER.debug(entry.getKey() + "=" + entry.getValue());
            }
        }
        return AdapterUserBuilder.buildUser(adapter);
    }
}
