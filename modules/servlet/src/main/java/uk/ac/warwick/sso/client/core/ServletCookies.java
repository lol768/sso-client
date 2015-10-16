package uk.ac.warwick.sso.client.core;

/**
 * Conversions between internal Cookie objects and the Servlet equivalents.
 */
public abstract class ServletCookies {
    public static javax.servlet.http.Cookie toServlet(Cookie c) {
        if (c == null) return null;
        javax.servlet.http.Cookie c2 = new javax.servlet.http.Cookie(c.getName(), c.getValue());
        c2.setComment(c.getComment());
        c2.setDomain(c.getDomain());
        if (c.isDelete()) {
            c2.setMaxAge(0);
        } else {
            c2.setMaxAge(c.getMaxAge());
        }
        c2.setPath(c.getPath());
        c2.setSecure(c.isSecure());
        c2.setHttpOnly(c.isHttpOnly());
        c2.setVersion(c.getVersion());
        return c2;
    }

    public static Cookie fromServlet(javax.servlet.http.Cookie c) {
        if (c == null) return null;
        Cookie c2 = new Cookie(c.getName(), c.getValue());
        c2.setComment(c.getComment());
        c2.setDomain(c.getDomain());
        c2.setMaxAge(c.getMaxAge());
        if (c.getMaxAge() == 0) {
            c2.setDelete(true);
        }
        c2.setPath(c.getPath());
        c2.setSecure(c.getSecure());
        c2.setHttpOnly(c.isHttpOnly());
        c2.setVersion(c.getVersion());
        return c2;
    }
}
