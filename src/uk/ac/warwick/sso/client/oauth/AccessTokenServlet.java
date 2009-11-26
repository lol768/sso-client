package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

@SuppressWarnings("serial")
public class AccessTokenServlet extends AbstractOAuthServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            generateAccessToken(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            generateAccessToken(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void generateAccessToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get the canonical URL for upgrading a token out of the SSO Config
        
        OAuthMessage requestMessage = OAuthServlet.getMessage(request, getConfig().getString("oauth.accesstoken.location"));

        OAuthToken entry = getValidatedEntry(requestMessage);
        if (entry == null)
            throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);

        if (entry.getCallbackToken() != null) {
            // We're using the fixed protocol
            String clientCallbackToken = requestMessage.getParameter(OAUTH_VERIFIER);
            if (!entry.getCallbackToken().equals(clientCallbackToken)) {
                disableToken(entry);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is not authorized");
                return;
            }
        } else if (!entry.isAuthorised()) {
            // Old protocol. Catch consumers trying to convert a token to
            // one that's not authorized
            disableToken(entry);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is not authorized");
            return;
        }

        // turn request token into access token
        OAuthToken accessEntry = convertToAccessToken(entry);

        sendResponse(response, OAuth.newList(OAuth.OAUTH_TOKEN, accessEntry.getToken(), OAuth.OAUTH_TOKEN_SECRET,
                accessEntry.getTokenSecret(), "user_id", entry.getUserId()));
    }

    private OAuthToken getValidatedEntry(OAuthMessage requestMessage) throws Exception {

        OAuthToken entry = getToken(requestMessage.getToken());
        if (entry == null)
            throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);

        if (entry.getType() != OAuthToken.Type.REQUEST)
            throw new OAuthProblemException(OAuth.Problems.TOKEN_USED);

        if (entry.isExpired())
            throw new OAuthProblemException(OAuth.Problems.TOKEN_EXPIRED);

        // find consumer key, compare with supplied value, if present.

        if (requestMessage.getConsumerKey() == null) {
            OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_ABSENT);
            e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_ABSENT, OAuth.OAUTH_CONSUMER_KEY);
            throw e;
        }

        String consumerKey = entry.getConsumerKey();
        if (!consumerKey.equals(requestMessage.getConsumerKey()))
            throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_REFUSED);

        OAuthConsumer consumer = getConsumer(consumerKey);

        if (consumer == null)
            throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);

        OAuthAccessor accessor = new OAuthAccessor(consumer);

        accessor.requestToken = entry.getToken();
        accessor.tokenSecret = entry.getTokenSecret();

        validateMessage(requestMessage, accessor);

        return entry;
    }

    public final void init(final ServletConfig ctx) throws ServletException {
        super.init(ctx);

        // Configure any wired stuff here
    }

}
