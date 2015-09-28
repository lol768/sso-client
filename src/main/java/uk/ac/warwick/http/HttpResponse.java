package uk.ac.warwick.http;


import org.apache.http.cookie.Cookie;

public interface HttpResponse {
    void setHeader(String s, String authHeader);

    void setStatus(int statusCode);

    void addCookie(Cookie cookie);
}
