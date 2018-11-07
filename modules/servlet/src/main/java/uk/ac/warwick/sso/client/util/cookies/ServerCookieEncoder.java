package uk.ac.warwick.sso.client.util.cookies;

/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import uk.ac.warwick.sso.client.core.Cookie;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import static uk.ac.warwick.sso.client.util.cookies.CookieUtil.*;

/**
 * A <a href="https://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie encoder to be used server side,
 * so some fields are sent (Version is typically ignored).
 *
 * As Netty's Cookie merges Expires and MaxAge into one single field, only Max-Age field is sent.
 *
 * Note that multiple cookies are supposed to be sent at once in a single "Set-Cookie" header.
 *
 */
public final class ServerCookieEncoder extends CookieEncoder {

    public ServerCookieEncoder(boolean strict) {
        super(strict);
    }

    /**
     * Encodes the specified cookie name-value pair into a Set-Cookie header value.
     *
     * @param name the cookie name
     * @param value the cookie value
     * @return a single Set-Cookie header value
     */
    public String encode(String name, String value) {
        return encode(new Cookie(name, value));
    }

    /**
     * Encodes the specified cookie into a Set-Cookie header value.
     *
     * @param cookie the cookie
     * @return a single Set-Cookie header value
     */
    public String encode(Cookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie");
        }
        final String name = cookie.getName();
        final String value = cookie.getValue() != null ? cookie.getValue() : "";

        validateCookie(name, value);

        StringBuilder buf = new StringBuilder();


        add(buf, name, value);

        if (cookie.getMaxAge() >= 0) {
            add(buf, CookieHeaderNames.MAX_AGE, cookie.getMaxAge());
            Date expires = cookie.getMaxAge() <= 0
                    ? new Date(0) // Set expires to the Unix epoch
                    : new Date(cookie.getMaxAge() * 1000L + System.currentTimeMillis());
            add(buf, CookieHeaderNames.EXPIRES, HttpHeaderDateFormat.get().format(expires));
        }

        if (cookie.getSameSite() != null) {
            add(buf, CookieHeaderNames.SAMESITE, cookie.getSameSite() == Cookie.SameSiteValue.LAX ? "Lax" : "Strict");
        }

        if (cookie.getPath() != null) {
            add(buf, CookieHeaderNames.PATH, cookie.getPath());
        }

        if (cookie.getDomain() != null) {
            add(buf, CookieHeaderNames.DOMAIN, cookie.getDomain());
        }
        if (cookie.isSecure()) {
            add(buf, CookieHeaderNames.SECURE);
        }
        if (cookie.isHttpOnly()) {
            add(buf, CookieHeaderNames.HTTPONLY);
        }

        return stripTrailingSeparator(buf);
    }

    /**
     * Batch encodes cookies into Set-Cookie header values.
     *
     * @param cookies a bunch of cookies
     * @return the corresponding bunch of Set-Cookie headers
     */
    public List<String> encode(Cookie... cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }
        if (cookies.length == 0) {
            return Collections.emptyList();
        }

        List<String> encoded = new ArrayList<String>(cookies.length);
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }

    /**
     * Batch encodes cookies into Set-Cookie header values.
     *
     * @param cookies a bunch of cookies
     * @return the corresponding bunch of Set-Cookie headers
     */
    public List<String> encode(Collection<? extends Cookie> cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }
        if (cookies.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> encoded = new ArrayList<String>(cookies.size());
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }

    /**
     * Batch encodes cookies into Set-Cookie header values.
     *
     * @param cookies a bunch of cookies
     * @return the corresponding bunch of Set-Cookie headers
     */
    public List<String> encode(Iterable<? extends Cookie> cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }
        if (cookies.iterator().hasNext()) {
            return Collections.emptyList();
        }

        List<String> encoded = new ArrayList<String>();
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }
}

final class CookieHeaderNames {
    public static final String PATH = "Path";

    public static final String EXPIRES = "Expires";

    public static final String MAX_AGE = "Max-Age";

    public static final String DOMAIN = "Domain";

    public static final String SECURE = "Secure";

    public static final String HTTPONLY = "HTTPOnly";

    public static final String SAMESITE = "SameSite";

    private CookieHeaderNames() {
        // Unused.
    }
}

final class HttpHeaderDateFormat extends SimpleDateFormat {
    private static final long serialVersionUID = -925286159755905325L;

    private final SimpleDateFormat format1 = new HttpHeaderDateFormatObsolete1();
    private final SimpleDateFormat format2 = new HttpHeaderDateFormatObsolete2();

    private static final ThreadLocal<HttpHeaderDateFormat> FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(HttpHeaderDateFormat::new);

    public static HttpHeaderDateFormat get() {
        return FORMAT_THREAD_LOCAL.get();
    }

    /**
     * Standard date format
     */
    private HttpHeaderDateFormat() {
        super("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public Date parse(String text, ParsePosition pos) {
        Date date = super.parse(text, pos);
        if (date == null) {
            date = format1.parse(text, pos);
        }
        if (date == null) {
            date = format2.parse(text, pos);
        }
        return date;
    }

    /**
     * First obsolete format
     */
    private static final class HttpHeaderDateFormatObsolete1 extends SimpleDateFormat {
        private static final long serialVersionUID = -3178072504225114298L;

        HttpHeaderDateFormatObsolete1() {
            super("E, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH);
            setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    /**
     * Second obsolete format
     */
    private static final class HttpHeaderDateFormatObsolete2 extends SimpleDateFormat {
        private static final long serialVersionUID = 3010674519968303714L;

        HttpHeaderDateFormatObsolete2() {
            super("E MMM d HH:mm:ss yyyy", Locale.ENGLISH);
            setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }
}
