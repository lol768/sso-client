package uk.ac.warwick.sso.client.core;

import uk.ac.warwick.util.web.Uri;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Wraps an HttpServletRequest to implement the HttpRequest interface.
 */
public class ServletRequestAdapter implements HttpRequest {
    private final HttpServletRequest req;

    private Map<String, List<String>> parsedQuery;

    public ServletRequestAdapter(HttpServletRequest req) {
        this.req = req;
    }

    @Override
    public List<String> getParameter(String name) {
        if (req.getParameterMap().containsKey(name)) {
            return Arrays.asList(req.getParameterValues(name));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getParameterNames() {
        return new TreeSet<>(Collections.list(req.getParameterNames()));
    }

    @Override
    public String getHeader(String s) {
        return req.getHeader(s);
    }

    @Override
    public List<String> getHeaders(String s) {
        return Collections.list(req.getHeaders(s));
    }

    @Override
    public String getRemoteAddr() {
        return req.getRemoteAddr();
    }

    @Override
    public String getRequestURL() {
        return req.getRequestURL().toString();
    }

    @Override
    public String getRequestURI() {
        return req.getRequestURI();
    }

    @Override
    public String getQueryString() {
        return req.getQueryString();
    }

    @Override
    public List<String> getQueryParameter(String name) {
        if (parsedQuery == null) {
            if (getQueryString() != null) {
                parsedQuery = Uri.parse("?" + getQueryString()).getQueryParameters();
            } else {
                parsedQuery = new HashMap<>();
            }
        }
        if (parsedQuery.containsKey(name)) {
            return parsedQuery.get(name);
        }
        return Collections.emptyList();
    }

    @Override
    public String getMethod() {
        return req.getMethod();
    }

    @Override
    public List<Cookie> getCookies() {
        List<Cookie> cookies = new ArrayList<>();
        javax.servlet.http.Cookie[] oldCookies = req.getCookies();
        if (oldCookies != null) {
            for (javax.servlet.http.Cookie old : oldCookies) {
                cookies.add(ServletCookies.fromServlet(old));
            }
        }
        return cookies;
    }

    @Override
    public Object getAttribute(String key) {
        return req.getAttribute(key);
    }
}
