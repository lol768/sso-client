package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.sso.client.core.*;
import uk.ac.warwick.sso.client.util.ListUtils;
import uk.ac.warwick.userlookup.*;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.CacheWithDataInitialisation;
import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.SingularCacheEntryFactoryWithDataInitialisation;
import uk.ac.warwick.util.core.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Framework-agnostic spinoff of SSOClientFilter.
 * SSOClientFilter now holds one of these and delegates to it to
 * do the work.
 */
public class SSOClientHandlerImpl implements SSOClientHandler {

    static final int BASIC_AUTH_CACHE_TIME_SECONDS = 300;

    static final String WARWICK_SSO = "WarwickSSO";

    public static final String USER_KEY = "SSO_USER";

    public static final String ACTUAL_USER_KEY = "SSO_ACTUAL_USER";

    public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

    public static final String PROXY_TICKET_COOKIE_NAME = "SSO-Proxy";

    public static final String DEFAULT_MASQUERADE_COOKIE_NAME = "masqueradeAs";

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOClientHandlerImpl.class);

    private SSOConfiguration _config;

    private AttributeAuthorityResponseFetcher _aaFetcher;

    private UserCache _cache;

    private UserLookupInterface _userLookup;

    private OnCampusService onCampusService;

    private CacheWithDataInitialisation<String, UserAndHash, String> _basicAuthCache;

    private boolean detectAnonymousOnCampusUsers;

    private boolean redirectToRefreshSession = true;

    private BasicAuthLoggingService basicAuthLoggingService =  new BasicAuthLoggingService();

    @Inject
    public SSOClientHandlerImpl(
        SSOConfiguration configuration,
        UserLookupInterface userlookup,
        AttributeAuthorityResponseFetcher aa,
        UserCache userCache,
        OnCampusService onCampusService
    ) {
        this._config = configuration;
        this._userLookup = userlookup;
        this._aaFetcher = aa;
        this._cache = userCache;
        this.onCampusService = onCampusService;

        runSanityCheck(_config);
    }

    public SSOClientHandlerImpl(
        SSOConfiguration configuration,
        UserLookupInterface userlookup,
        UserCache userCache,
        OnCampusService onCampusService
    ) {
        this(configuration, userlookup, new AttributeAuthorityResponseFetcherImpl(configuration), userCache, onCampusService);
    }

    private User doGetUserByOldSSO(final List<Cookie> cookies) {
        User user = new AnonymousUser();
        Cookie warwickSSO = getCookie(cookies, WARWICK_SSO);
        if (warwickSSO != null) {
            user = getUserLookup().getUserByToken(warwickSSO.getValue());
        }
        return user;
    }

    private boolean isBasicAuthRequest(final HttpRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Basic ");
    }

    private User doBasicAuth(final HttpRequest request) throws IOException {

        String auth64 = request.getHeader("Authorization");
        LOGGER.info("Doing BASIC auth:" + auth64);
        final int authStartPos = 6;
        auth64 = auth64.substring(authStartPos);
        BASE64Decoder decoder = new BASE64Decoder();
        String auth = new String(decoder.decodeBuffer(auth64.trim()));

        if (!auth.contains(":")) {
            LOGGER.debug("Returning anon user as auth was invalid: " + auth);
            return new AnonymousUser();
        }
        try {
            // self-populating cache makes the actual request to wsos. see #getBasicAuthCache
            String[] split = auth.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Malformed auth string - wrong number of colons.");
            }
            String userName = split[0];
            String password = split[1];

            UserAndHash userAndHash = getBasicAuthCache().get(userName, password);
            User user = userAndHash.getUser();
            String hash = userAndHash.getHash();

            if (hash != null && !SaltedDigest.matches(hash, password)) {
                // entry was previously valid, but pass doesn't match.
                getBasicAuthCache().remove(userName);
                userAndHash = getBasicAuthCache().get(userName, password);
                user = userAndHash.getUser();
                // don't need to check pass hash because it's just been generated based on a response.
            }
            basicAuthLoggingService.log(user, request);
            return user;

        } catch (Exception e) {
            LOGGER.warn("Exception making basic auth request - using anonymous user", e);
            return new AnonymousUser();
        }

    }

    private User authUserWithCache(String userName, String password)
            throws CacheEntryUpdateException {
        UserAndHash userAndHash = getBasicAuthCache().get(userName);
        User user = userAndHash.getUser();
        String hash = userAndHash.getHash();

        if (hash == null || !SaltedDigest.matches(hash, password)) {
            user = null;
        }
        return user;
    }

    private CacheWithDataInitialisation<String, UserAndHash, String> getBasicAuthCache() {
        if (_basicAuthCache == null) {
            _basicAuthCache = Caches.newDataInitialisatingCache("BasicAuthCache", new SingularCacheEntryFactoryWithDataInitialisation<String, UserAndHash, String>() {
                public UserAndHash create(String userName, String password) throws CacheEntryUpdateException {
                    try {
                        User user = getUserLookup().getUserByIdAndPassNonLoggingIn(userName, password);
                        String hash = null;
                        if (user.isFoundUser()) {
                            hash = SaltedDigest.generate(password);
                        }
                        return new UserAndHash(user, hash);
                    } catch (UserLookupException e) {
                        throw new CacheEntryUpdateException(e);
                    }
                }

                // only cache fully sucessful requests
                public boolean shouldBeCached(UserAndHash uah) {
                    return uah.getUser().isFoundUser();
                }
            }, BASIC_AUTH_CACHE_TIME_SECONDS, Caches.CacheStrategy.valueOf(getConfig().getString("ssoclient.cache.strategy")));
        }
        return _basicAuthCache;
    }


    private Response redirectToLogin(/*final HttpResponse response, */final HttpRequest request,
                                     final Cookie loginTicketCookie) throws UnsupportedEncodingException, IOException {
        Response response = new Response();
        response.setContinueRequest(false);

        boolean isRangeRequest = StringUtils.hasText(request.getHeader("Range"));
        if (isRangeRequest) {
            // Think this is to work around the looping bug in Chrome when it is requesting PDFs.
            // 407 is the only response that will stop its reign of terror.
            response.setStatusCode(407); // SC_PROXY_AUTHENTICATION_REQUIRED
            LOGGER.debug("Found global login cookie (" + loginTicketCookie.getValue()
                    + "), but not a valid SSC for range request - sending 407");
        } else {
            LinkGenerator generator = new LinkGeneratorImpl(_config, request);
            String loginLink = generator.getLoginUrl();
            response.setRedirect(loginLink);

            LOGGER.debug("Found global login cookie (" + loginTicketCookie.getValue()
                    + "), but not a valid SSC, redirecting to Handle Service " + _config.getString("origin.login.location"));
        }

        return response;
    }


    private void runSanityCheck(Configuration config) {
        String loginLocation = config.getString("origin.login.location", null);
        String mode = config.getString("mode", "new");

        if (mode.equals("old") && loginLocation.contains("/hs")) {
            LOGGER.warn("Possible misconfiguration: you are using old mode but the login location contains /hs, which is for new mode. Do you mean /slogin?");
        } else if (mode.equals("new") && loginLocation.contains("/slogin")) {
            LOGGER.warn("Possible misconfiguration: you are using new mode but the login location contains /slogin, which is for old mode. Do you mean /hs?");
        }
    }

    @Override
    public Response handle(final HttpRequest request) throws IOException {
        SSOConfiguration.setConfig(_config);
        try {
            URL target = getTarget(request);
            LOGGER.debug("Target=" + target);

            // prevent ssoclientfilter from sitting in front of shire and logout servlets
            String shireLocation = _config.getString("shire.location", null);
            String logoutLocation = _config.getString("logout.location", null);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("shire.location=" + shireLocation);
                LOGGER.debug("logout.location=" + logoutLocation);
            }

            if (target.toExternalForm().equals(shireLocation) || target.toExternalForm().equals(logoutLocation)) {
                LOGGER.debug("Letting request through without filtering because it is a shire or logout request");
                Response response = new Response();
                response.setContinueRequest(true);
                return response;
            }

            User user = new AnonymousUser();

            boolean allowBasic = allowHttpBasic(target, request);

    		/* [SSO-550] These variables are for handling sending WarwickSSO as a parameter,
             * useful in limited cases like Flash apps who can't send cookies
    		 */
            String requestToken = ListUtils.head(request.getParameter(WARWICK_SSO));
            boolean postCookies = ("POST".equalsIgnoreCase(request.getMethod()) && requestToken != null);

            if (allowBasic && request.getParameter("forcebasic").contains("true") && !isBasicAuthRequest(request)) {
                return basicAuthHeaders();
            }

            List<Cookie> cookies = request.getCookies();

            if (allowBasic && isBasicAuthRequest(request)) {
                user = doBasicAuth(request);
            } else if (_config.getString("mode").equals("old") || request.getAttribute("SSO_ALLOW_OLD_MODE") != null) {
                // do old style single sign on via WarwickSSO cookie
                user = doGetUserByOldSSO(cookies);
            } else if (postCookies) {
                user = getUserLookup().getUserByToken(requestToken);
            } else {
                // do new style single sign on with shibboleth
                Cookie loginTicketCookie = getCookie(cookies, GLOBAL_LOGIN_COOKIE_NAME);
                Cookie serviceSpecificCookie = getCookie(cookies, _config.getString("shire.sscookie.name"));

                Cookie proxyTicketCookie = getCookie(cookies, PROXY_TICKET_COOKIE_NAME);

                if (loginTicketCookie != null && serviceSpecificCookie == null) {
                    if (redirectToRefreshSession) {
                        return redirectToLogin(request, loginTicketCookie);
                    }
                } else {
                    if (proxyTicketCookie != null) {
                        user = getUserFromProxyTicket(proxyTicketCookie);
                    } else if (serviceSpecificCookie != null) {
                        LOGGER.debug("Found SSC (" + serviceSpecificCookie.getValue() + ")");

                        SSOToken token = new SSOToken(serviceSpecificCookie.getValue(), SSOToken.SSC_TICKET_TYPE);
                        UserCacheItem item = getCache().get(token);

                        if (redirectToRefreshSession && (item == null || !item.getUser().isLoggedIn()) && loginTicketCookie != null) {
                            Response response = redirectToLogin(request, loginTicketCookie);
                            // didn't find user, so cookie is invalid, destroy it!
                            destroySSC(response);
                            return response;
                        } else if (item != null && item.getUser().isLoggedIn()) {
                            user = item.getUser();
                        } else {
                            // user has SSC but is not actually logged in
                            LOGGER.debug("Invalid SSC as user was not found in cache");
                        }

                    }
                }
            }

            user = handleOnCampusUsers(user, request);
            setOldWarwickSSOToken(user, cookies);

            Response response = new Response();
            response.setContinueRequest(true);
            response.setActualUser(user);
            response.setUser(getApparentUser(cookies, user));

            return response;
        } finally {
            // SSO-1489
            //SSOConfiguration.removeConfig();
        }
    }

    private User getApparentUser(final List<Cookie> cookies, final User actualUser) {
        if (!actualUser.isFoundUser()) {
            return actualUser;
        }

        Cookie masqueradeCookie = getCookie(cookies, _config.getString("masquerade.cookie.name", DEFAULT_MASQUERADE_COOKIE_NAME));

        if (masqueradeCookie != null) {
            try {
                String masqueradeGroup = _config.getString("masquerade.group");

                if (getUserLookup().getGroupService().isUserInGroup(actualUser.getUserId(), masqueradeGroup)) {
                    User apparentUser = getUserLookup().getUserByUserId(masqueradeCookie.getValue());
                    if (apparentUser.isFoundUser()) {
                        LOGGER.debug(actualUser.getUserId() + " is masquerading as " + apparentUser.getUserId());
                        return apparentUser;
                    }
                }
            } catch (GroupServiceException e) {
                LOGGER.warn("Error checking membership of " + actualUser.getUserId() + " in masquerade group", e);
            } catch (NoSuchElementException e) {
                // Masquerade group is not configured
            }
        }

        return actualUser;
    }

    /**
     * If user is anonymous and they are on campus, replace the instance of User
     * with an AnonymousOnCampusUser. It extends AnonymousUser so it won't affect
     * current code, but you can use instanceof to check if they're on campus.
     */
    private User handleOnCampusUsers(User user, HttpRequest request) {
        if (detectAnonymousOnCampusUsers) {
            if (isOnCampus(request)) {
                if (user instanceof AnonymousUser) {
                    return new AnonymousOnCampusUser();
                }
                user.setOnCampus(true);
            }
        }
        return user;
    }

    private boolean isOnCampus(HttpRequest request) {
        return getOnCampusService().isOnCampus(request);
    }

    private void setOldWarwickSSOToken(final User user, final List<Cookie> cookies) {
        // set the old WarwickSSO token for legacy reasons
        if (user.getOldWarwickSSOToken() == null) {
            Cookie warwickSSO = getCookie(cookies, WARWICK_SSO);
            if (warwickSSO != null) {
                user.setOldWarwickSSOToken(warwickSSO.getValue());
            }
        }
    }

    private void destroySSC(final Response response) {
        Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
        cookie.setPath(_config.getString("shire.sscookie.path"));
        cookie.setDomain(_config.getString("shire.sscookie.domain"));
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    private Response basicAuthHeaders() {
        final String authHeader = "Basic realm=\"" + _config.getString("shire.providerid") + "\"";
        Response response = new Response();
        LOGGER.info("Client is requesting forcing HTTP Basic Auth, sending WWW-Authenticate=" + authHeader);
        response.setHeaders(Collections.singletonList(
                (Header) new BasicHeader("WWW-Authenticate", authHeader)
        ));
        response.setStatusCode(401 /* Unauthorized */);
        return response;
    }

    private User getUserFromProxyTicket(final Cookie proxyTicketCookie) {
        User user = new AnonymousUser();
        try {
            SAMLSubject subject = new SAMLSubject();
            SAMLNameIdentifier nameId = new SAMLNameIdentifier(proxyTicketCookie.getValue(),
                    _config.getString("origin.originid"), SSOToken.PROXY_TICKET_TYPE);
            subject.setName(nameId);
            LOGGER.info("Trying to get user from proxy cookie:" + nameId);
            user = getAaFetcher().getUserFromSubject(subject);
        } catch (SSOException e) {
            LOGGER.error("Could not get user from proxy cookie", e);
        } catch (SAMLException e) {
            LOGGER.error("Could not get user from proxy cookie", e);
        }
        return user;
    }

    /**
     * HTTP Basic Auth is only allowed over HTTPS, so we need to check that. But the app
     * itself generally isn't directly serving HTTPS so we need to check if it's being
     * proxied from localhost.
     * <p>
     * FIXME this doesn't work for a non-local frontend like BIG-IP
     */
    private boolean allowHttpBasic(final URL target, final HttpRequest request) {

        if (!_config.getBoolean("httpbasic.allow")) {
            return false;
        }

        boolean jBossLocalhost = false;
        boolean hasXForwardedFor = false;
        boolean jBossSSL = false;

        URL realURL = null;
        try {
            realURL = new URL(request.getRequestURL().toString());
            if (realURL.getHost().equalsIgnoreCase("localhost") || realURL.getHost().equalsIgnoreCase("localhost.warwick.ac.uk")) {
                jBossLocalhost = true;
            }
            if (realURL.getProtocol().equalsIgnoreCase("https")) {
                jBossSSL = true;
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not make URL out of:" + request.getRequestURI());
        }

        if (request.getHeader("x-forwarded-for") != null) {
            hasXForwardedFor = true;
        }

        if (hasXForwardedFor) {
            // was proxied...probably
            if ("https".equalsIgnoreCase(target.getProtocol()) || target.getHost().equalsIgnoreCase("localhost")
                    || target.getHost().equalsIgnoreCase("localhost.warwick.ac.uk")) {
                LOGGER.debug("HTTP Basic Auth is allowed because it is proxied but has a sensible target:" + target);
                return true;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP Basic Auth NOT allowed because it is proxied but does NOT have sensible target:" + target);
            }

        } else {
            // was not proxied...probably
            if (jBossSSL || jBossLocalhost) {
                LOGGER.debug("HTTP Basic Auth is allowed because jboss is running on localhost or SSL and is not proxied");
                return true;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER
                        .debug("HTTP Basic Auth is NOT allowed because jboss is NOT running on localhost or SSL and is not proxied");
            }
        }

        return false;
    }

    /**
     * @param request
     * @return
     */
    private URL getTarget(final HttpRequest request) {
        LinkGenerator generator = new LinkGeneratorImpl(_config, request);
        try {
            return new URL(generator.getTarget());
        } catch (MalformedURLException e) {
            LOGGER.warn("Target is an invalid url", e);
            throw new RuntimeException("Target is an invalid url", e);
        }

    }

    private static Cookie getCookie(final List<Cookie> cookies, final String name) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                LOGGER.debug("Found cookie:" + name + "=" + cookie.getValue());
                return cookie;
            }
        }
        return null;
    }

    @Override
    public AttributeAuthorityResponseFetcher getAaFetcher() {
        return _aaFetcher;
    }

    @Override
    public void setAaFetcher(final AttributeAuthorityResponseFetcher aaFetcher) {
        _aaFetcher = aaFetcher;
    }

    @Override
    public UserCache getCache() {
        return _cache;
    }

    @Override
    public void setCache(final UserCache cache) {
        _cache = cache;
    }

    @Override
    public Configuration getConfig() {
        return _config;
    }

    @Override
    public boolean isDetectAnonymousOnCampusUsers() {
        return detectAnonymousOnCampusUsers;
    }

    @Override
    public void setDetectAnonymousOnCampusUsers(boolean detectAnonymousOnCampusUsers) {
        this.detectAnonymousOnCampusUsers = detectAnonymousOnCampusUsers;
    }


    /**
     * If true (the default), then if the filter finds cookies relating to an existing session that is
     * no longer valid, it will redirect to Websignon to get those cookies refreshed. You may want to disable
     * this for places where you wish to know about a user who might be logged in, but don't ever want
     * SSOClient to automatically redirect the user agent away. In these cases, an AnonymousUser will
     * be returned instead.
     */
    @Override
    public boolean isRedirectToRefreshSession() {
        return redirectToRefreshSession;
    }

    /**
     * @see #isRedirectToRefreshSession()
     */
    @Override
    public void setRedirectToRefreshSession(boolean redirectToRefreshSession) {
        this.redirectToRefreshSession = redirectToRefreshSession;
    }

    @Override
    public UserLookupInterface getUserLookup() {
        return _userLookup;
    }

    static class UserAndHash implements Serializable {
        private static final long serialVersionUID = 4707495652163070391L;
        private final User user;
        private final String hash;

        public UserAndHash(User user, String hash) {
            super();
            this.user = user;
            this.hash = hash;
        }

        public User getUser() {
            return user;
        }

        public String getHash() {
            return hash;
        }
    }

    @Override
    public OnCampusService getOnCampusService() {
        return onCampusService;
    }

    @Override
    public void setOnCampusService(OnCampusService onCampusService) {
        this.onCampusService = onCampusService;
    }
}
