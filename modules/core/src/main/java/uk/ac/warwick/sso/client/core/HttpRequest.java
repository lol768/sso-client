package uk.ac.warwick.sso.client.core;

import java.util.List;
import java.util.Set;

/**
 * INTERNAL INTERFACE.
 *
 * Only to be used by framework adapters to link in to Servlet or whatever.
 * No API should expose HttpRequest as a type.
 *
 * Agnostic interface to HTTP request, which internal
 * classes can use to implement logic without being
 * tied to a particular framework like Servlet API.
 *
 * Unless otherwise documented, these are equivalent to their Servlet
 * counterparts.
 *
 * We try only to use the read-only parts of Request here,
 * and put things into Response
 */
public interface HttpRequest {
    List<String> getParameter(String name);

    Set<String> getParameterNames();

    String getHeader(String s);
    List<String> getHeaders(String s);

    String getRemoteAddr();

    /** Everything but query params, e.g. http://localhost:8080/xyz/abc */
    String getRequestURL();

    /** Just the path, no query params, e.g. /xyz/abc */
    String getRequestURI();

    String getQueryString();

    List<String> getQueryParameter(String name);

    String getMethod();

    List<Cookie> getCookies();

    /**
     * On request types that support storing arbitrary attributes,
     * this will return an object from the attribute map if it exists.
     * Not all requests do, though, so those will always return null.
     */
    Object getAttribute(String key);
}
