package uk.ac.warwick.sso.client.trusted;

import org.apache.log4j.Logger;
import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.sso.client.SSOConfigLoader;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.oauth.OAuthServiceImpl;
import uk.ac.warwick.sso.client.tags.SSOLinkGenerator;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupFactory;
import uk.ac.warwick.userlookup.UserLookupInterface;
import uk.ac.warwick.util.core.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TrustedApplicationFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(TrustedApplicationFilter.class);

    private String configSuffix = "";

    private SSOConfiguration config;

    private UserLookupInterface userLookup;

    private TrustedApplicationsManager appManager;

    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) arg0;
        HttpServletResponse response = (HttpServletResponse) arg1;

        if (!StringUtils.hasText(request.getHeader(TrustedApplication.HEADER_CERTIFICATE)) || SSOClientFilter.getUserFromRequest(request).isFoundUser()) {
            // Either not a trusted apps request, or we already have a perfectly fine user here.
            chain.doFilter(request, response);
        } else {
            try {
                User user = parseTrustedApplicationsRequest(request);

                // Set it to the SSO Client Filter's parameter
                request.setAttribute(SSOClientFilter.getUserKey(), user);
                response.setHeader(TrustedApplication.HEADER_STATUS, TrustedApplication.Status.OK.name());

                chain.doFilter(request, response);
            } catch (TransportException e) {
                response.setHeader(TrustedApplication.HEADER_STATUS, TrustedApplication.Status.Error.name());
                response.setHeader(TrustedApplication.HEADER_ERROR, e.getTransportErrorMessage().getFormattedMessage());
            }
        }
    }

    private User parseTrustedApplicationsRequest(HttpServletRequest request) throws TransportException {
        String requestedUrl = getRequestedUrl(request);

        String certStr = request.getHeader(TrustedApplication.HEADER_CERTIFICATE);

        String providerId = request.getHeader(TrustedApplication.HEADER_PROVIDER_ID);
        if (!StringUtils.hasText(providerId)) {
            throw new FilterException(new TransportErrorMessage.ProviderIdNotFoundInRequest());
        }

        TrustedApplication app = appManager.getTrustedApplication(providerId);
        if (app == null) {
            throw new FilterException(new TransportErrorMessage.ApplicationUnknown(providerId));
        }

        // This will throw an InvalidCertificateException (which is a transport exception)
        ApplicationCertificate certificate =
            app.decode(new EncryptedCertificateImpl(providerId, certStr), request);

        String signature = request.getHeader(TrustedApplication.HEADER_CERTIFICATE);

        if (!StringUtils.hasText(signature)) {
            throw new FilterException(new TransportErrorMessage.BadSignature());
        }

        try {
            if (!app.verifySignature(certificate.getCreationTime(), requestedUrl, certificate.getUsername(), signature)) {
                throw new FilterException(new TransportErrorMessage.BadSignature(requestedUrl));
            }
        } catch (SignatureVerificationFailedException e) {
            throw new FilterException(new TransportErrorMessage.BadSignature(requestedUrl));
        }

        User user = getUserLookup().getUserByUserId(certificate.getUsername());
        if (user != null && user.isFoundUser() && !user.isLoginDisabled()) {
            user.setTrustedApplicationsUser(true);

            // Ensure the user is logged in
            user.setIsLoggedIn(true);
        } else if (user != null && user.isLoginDisabled()) {
            throw new FilterException(new TransportErrorMessage.PermissionDenied());
        } else {
            throw new FilterException(new TransportErrorMessage.UserUnknown(certificate.getUsername()));
        }

        return user;
    }

    private String getRequestedUrl(HttpServletRequest request) {
        SSOLinkGenerator generator = new SSOLinkGenerator();
        generator.setConfig(getConfig());
        generator.setRequest(request);
        return generator.getTarget();
    }

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

        if (appManager == null && config != null) {
            try {
                appManager = new SSOConfigTrustedApplicationsManager(config);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    public SSOConfiguration getConfig() {
        if (config == null) {
            config = SSOConfiguration.getConfig();
        }
        return config;
    }

    public void setConfig(final SSOConfiguration config) {
        this.config = config;
    }

    public String getConfigSuffix() {
        return configSuffix;
    }

    /**
     * Set the configuration suffix for the sso-config.xml file. This should be
     * the same value as set for {@link SSOClientFilter}
     */
    public void setConfigSuffix(final String configSuffix) {
        this.configSuffix = configSuffix;
    }

    public UserLookupInterface getUserLookup() {
        if (userLookup == null) {
            userLookup = UserLookupFactory.getInstance();
        }
        return userLookup;
    }

    public void setUserLookup(final UserLookupInterface userLookup) {
        this.userLookup = userLookup;
    }

    private static class FilterException extends TransportException {
        public FilterException(TransportErrorMessage error)
        {
            super(error);
        }

        public String getMessage() {
            Throwable cause = getCause();
            if (cause != null) {
                return cause.getMessage();
            }
            return super.getMessage();
        }
    }
}
