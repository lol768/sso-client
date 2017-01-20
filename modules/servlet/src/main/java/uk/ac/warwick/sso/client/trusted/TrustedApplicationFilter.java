package uk.ac.warwick.sso.client.trusted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.*;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.UserLookupInterface;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TrustedApplicationFilter extends HandleFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedApplicationFilter.class);

    @Inject
    private TrustedApplicationHandler handler;

    private SSOConfiguration config;

    private String configSuffix = "";
    private String configLocation;
    private TrustedApplicationsManager trustedApplicationsManager;

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public void setConfigSuffix(String configSuffix) {
        this.configSuffix = configSuffix;
    }

    @Override
    public void init(FilterConfig ctx) throws ServletException {
        if (ctx.getInitParameter("configsuffix") != null) {
            configSuffix = ctx.getInitParameter("configsuffix");
        }

        // config is already loaded, probably through spring injection
        if (config == null) {
            ServletContext servletContext = ctx.getServletContext();
            config = (SSOConfiguration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + configSuffix);

            if (config == null) {
                // try to load the sso config for instances where the Listener
                // cannot be used (e.g. JRun)
                LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
                        + configSuffix + "; attempting to load sso config");
                SSOConfigLoader loader = new SSOConfigLoader();
                loader.loadSSOConfig(servletContext);
                config = (SSOConfiguration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + configSuffix);
            }

            if (config == null) {
                LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
                        + configSuffix);
            } else {
                LOGGER.info("Found sso config");
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (SSOClientFilter.getUserFromRequest((HttpServletRequest) servletRequest).isFoundUser()) {
            // Already have a perfectly fine user here
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            filterWithHandler((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
        }
    }

    public void destroy() {
    }

    @Override
    public SSOConfiguration getConfig() {
        return config;
    }

    @Override
    public SSOHandler getHandler() {
        if (handler == null) {
            handler = new TrustedApplicationHandlerImpl(
                    getUserLookup(),
                    getTrustedApplicationsManager(),
                    getConfig()
            );
        }

        return handler;
    }

    public void setHandler(TrustedApplicationHandler handler) {
        this.handler = handler;
    }

    public void setConfig(SSOConfiguration config) {
        this.config = config;
    }

    private UserLookupInterface userLookup;

    public UserLookupInterface getUserLookup() {
        if (userLookup == null) {
            userLookup = UserLookup.getInstance();
        }
        return userLookup;
    }

    public void setUserLookup(UserLookupInterface userLookup) {
        this.userLookup = userLookup;
        if (handler != null) {
            handler.setUserLookup(userLookup);
        }
    }

    public TrustedApplicationsManager getTrustedApplicationsManager() {
        if (trustedApplicationsManager == null) {
            try {
                trustedApplicationsManager = new SSOConfigTrustedApplicationsManager(getConfig());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return trustedApplicationsManager;
    }

    public void setTrustedApplicationsManager(TrustedApplicationsManager trustedApplicationsManager) {
        this.trustedApplicationsManager = trustedApplicationsManager;
    }
}
