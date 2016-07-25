package uk.ac.warwick.sso.client.core;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import uk.ac.warwick.userlookup.User;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * In SSOClientFilter we would pass around the Response object and fill it in
 * with properties as we went, and then either continue the request or not.
 * This doesn't fit well with other frameworks, which don't always have a mutable
 * response object that we can wrap around.
 * <p>
 * This Response object is intended as a return type for the SSOClientHandler,
 * containing all the cookies, headers and other response details that an adapter
 * should need to turn into a framework-native response.
 */
public class Response {
    private List<Cookie> cookies = new ArrayList<>();
    private List<Header> headers = new ArrayList<>();

    /**
     * If true, we allow the original request to continue (e.g. authorized) -
     * akin to calling the Servlet filter chain.
     * If false, we do not do that. The response is just the
     */
    private boolean continueRequest;

    private int statusCode = 200;
    private String redirect;
    private User user;
    private User actualUser;
    private Exception error;

    private OutputStream outputStream = new ByteArrayOutputStream();

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public void setHeader(String name, String value) {
        removeHeadersWithNameIgnoreCase(name);

        this.headers.add(new BasicHeader(name, value));
    }

    private void removeHeadersWithNameIgnoreCase(String name) {
        Iterator<Header> iterator = headers.iterator();

        while (iterator.hasNext()) {
            Header header = iterator.next();

            if (header.getName().equalsIgnoreCase(name)) {
                iterator.remove();
            }
        }
    }

    public boolean isContinueRequest() {
        return continueRequest;
    }

    public void setContinueRequest(boolean continueRequest) {
        this.continueRequest = continueRequest;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Return the given status code.
     * Sets continueRequest to false, as it's implied we're returning an
     * alternate response.
     */
    public void setStatusCode(int statusCode) {
        setContinueRequest(false);
        this.statusCode = statusCode;
    }

    public String getRedirect() {
        return redirect;
    }

    /**
     * Sets a URL to redirect to.
     * Implicitly sets continueRequest to false, since we're redirecting.
     */
    public void setRedirect(String redirect) {
        setContinueRequest(false);
        this.redirect = redirect;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setActualUser(User actualUser) {
        this.actualUser = actualUser;
    }

    public User getActualUser() {
        return actualUser;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * An output stream to which you can write some response body.
     * Don't write too much into here because it's all in memory.
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }
}
