package uk.ac.warwick.sso.client.oauth.uk.ac.warwick.sso.client.oauth;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.http.HttpMessage;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.core.HttpRequest;
import uk.ac.warwick.sso.client.core.LinkGenerator;
import uk.ac.warwick.sso.client.core.LinkGeneratorImpl;
import uk.ac.warwick.sso.client.core.Response;
import uk.ac.warwick.sso.client.oauth.OAuthService;
import uk.ac.warwick.sso.client.oauth.OAuthToken;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupInterface;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * For internal use. Handles OAuth in a request
 */
public class CoreOAuthHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreOAuthHandler.class);
    private final SSOConfiguration config;
    private final OAuthService service;
    private final UserLookupInterface userLookup;
    private boolean expiredToken401 = true;

    public CoreOAuthHandler(SSOConfiguration configuration, UserLookupInterface userLookup, OAuthService oauthService) {
        this.config = configuration;
        this.service = oauthService;
        this.userLookup = userLookup;
    }

    public Response handle(HttpRequest request, User existingUser) {
        Response response = new Response();
        if (existingUser != null && existingUser.isFoundUser()) {
            response.setUser(existingUser);
            response.setContinueRequest(true);
        } else {
            final LinkGenerator linkGenerator = new LinkGeneratorImpl(getConfig(), request);
            final String requestedUrl = linkGenerator.getTarget();

            final OAuthMessage message = getMessage(request, requestedUrl);
            try {
                if (message != null && message.getToken() != null) {

                    OAuthToken token = getOAuthService().getToken(message.getToken()).get();

                    if (token != null && token.isAuthorised() && !token.isExpired() && token.getType() == OAuthToken.Type.ACCESS
                            && token.getConsumerKey().equals(message.getConsumerKey())
                            && isCorrectScope(token, getConfig().getString("shire.providerid"))) {

                        User user = userLookup.getUserByUserId(token.getUserId());

                        if (user != null && user.isFoundUser()) {
                            user.setOAuthUser(true);

                            // Ensure the user is logged in
                            user.setIsLoggedIn(true);

                            // Set it to the SSO Client Filter's parameter
                            response.setUser(user);
                            response.setContinueRequest(true);
                            return response;
                        }
                    }

                    // We were given a message and a token but have failed to find a
                    // valid user. This is usually because the token is expired (or we
                    // purged the tokens from the db)
                    if (expiredToken401) {
                        // Send a hint to the consumer that they should be OAuthing
                        response.setHeaders(Arrays.asList(
                           (Header)new BasicHeader("WWW-Authenticate", "OAuth realm=\"" + getConfig().getString("shire.providerid") + "\""))
                        );

                        response.setStatusCode(401 /*SC_UNAUTHORIZED*/);
                    }
                }
            } catch (OAuthProblemException e) {
                try {
                    handleException(response, e, getConfig().getString("shire.providerid"));
                } catch (IOException e1) {
                    LOGGER.error("Couldn't retrieve user from OAuth token", e);
                    response.setError(e);
                }
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOGGER.error("Couldn't retrieve user from OAuth token", e);
                response.setError(e);
            }
        }
        return response;
    }

    private void handleException(Response response, OAuthProblemException problem, String realm) throws IOException {
        Object httpCode = problem.getParameters().get(HttpMessage.STATUS_CODE);
        if (httpCode == null) {
            httpCode = OAuth.Problems.TO_HTTP_CODE.get(problem.getProblem());
        }
        if (httpCode == null) {
            httpCode = 403 /*SC_FORBIDDEN*/;
        }

        response.setStatusCode(Integer.parseInt(httpCode.toString()));
        OAuthMessage message = new OAuthMessage(null, null, problem
                .getParameters().entrySet());

        response.setHeaders(Arrays.<Header>asList(
                new BasicHeader("WWW-Authenticate", message.getAuthorizationHeader(realm)),
                new BasicHeader("Content-Type", OAuth.FORM_ENCODED + ";charset=" + OAuth.ENCODING)
        ));

        OAuth.formEncode(message.getParameters(), response.getOutputStream());
    }

    public static boolean isCorrectScope(OAuthToken token, String expectedScope) throws OAuthProblemException {
        for (String scope: token.getService().split("\\+")) {
            if (scope.equalsIgnoreCase(expectedScope)) {
                return true;
            }
        }

        OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_REJECTED);
        e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_REJECTED, "scope");
        throw e;
    }

    private OAuthMessage getMessage(HttpRequest request, String requestedUrl) {
        return new CoreRequestMessage(request, requestedUrl);
    }

    public SSOConfiguration getConfig() {
        return config;
    }

    public OAuthService getOAuthService() {
        return service;
    }

    public boolean isExpiredToken401() {
        return expiredToken401;
    }

    public void setExpiredToken401(boolean expiredToken401) {
        this.expiredToken401 = expiredToken401;
    }

    /** Wrap request as an OAuthMessage.
     * Mostly copied from HttpRequestMessage, the Servlet equivalent.
     */
    static class CoreRequestMessage extends OAuthMessage {
        public CoreRequestMessage(HttpRequest request, String requestedUrl) {
            super(request.getMethod(), requestedUrl, getParams(request));
        }

        private static List<OAuth.Parameter> getParams(HttpRequest request) {
            List<OAuth.Parameter> list = new ArrayList<>();
            for (String header : request.getHeaders("Authorization")) {
                for (OAuth.Parameter parameter : OAuthMessage.decodeAuthorization(header)) {
                    if (!"realm".equalsIgnoreCase(parameter.getKey())) {
                        list.add(parameter);
                    }
                }
            }
            for (String name : request.getParameterNames()) {
                for (String value : request.getParameter(name)) {
                    list.add(new OAuth.Parameter(name, value));
                }
            }
            return list;
        }
    }
}
