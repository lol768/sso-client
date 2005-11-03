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
		setConfig((new SSOConfiguration()).getConfig());
	}

	public final void setTarget(final String target) {
		_target = target;
	}

	public final String getTarget() {

		String target = _target;

		if (target != null && !target.equals("")) {
			LOGGER.debug("Using target specifically passed in:" + target);
			return target;
		}

		if (getRequest() == null) {
			LOGGER.debug("Found not HttpServletRequest to get url information from, returning empty target");
			return "";
		}

		target = getRequest().getRequestURL().toString();
		if (getRequest().getQueryString() != null) {
			target += "?" + getRequest().getQueryString();
		}
		LOGGER.debug("Target from request.getRequestURL()=" + target);

		String urlParamKey = _config.getString("shire.urlparamkey");
		LOGGER.debug("shire.urlparamkey:" + urlParamKey);
		if (urlParamKey != null && getRequest().getParameter(urlParamKey) != null) {
			target = getRequest().getParameter(urlParamKey);

			String queryString = getRequest().getQueryString();

			queryString = stripQueryStringParam(urlParamKey, queryString);

			List keys = getConfig().getList("shire.stripparams.key");
			if (keys != null && !keys.isEmpty()) {
				Iterator it = keys.iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
					queryString = stripQueryStringParam(key, queryString);
				}
			}
			
			if (queryString.startsWith("&")) {
				queryString = queryString.replaceFirst("&","");
			}
			if (queryString != null && queryString.length() > 0) {
				target += "?" + queryString;
			}
			LOGGER.debug("Found target from paramter " + urlParamKey + "=" + target);
		}
		return target;

	}

	/**
	 * @param urlParamKey
	 * @param queryString
	 * @return
	 */
	private String stripQueryStringParam(final String urlParamKey, final String queryString) {
		String newQueryString = queryString.replaceFirst(urlParamKey + "=" + getRequest().getParameter(urlParamKey), "");
		return newQueryString;
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
