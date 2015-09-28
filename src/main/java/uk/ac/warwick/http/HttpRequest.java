package uk.ac.warwick.http;

import com.sun.org.apache.bcel.internal.generic.FieldGen;

import java.net.URL;
import java.util.List;

/**
 * Agnostic interface to HTTP request, which internal
 * classes can use to implement logic without being
 * tied to a particular framework like Servlet API.
 */
public interface HttpRequest {
    List<String> getParameter(String name);

    String getHeader(String s);
    List<String> getHeaders(String s);

    String getRemoteHost();

    URL getRequestURL();
    String getRequestURI();
}
