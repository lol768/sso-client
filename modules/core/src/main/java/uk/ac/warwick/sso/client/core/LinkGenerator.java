package uk.ac.warwick.sso.client.core;


/**
 * Non-Servlet version of the original SSO*LinkGenerator classes.
 */
public interface LinkGenerator {
    String getLoginUrl();

    String getLogoutUrl();

    String getPermissionDeniedLink(boolean loggedIn);

    String getNotLoggedInLink();

    String getTarget();

    void setTarget(String target);
}
