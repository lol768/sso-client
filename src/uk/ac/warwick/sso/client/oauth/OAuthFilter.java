package uk.ac.warwick.sso.client.oauth;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.sso.client.SSOConfigLoader;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.oauth.OAuthToken.Type;
import uk.ac.warwick.sso.client.tags.SSOLinkGenerator;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupFactory;
import uk.ac.warwick.userlookup.UserLookupInterface;

public final class OAuthFilter implements Filter {
    
    private static final Logger LOGGER = Logger.getLogger(OAuthFilter.class);

    private String _configSuffix = "";

    private Configuration _config;
    
    private OAuthService _service;

    private UserLookupInterface _userLookup;
    
    private boolean _expiredToken401 = true;

    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) arg0;
        HttpServletResponse response = (HttpServletResponse) arg1;
        
        User user = SSOClientFilter.getUserFromRequest(request);
        if (user.isFoundUser()) {
            // We already have a perfectly fine user here.
            chain.doFilter(request, response);
            return;
        }
        
        SSOLinkGenerator generator = new SSOLinkGenerator();
        generator.setConfig(getConfig());
        generator.setRequest(request);
        
        String requestedUrl = generator.getTarget();
        
        OAuthMessage message = OAuthServlet.getMessage(request, requestedUrl);
        
        if (message != null && message.getToken() != null) {
            try {
                OAuthToken token = getOAuthService().getToken(message.getToken()).get();
                
                if (token != null 
                 && token.isAuthorised() 
                 && !token.isExpired() 
                 && token.getType() == Type.ACCESS 
                 && token.getConsumerKey().equals(message.getConsumerKey())
                 && isCorrectScope(token, getConfig().getString("shire.providerid"))) {
                    user = getUserLookup().getUserByUserId(token.getUserId());
                    
                    if (user != null && user.isFoundUser()) {
                        // Set it to the SSO Client Filter's parameter
                        request.setAttribute(SSOClientFilter.getUserKey(), user);
                        
                        chain.doFilter(request, response);
                        return;
                    }
                }
            } catch (OAuthProblemException e) {
                OAuthServlet.handleException(response, e, getConfig().getString("shire.providerid"));
            } catch (ExecutionException e) {
                LOGGER.error("Couldn't retrieve user from OAuth token", e);
            } catch (InterruptedException e) {
                LOGGER.error("Couldn't retrieve user from OAuth token", e);
            }
            
            // We were given a message and a token but have failed to find a
            // valid user. This is usually because the token is expired (or we
            // purged the tokens from the db)
            if (_expiredToken401) {
                // Send a hint to the consumer that they should be OAuthing
                response.addHeader("WWW-Authenticate", "OAuth realm=\"" + getConfig().getString("shire.providerid") + "\"");
                
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }

    private static boolean isCorrectScope(OAuthToken token, String expectedScope) throws OAuthProblemException {
        for (String scope : token.getService().split("\\+")) {
            if (scope.equalsIgnoreCase(expectedScope)) {
                return true;
            }
        }
        
        OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_REJECTED);
        e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_REJECTED, "scope");
        throw e;
    }

    public void init(FilterConfig ctx) throws ServletException {
        if (ctx.getInitParameter("configsuffix") != null) {
            _configSuffix = ctx.getInitParameter("configsuffix");
        }

        // config is already loaded, probably through spring injection
        if (_config == null) {
            ServletContext servletContext = ctx.getServletContext();
            _config = (Configuration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);

            if (_config == null) {
                // try to load the sso config for instances where the Listener cannot be used (e.g. JRun)
                LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
                        + _configSuffix + "; attempting to load sso config");
                SSOConfigLoader loader = new SSOConfigLoader();
                loader.loadSSOConfig(servletContext);
                _config = (Configuration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);
            }

            if (_config == null) {
                LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
                        + _configSuffix);
            } else {
                LOGGER.info("Found sso config");
            }
        }
        
        if (_service == null && _config != null) {
            _service = new OAuthServiceImpl(_config);
        }
    }

    public void destroy() {
        // nothing to do
    }

    public Configuration getConfig() {
        if (_config == null) {
            _config = SSOConfiguration.getConfig();
        }
        return _config;
    }

    public void setConfig(final Configuration config) {
        _config = config;
    }

    public String getConfigSuffix() {
        return _configSuffix;
    }

    public void setConfigSuffix(final String configSuffix) {
        _configSuffix = configSuffix;
    }

    public OAuthService getOAuthService() {
        if (_service == null) {
            _service = new OAuthServiceImpl(getConfig());
        }
        return _service;
    }

    public void setOAuthService(OAuthService service) {
        _service = service;
    }
    
    public UserLookupInterface getUserLookup() {
        if (_userLookup == null) {
            _userLookup = UserLookupFactory.getInstance();
        }
        return _userLookup;
    }

    public void setUserLookup(final UserLookupInterface userLookup) {
        _userLookup = userLookup;
    }
    
    public void setExpiredToken401(final boolean expiredToken401) {
        _expiredToken401 = expiredToken401;
    }
}
