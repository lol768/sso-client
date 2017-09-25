package uk.ac.warwick.sso.client;

import org.apache.http.Header;
import uk.ac.warwick.sso.client.core.*;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.util.core.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class HandleFilter {

    public abstract SSOConfiguration getConfig();

    public abstract SSOHandler getHandler();

    private static final String USER_KEY = SSOClientHandlerImpl.USER_KEY;

    private static final String ACTUAL_USER_KEY = SSOClientHandlerImpl.ACTUAL_USER_KEY;

    public void filterWithHandler(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        final HeaderSettingHttpServletRequest request = new HeaderSettingHttpServletRequest(servletRequest);

        HttpRequest req = new ServletRequestAdapter(request);
        Response res = getHandler().handle(req);

        for (Cookie c : res.getCookies()) {
            response.addCookie(ServletCookies.toServlet(c));
        }

        for (Header header : res.getHeaders()) {
            response.setHeader(header.getName(), header.getValue());
        }

        if (res.getUser() != null) {
            putUserIntoKey(res.getUser(), request, getUserKey(getConfig()));
        }

        if (res.getActualUser() != null) {
            putUserIntoKey(res.getActualUser(), request, getActualUserKey(getConfig()));
        }

        if (res.isContinueRequest()) {
            filterChain.doFilter(request, response);
        } else {
            if (res.getRedirect() != null)
                response.sendRedirect(res.getRedirect());
            else
                response.setStatus(res.getStatusCode());
        }
        addAdditionalCookieAttributes(response);
    }

    private void putUserIntoKey(final User user, final HeaderSettingHttpServletRequest request, final String userKey) {
        request.setAttribute(userKey, user);
        request.setRemoteUser(user.getUserId());

        if (!user.getExtraProperties().isEmpty()) {
            for (Map.Entry<String, String> entry : user.getExtraProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                request.setAttribute(userKey + "_" + key, value);
                request.addHeader(userKey + "_" + key, value);
            }
        }

        // Handle mismatch in attributes between old/new mode (user/cn)
        request.setAttribute(userKey + "_usercode", user.getUserId());
        request.addHeader(userKey + "_usercode", user.getUserId());

        request.addHeader(userKey + "_groups", "");
    }

    private static String getUserKey(SSOConfiguration config) {
        return config.getString("shire.filteruserkey", USER_KEY);
    }

    private static String getActualUserKey(SSOConfiguration config) {
        return config.getString("shire.filteractualuserkey", ACTUAL_USER_KEY);
    }

    protected void addAdditionalCookieAttributes(HttpServletResponse response) {
        String originalSetCookieString = response.getHeader("Set-Cookie");
        if (!StringUtils.hasText(originalSetCookieString)) return;
        String sameSiteConfig = getConfig().getString("shire.sscookie.samesite", "Lax");
        if (sameSiteConfig.equalsIgnoreCase("none")) return;
        String sameSiteSetting = getProperSameSiteValue(sameSiteConfig);
        String newSetCookieString = getSameSiteStrictCookieForSSC(originalSetCookieString, getConfig().getString("shire.sscookie.name"), sameSiteSetting);
        response.setHeader("Set-Cookie", newSetCookieString);
    }

    public static String getSameSiteStrictCookieForSSC(String cookie, String ssc, String setting) {
        if (!cookie.contains(ssc)) return cookie;
        return Arrays.stream(cookie.split(",")).map(e -> e.trim().startsWith(ssc) ? e + "; SameSite=" + setting : e).collect(Collectors.joining(","));
    }

    public static String getProperSameSiteValue(String s) {
        if (StringUtils.hasText(s)) {
            switch (s.toLowerCase()) {
                case "lax":
                    return "Lax";
                case "strict":
                    return "Strict";
            }
        }
        return "Lax";
    }
}
