package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuth.Parameter;
import net.oauth.server.OAuthServlet;

@SuppressWarnings("serial")
/**
 * @deprecated See SSO-840
 */
public class RequestTokenServlet extends AbstractOAuthServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            generateRequestToken(req, resp);
        } catch (Exception e) {
            OAuthServlet.handleException(resp, e, getRealm(req), true);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            generateRequestToken(req, resp);
        } catch (Exception e) {
            OAuthServlet.handleException(resp, e, getRealm(req), true);
        }
    }

    private void generateRequestToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get the canonical URL for requesting a token out of the SSO Config
        
        OAuthMessage requestMessage = OAuthServlet.getMessage(request, getConfig().getString("oauth.requesttoken.location"));

        String consumerKey = requestMessage.getConsumerKey();
        if (consumerKey == null) {
            OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_ABSENT);
            e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_ABSENT, OAuth.OAUTH_CONSUMER_KEY);
            throw e;
        }
        
        OAuthConsumer consumer = getConsumer(consumerKey);
        if (consumer == null) {
            throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
        }

        OAuthAccessor accessor = new OAuthAccessor(consumer);
        validateMessage(requestMessage, accessor);

        String callback = null;
        if (getConfig().getBoolean("oauth.requesttoken.signedcallbacks")) {
            callback = requestMessage.getParameter(OAuth.OAUTH_CALLBACK);
        }
        if (callback == null && !getConfig().getBoolean("oauth.requesttoken.allow10")) {
            OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_ABSENT);
            e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_ABSENT, OAuth.OAUTH_CALLBACK);
            throw e;
        }

        // generate request_token and secret
        OAuthToken entry = generateRequestToken(consumerKey, requestMessage.getParameter(OAuth.OAUTH_VERSION), callback);

        List<Parameter> responseParams = OAuth.newList(OAuth.OAUTH_TOKEN, entry.getToken(), OAuth.OAUTH_TOKEN_SECRET,
                entry.getTokenSecret());
        if (callback != null) {
            responseParams.add(new Parameter(OAUTH_CALLBACK_CONFIRMED, "true"));
        }

        sendResponse(response, responseParams);
    }

    public final void init(final ServletConfig ctx) throws ServletException {
        super.init(ctx);

        // Configure any wired stuff here
    }

}
