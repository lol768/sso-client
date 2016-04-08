package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

abstract class AbstractShireSkippingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractShireSkippingFilter.class);

    public final void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Configuration config = SSOConfiguration.getConfig();

        String shireLocation = config.getString("shire.location");
        String logoutLocation = config.getString("logout.location");

        URL target = getTarget(request);
        LOGGER.debug("Target=" + target);

        if (target != null && (target.toExternalForm().equals(shireLocation) || target.toExternalForm().equals(logoutLocation))) {
            LOGGER.debug("Letting request through without filtering because it is a shire or logout request");
            chain.doFilter(req, res);
            return;
        }

        doFilterInternal(request, response, chain);
    }

    abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;

    private URL getTarget(final HttpServletRequest request) {
        SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
        generator.setRequest(request);
        try {
            return new URL(generator.getTarget());
        } catch (MalformedURLException e) {
            LOGGER.warn("Target is an invalid url", e);
            return null;
        }
    }

}
