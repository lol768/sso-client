package uk.ac.warwick.userlookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

public final class UserBuilder {
    public static final Logger LOGGER = LoggerFactory.getLogger(UserBuilder.class);

    public User populateUser(final Map<String, String> results) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating user from SSO results");
            for (Entry<String, String> entry : results.entrySet()) {
                LOGGER.debug(entry.getKey() + "=" + entry.getValue());
            }
        }

        SentryUserAdapter sentryUserAdapter = new SentryUserAdapter(results);

        return AdapterUserBuilder.buildUser(sentryUserAdapter);
    }
}
