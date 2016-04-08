package uk.ac.warwick.sso.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.userlookup.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CSRFFilter extends AbstractShireSkippingFilter {

    public static final String CSRF_HTTP_HEADER = "X-CSRF-Token";

    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFFilter.class);

    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getMethod().equalsIgnoreCase("post")) {
            User user = SSOClientFilter.getUserFromRequest(request);

            if (user != null && user.isFoundUser() && user.isLoggedIn()) {
                String csrfToken = (String) user.getExtraProperty(ShireCommand.CSRF_TOKEN_PROPERTY_NAME);

                if (csrfToken == null) {
                    LOGGER.warn("User has no CSRF token!");
                } else {
                    // Allow the token to be provided either as a POST param, or in an HTTP header
                    final String providedToken;
                    if (request.getParameterMap().containsKey(ShireCommand.CSRF_TOKEN_PROPERTY_NAME)) {
                        providedToken = request.getParameter(ShireCommand.CSRF_TOKEN_PROPERTY_NAME);
                    } else {
                        providedToken = request.getHeader(CSRF_HTTP_HEADER);
                    }

                    if (providedToken == null || providedToken.length() == 0) {
                        LOGGER.info("No CSRF token was provided in the POST; denying request");

                        response.setHeader("X-Error", "No CSRF token");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else if (!providedToken.equals(csrfToken)) {
                        LOGGER.warn("Provided CSRF token does not match stored CSRF token; denying request");

                        response.setHeader("X-Error", "Wrong CSRF token");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        LOGGER.debug("Allowing CSRF request through as token matches");
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing
    }

    public void destroy() {
        // nothing
    }
}
