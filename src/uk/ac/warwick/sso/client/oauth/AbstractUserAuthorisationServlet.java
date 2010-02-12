package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;
import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.userlookup.User;

/**
 * A servlet designed for extension by the developer of the parent application,
 * which will present a form to the user asking whether they wish to authorise
 * this application, and accept the response from that form.
 */
@SuppressWarnings("serial")
public abstract class AbstractUserAuthorisationServlet extends AbstractOAuthServlet {

    /**
     * Show the form to the user.
     */
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = SSOClientFilter.getUserFromRequest(req);
        if (!user.isFoundUser() || !user.isLoggedIn()) {
            // This servlet should go through SSOClientFilter
            OAuthServlet.handleException(resp, new ServletException("Couldn't find user in request!"), getRealm(req), true);
        }

        String token = req.getParameter("token");
        String consumerTitle = req.getParameter("description");
        boolean unsignedCallback = Boolean.valueOf(req.getParameter("unsignedCallback"));
        String callback = req.getParameter("callback");

        showForm(req, resp, user, token, consumerTitle, callback, unsignedCallback);
    }

    protected abstract void showForm(HttpServletRequest req, HttpServletResponse resp, User user, String token,
            String consumerTitle, String callback, boolean unsignedCallback) throws ServletException, IOException;

    /**
     * Handle input from the form, sending to doAuthoriseToken() or
     * doDenyToken()
     */
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = SSOClientFilter.getUserFromRequest(req);
        if (!user.isFoundUser() || !user.isLoggedIn()) {
            // This servlet should go through SSOClientFilter
            OAuthServlet.handleException(resp, new ServletException("Couldn't find user in request!"), getRealm(req), true);
        }

        try {
            OAuthToken token = getToken(req.getParameter("token"));

            if (isAuthoriseRequest(req)) {
                doAuthoriseToken(token, user);
            } else if (isDenyRequest(req)) {
                doDenyToken(token);
            }

            // Back to the authorisation step
            String authoriseLocation = getConfig().getString("oauth.authorise.location");

            StringBuilder azn = new StringBuilder(authoriseLocation);
            if (azn.indexOf("?") == -1) {
                azn.append('?');
            } else {
                azn.append('&');
            }
            azn.append(OAuth.OAUTH_TOKEN);
            azn.append('=');
            azn.append(OAuth.percentEncode(token.getToken()));

            resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            resp.setHeader("Location", azn.toString());
        } catch (OAuthProblemException e) {
            OAuthServlet.handleException(resp, e, getRealm(req), true);
        }
    }

    protected abstract boolean isAuthoriseRequest(HttpServletRequest request) throws ServletException;

    protected abstract boolean isDenyRequest(HttpServletRequest request) throws ServletException;

}
