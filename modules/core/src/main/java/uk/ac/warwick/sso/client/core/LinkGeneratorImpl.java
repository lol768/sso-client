package uk.ac.warwick.sso.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.userlookup.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class LinkGeneratorImpl implements LinkGenerator {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(LinkGeneratorImpl.class);
    
    private final HttpRequest _request;
    private final SSOConfiguration _config;

    private String _target;

    public LinkGeneratorImpl(SSOConfiguration config, HttpRequest request) {
        this._request = request;
        this._config = config;
    }

    @Override
    public final String getLoginUrl() {

        String loginLocation = getConfig().getString("origin.login.location");
        if (loginLocation == null || loginLocation.equals("")) {
            throw new RuntimeException("SSOLoginLinkTag needs the property: origin.login.location");
        }
        String shireLocation = getConfig().getString("shire.location");
        if (shireLocation == null || shireLocation.equals("")) {
            throw new RuntimeException("SSOLoginLinkTag needs the property: shire.location");
        }
        String providerId = getConfig().getString("shire.providerid");
        if (providerId == null || providerId.equals("")) {
            throw new RuntimeException("SSOLoginLinkTag needs the property: shire.providerid");
        }

        String linkUrl;
        try {
            linkUrl = loginLocation + "?shire=" + URLEncoder.encode(shireLocation, "UTF-8") + "&providerId="
                    + URLEncoder.encode(providerId, "UTF-8") + "&target=" + URLEncoder.encode(getTarget(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }

        return linkUrl;

    }

    @Override
    public final String getLogoutUrl()  {

        if (getConfig() == null) {
            throw new RuntimeException("Could not find the SSO config");
        }

        String logoutLocation = getConfig().getString("origin.logout.location");
        if (logoutLocation == null || logoutLocation.equals("")) {
            throw new RuntimeException("SSOLogoutLinkGenerator needs a logout location origin.logout.location");
        }

        String linkUrl;
        try {
            linkUrl = logoutLocation + "?target=" + URLEncoder.encode(getTarget(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }

        return linkUrl;

    }


    /**
     * Will use the notloggedin link if the user isn't logged in.
     * This is safe to do, because if the user is not logged in then
     * the two attributes have the exact same behaviour. If they
     * are signed in but they aren't allowed to access a resource, then
     * they genuinely do need error=permdenied so they get the
     * page to login again.
     */
    @Override
    public final String getPermissionDeniedLink(boolean signedIn) {
        if (signedIn) {
            return getRealPermissionDeniedLink();
        } else {
            return getNotLoggedInLink();
        }
    }

    public final String getRealPermissionDeniedLink() {
        return getLoginUrl() + "&error=permdenied";
    }

    @Override
    public final String getNotLoggedInLink() {
        return getLoginUrl() + "&error=notloggedin";
    }

    @Override
    public final String getTarget() {

        String target = _target;

        if (target != null && !target.equals("")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using target specifically passed in:" + target);
            }
            return target;
        }

        if (getRequest() == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found not HttpServletRequest to get url information from, returning empty target");
            }
            return "";
        }

        target = getRequest().getRequestURL();
        if (getRequest().getQueryString() != null) {
            target += "?" + getRequest().getQueryString();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Target from _request.getRequestURL()=" + target);
        }

        String uriHeader = _config.getString("shire.uri-header", null);
        String urlParamKey = _config.getString("shire.urlparamkey", null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("shire.uriHeader:" + uriHeader);
            LOGGER.debug("shire.urlparamkey:" + urlParamKey);
        }

        // SSO-770 accept the _requested URI as a header
        if (uriHeader != null && getRequest().getHeader(uriHeader) != null) {

            target = getRequest().getHeader(uriHeader);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found target from header - " + uriHeader + ": " + target);
            }

        } else if (urlParamKey != null && getRequest().getParameter(urlParamKey).size() > 0) {

            target = getParamValueFromQueryString(urlParamKey, getRequest().getQueryString());

            String queryString = stripQueryStringParam(urlParamKey, getRequest().getQueryString());

            List<String> keys = getConfig().getList("shire.stripparams.key");
            if (keys != null) {
                for (String key : keys) {
                    queryString = stripQueryStringParam(key, queryString);
                }
            }

            if (queryString.startsWith("&")) {
                queryString = queryString.replaceFirst("&", "");
            }
            if (queryString != null && queryString.length() > 0) {
                target += "?" + queryString;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found target from parameter " + urlParamKey + "=" + target);
            }
        }
        return target;

    }


    private String getParamValueFromQueryString(final String paramName, final String queryString) {
        for (String param : queryString.split("&")) {
            if (param.startsWith(paramName + "=")) {
                return param.split("=")[1];
            }
        }
        return null;
    }

    /**
     * @param urlParamKey
     * @param queryString
     * @return
     */
    private String stripQueryStringParam(final String urlParamKey, final String queryString) {
        StringBuilder newQS = new StringBuilder();
        String sep = "";
        for (String param : queryString.split("&")) {
            if (param.startsWith(urlParamKey + "=")) {
                // drop it
            } else {
                // keep it and build back into querystring
                newQS.append(sep).append(param);
                sep = "&";
            }
        }
        return newQS.toString();

    }

    public SSOConfiguration getConfig() {
        return _config;
    }

    public HttpRequest getRequest() {
        return _request;
    }

    @Override
    public void setTarget(String target) {
        this._target = target;
    }
}
