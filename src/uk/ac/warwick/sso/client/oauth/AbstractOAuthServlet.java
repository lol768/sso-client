package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.warwick.sso.client.SSOConfigLoader;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.oauth.OAuthToken.Type;
import uk.ac.warwick.sso.client.tags.SSOLinkGenerator;
import uk.ac.warwick.userlookup.User;

@SuppressWarnings("serial")
/**
 * @deprecated See SSO-840
 */
public abstract class AbstractOAuthServlet extends HttpServlet {
    
    public static final String OAUTH_SESSION_HANDLE = "oauth_session_handle";
    
    public static final String OAUTH_EXPIRES_IN = "oauth_expires_in";
    
    public static final String OAUTH_BODY_HASH = "oauth_body_hash";
    
    public static final String OAUTH_VERIFIER = "oauth_verifier";
    
    public static final String OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";
    
    public static final String PROBLEM_ACCESS_TOKEN_EXPIRED = "access_token_expired";

    public static final String PROBLEM_PARAMETER_MISSING = "parameter_missing";

    public static final String PROBLEM_TOKEN_REVOKED = "token_revoked";

    public static final String PROBLEM_TOKEN_INVALID = "token_invalid";

    public static final String PROBLEM_PARAMETER_ABSENT = "parameter_absent";

    public static final String PROBLEM_BAD_VERIFIER = "bad_verifier";

    public static final String PROBLEM_TOKEN_REJECTED = "token_rejected";

    public static final String PROBLEM_PARAMETER_REJECTED = "parameter_rejected";

    public static final String PROBLEM_PERMISSION_DENIED = "permission_denied";

    public static final String PROBLEM_CONSUMER_KEY_REFUSED = "consumer_key_refused";

    public static final String PROBLEM_CONSUMER_KEY_UNKNOWN = "consumer_key_unknown";
    
    public static final SecureRandom rand = new SecureRandom();

    private final static char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
    
    // This needs to be long enough that an attacker can't guess it. If the
    // attacker can guess this value before they exceed the maximum number of
    // attempts, they can complete a session fixation attack against a user.
    private static final int CALLBACK_TOKEN_LENGTH = 6;
    
    private static final Log LOGGER = LogFactory.getLog(AbstractOAuthServlet.class);
    
    private OAuthValidator _validator = new SimpleOAuthValidator();

    private SSOConfiguration _config;
    
    private OAuthService _service;

    private String _configSuffix = "";
    
    protected final void sendResponse(HttpServletResponse servletResponse, List<OAuth.Parameter> parameters) throws IOException {
        servletResponse.setContentType("text/plain");
        OutputStream out = servletResponse.getOutputStream();
        OAuth.formEncode(parameters, out);
        out.close();
    }
    
    protected void validateMessage(OAuthMessage message, OAuthAccessor accessor) throws OAuthException, IOException, URISyntaxException {
        getValidator().validateMessage(message, accessor);
    }
    
    protected final String getRealm(HttpServletRequest request) throws ServletException {
        SSOLinkGenerator generator = new SSOLinkGenerator();
        generator.setConfig(getConfig());
        generator.setRequest(request);
        
        String requestedUrl = generator.getTarget();
        
        try {
            URL url = new URL(requestedUrl);
            String realm = url.getProtocol() + "://" + url.getHost();
            
            return realm;
        } catch (MalformedURLException e) {
            throw new ServletException(e);
        }
    }
    
    protected OAuthConsumer getConsumer(String consumerKey) throws OAuthProblemException {
        try {
            return getOAuthService().getConsumerByConsumerKey(consumerKey).get();
        } catch (Exception e) {
            LOGGER.error("Couldn't get consumer from OAuth service", e);
            return null;
        }
    }
    
    protected OAuthToken generateRequestToken(String consumerKey, String version, String signedCallbackUrl) throws OAuthProblemException {
        OAuthToken token = new OAuthToken();
        token.setConsumerKey(consumerKey);
        token.setOAuthVersion(version);
        token.setService(getConfig().getString("shire.providerid"));
        token.setType(OAuthToken.Type.REQUEST);
        token.setIssueTime(new Date());

        if (signedCallbackUrl != null) {
            token.setCallbackUrlSigned(true);
            token.setCallbackUrl(signedCallbackUrl);
        }
        
        try {
            return getOAuthService().generateRequestToken(token).get();
        } catch (Exception e) {
            LOGGER.error("Couldn't generate/store request token from OAuth service", e);
            throw new OAuthProblemException("Couldn't generate/store request token from OAuth service");
        }
    }
    
    protected OAuthToken getToken(String token) throws OAuthProblemException {
        try {
            return getOAuthService().getToken(token).get();
        } catch (Exception e) {
            LOGGER.error("Couldn't get token OAuth service", e);
            throw new OAuthProblemException("Couldn't get token from OAuth service");
        }
    }
    
    protected OAuthToken convertToAccessToken(OAuthToken token) throws OAuthProblemException {
        if (token.getType() != Type.REQUEST) {
            throw new OAuthProblemException("Must be a request token!");
        }
        
        token.setToken(UUID.randomUUID().toString());
        token.setTokenSecret(UUID.randomUUID().toString());

        token.setType(Type.ACCESS);
        token.setIssueTime(new Date());
        
        try {
            return getOAuthService().store(token).get();
        } catch (Exception e) {
            LOGGER.error("Couldn't convert to access token", e);
            throw new OAuthProblemException("Couldn't convert to access token");
        }
    }
    
    protected void disableToken(OAuthToken token) throws OAuthProblemException {
        token.setType(Type.DISABLED);
        
        getOAuthService().store(token);
    }
    
    protected void doAuthoriseToken(OAuthToken token, User user) throws OAuthProblemException {
        token.setAuthorised(true);
        token.setUserId(user.getUserId());
        
        if (token.isCallbackUrlSigned()) {
            token.setCallbackToken(getRandomDigits(CALLBACK_TOKEN_LENGTH));
        }
        
        getOAuthService().store(token);
    }
    
    protected void doDenyToken(OAuthToken token) throws OAuthProblemException {
        // defer
        disableToken(token);
    }
    
    public void init(final ServletConfig ctx) throws ServletException {
        super.init(ctx);
        
        if (ctx.getInitParameter("configsuffix") != null) {
            _configSuffix = ctx.getInitParameter("configsuffix");
        }

        if (getConfig() == null) {
            _config = (SSOConfiguration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);
        }
        
        if (getOAuthService() == null) {
            _service = new OAuthServiceImpl(getConfig());
        }
    }

    public final String getConfigSuffix() {
        return _configSuffix;
    }

    public final void setConfigSuffix(final String configSuffix) {
        _configSuffix = configSuffix;
    }

    public final SSOConfiguration getConfig() {
        return _config;
    }

    public final void setConfig(final SSOConfiguration config) {
        _config = config;
    }

    public final OAuthValidator getValidator() {
        return _validator;
    }

    public final void setValidator(OAuthValidator validator) {
        _validator = validator;
    }

    public final OAuthService getOAuthService() {
        return _service;
    }

    public final void setOAuthService(OAuthService service) {
        _service = service;
    }
    
    private static String getRandomDigits(int len) {
        byte[] random = getRandomBytes(len);
        StringBuilder out = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            out.append(DIGITS[Math.abs(random[i] % DIGITS.length)]);
        }
        return out.toString();
    }

    private static byte[] getRandomBytes(int numBytes) {
        byte[] out = new byte[numBytes];
        rand.nextBytes(out);
        return out;
    }

}
