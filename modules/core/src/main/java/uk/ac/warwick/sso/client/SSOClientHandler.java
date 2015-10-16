package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.HttpRequest;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.sso.client.core.Response;
import uk.ac.warwick.userlookup.UserLookupInterface;

import java.io.IOException;

/**
 * Takes a request and checks for an existing session by looking
 * at session cookies and the user cache.
 */
public interface SSOClientHandler {
    Response handle(HttpRequest request) throws IOException;

    AttributeAuthorityResponseFetcher getAaFetcher();

    void setAaFetcher(AttributeAuthorityResponseFetcher aaFetcher);

    UserCache getCache();

    void setCache(UserCache cache);

    Configuration getConfig();

    boolean isDetectAnonymousOnCampusUsers();

    void setDetectAnonymousOnCampusUsers(boolean detectAnonymousOnCampusUsers);

    boolean isRedirectToRefreshSession();

    void setRedirectToRefreshSession(boolean redirectToRefreshSession);

    UserLookupInterface getUserLookup();

    OnCampusService getOnCampusService();

    void setOnCampusService(OnCampusService onCampusService);
}
