package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

@SuppressWarnings("serial")
/**
 * @deprecated See SSO-840
 */
public class AuthorisationServlet extends AbstractOAuthServlet {
    
    public static final String AUTHORISE_PARAMETER = "authorise";
    
    public static final String DENY_PARAMETER = "authorise";
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            checkAuthorisation(req, resp);
        } catch (Exception e) {
            OAuthServlet.handleException(resp, e, getRealm(req), true);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            checkAuthorisation(req, resp);
        } catch (Exception e) {
            OAuthServlet.handleException(resp, e, getRealm(req), true);
        }
    }
    
    private void checkAuthorisation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get the canonical URL for the authorisation page out of the SSO config
        
        OAuthMessage requestMessage = OAuthServlet.getMessage(request, getConfig().getString("oauth.authorise.location"));
        
        boolean valid = false;
        OAuthToken token = null;
        OAuthConsumer consumer = null;
        if (requestMessage.getToken() == null) {
            // MALFORMED REQUEST
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authentication token not found");
        } else {
            token = getToken(requestMessage.getToken());
            if (token == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "OAuth token not found");
            } else {
                consumer = getConsumer(token.getConsumerKey());
                // Extremely rare case where consumer disappears
                if (consumer == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "consumer for entry not found");
                } else {
                    valid = true;
                }
            }
        }
        
        if (!valid || token == null || consumer == null) {
            return;
        }
        
        // A flag to deal with protocol flaws in OAuth/1.0
        boolean securityThreat = !token.isCallbackUrlSigned();

        // Check for a callback in the oauth entry
        String callback = token.getCallbackUrl();

        if (callback == null) {
            // see if there's a callback in the url params
            callback = requestMessage.getParameter(OAuth.OAUTH_CALLBACK);
        }

        if (callback == null) {
            // see if the consumer has a callback
            callback = consumer.callbackURL;
        }

        // The token is disabled if you try to convert to an access token
        // prior to authorization
        if (token.getType() == OAuthToken.Type.DISABLED) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is disabled, please reinitate login");
            return;
        }

        // Redirect to a UI flow if the token is not authorized
        if (!token.isAuthorised()) {
            sendToAuthorisationPage(request, response, token, consumer, securityThreat, callback);
            return;
        }

        // If we're here then the token has been authorized

        // redirect to callback
        if (callback == null || "oob".equals(callback)) {
            // consumer did not specify a callback
            outputToken(response, token);
        } else {
            redirectToCallback(response, token, callback);
        }
    }
    
    private void redirectToCallback(HttpServletResponse response, OAuthToken token, String callbackUrl) throws IOException {
        // Add parameters to the callback URL
        String callback = OAuth.addParameters(callbackUrl, OAuth.OAUTH_TOKEN, token.getToken());
        // Add user_id to the callback
        callback = OAuth.addParameters(callback, "user_id", token.getUserId());
        if (token.getCallbackToken() != null) {
            callback = OAuth.addParameters(callback, OAUTH_VERIFIER, token.getCallbackToken());
        }

        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", callback);
    }

    private void outputToken(HttpServletResponse response, OAuthToken token) throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.write("Token successfully authorised.\n");
        if (token.getCallbackToken() != null) {
            // Usability fail.
            out.write("Please enter code " + token.getCallbackToken() + " at the consumer.");
        }
    }

    private void sendToAuthorisationPage(HttpServletRequest request, HttpServletResponse response, OAuthToken token,
            OAuthConsumer consumer, boolean securityThreat, String callback) {
        // Build a URL with parameters and send it off to the consumer's own authorisation URL
        String location = getConfig().getString("oauth.authorise.userlocation");
        if (location.indexOf("?") == -1) {
            location += "?";
        }
        
        try {
            StringBuilder url = new StringBuilder(location);
            url.append("description=");
            url.append(URLEncoder.encode(consumer.getProperty("title").toString(), "UTF-8"));
            url.append("&");
            
            url.append("unsignedCallback=");
            url.append(Boolean.toString(securityThreat));
            url.append("&");
            
            url.append("token=");
            url.append(URLEncoder.encode(token.getToken(), "UTF-8"));
            url.append("&");
            
            url.append("callback=");
            
            if (callback != null) {
                url.append(URLEncoder.encode(callback, "UTF-8"));
            }
    
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", url.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}
