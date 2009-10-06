/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.SSOConfiguration;

public class SSOLinkGenerator {

	private static final Logger LOGGER = Logger.getLogger(SSOLinkGenerator.class);

	private String _target;

	private Configuration _config;

	private HttpServletRequest _request;

	/**
	 * SSOLinkGenerator will try and get a configuration from the ThreadLocal based SSOConfiguration, but you can
	 * override this by just setting a Configuration manually
	 * 
	 */
	public SSOLinkGenerator() {
		setConfig(SSOConfiguration.getConfig());
	}

	public final void setTarget(final String target) {
		_target = target;
	}

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

		target = getRequest().getRequestURL().toString();
		if (getRequest().getQueryString() != null) {
			target += "?" + getRequest().getQueryString();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Target from request.getRequestURL()=" + target);
		}

		String uriHeader = _config.getString("shire.uri-header");
		String urlParamKey = _config.getString("shire.urlparamkey");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("shire.uriHeader:" + uriHeader);
			LOGGER.debug("shire.urlparamkey:" + urlParamKey);
		}
		
		// SSO-770 accept the requested URI as a header
		if (uriHeader != null && getRequest().getHeader(uriHeader) != null) {
			
			target = getRequest().getHeader(uriHeader);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Found target from header - " + uriHeader + ": " + target);
			}
			
		} else if (urlParamKey != null && getRequest().getParameter(urlParamKey) != null) {

			target = getParamValueFromQueryString(urlParamKey, getRequest().getQueryString());

			String queryString = stripQueryStringParam(urlParamKey, getRequest().getQueryString());

			List keys = getConfig().getList("shire.stripparams.key");
			if (keys != null && !keys.isEmpty()) {
				Iterator it = keys.iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
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

		String[] params = queryString.split("&");
		for (int i = 0; i < params.length; i++) {
			String param = params[i];
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

		String newQS = "";
		String sep = "";
		String[] params = queryString.split("&");
		for (int i = 0; i < params.length; i++) {
			String param = params[i];
			if (param.startsWith(urlParamKey + "=")) {
				// drop it
			} else {
				// keep it and build back into querystring
				newQS += sep + param;
				sep = "&";
			}
		}
		return newQS;

	}

	public final Configuration getConfig() {
		return _config;
	}

	public final void setConfig(final Configuration config) {
		_config = config;
	}

	public final HttpServletRequest getRequest() {
		return _request;
	}

	public final void setRequest(final HttpServletRequest request) {
		_request = request;
	}

}
