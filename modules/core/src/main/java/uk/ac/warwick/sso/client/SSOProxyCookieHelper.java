/*
 * Created on 17-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URL;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public class SSOProxyCookieHelper {

	private AttributeAuthorityResponseFetcher _attributeAuthorityResponseFetcher;

	private SSOConfiguration _config;

	/**
	 * You do not need to set the config or attributeauthorityresponsefetcher, good defaults are used, but you can
	 * override them if you so choose
	 * 
	 */
	public SSOProxyCookieHelper() {
		// default empty constructor
	}

	public static final Logger LOGGER = LoggerFactory.getLogger(SSOProxyCookieHelper.class);

	/**
	 * If you are going to proxy through to another SSO enabled application, you need a proxy cookie. Passing in an SSO
	 * config, a target url and id and a user, the helper returns an HttpClient cookie which just needs to be added into
	 * the HttpState for the HttpClient connection that is being made to the target. Will return a null cookie if a
	 * cookie can't be generated
	 * 
	 * 
	 * @param targetURL
	 * 
	 * @param user
	 * @return
	 */
	public final Cookie getProxyHttpClientCookie(final URL targetURL, final User user) {

		String pgt = (String) user.getExtraProperty(SSOToken.PROXY_GRANTING_TICKET_TYPE);

		if (pgt == null) {
			LOGGER.debug("User had no " + SSOToken.PROXY_GRANTING_TICKET_TYPE + " property");
			return null;
		}

		String proxyTicket = null;

		try {
			SAMLNameIdentifier nameId = new SAMLNameIdentifier(pgt, getConfig().getString("shire.providerid"),
					SSOToken.PROXY_GRANTING_TICKET_TYPE);
			SAMLSubject subject = new SAMLSubject(nameId, null, null, null);

			proxyTicket = getAttributeAuthorityResponseFetcher().getProxyTicket(subject, targetURL.toExternalForm());

			if (proxyTicket == null) {
				LOGGER.info("No " + SSOToken.PROXY_GRANTING_TICKET_TYPE + " returned, so returning null for cookie");
				return null;
			}

			LOGGER.info("Got proxyticket:" + proxyTicket);
			return generateHttpClientCookie(targetURL, proxyTicket);

		} catch (SSOException e) {
			LOGGER.error("Can not connect to AA", e);
			return null;
		} catch (SAMLException e) {
			LOGGER.error("Can not generate SAMLSubject", e);
			return null;
		}

	}

	/**
	 * @param targetURL
	 * @param proxyTicket
	 * @return
	 */
	private Cookie generateHttpClientCookie(final URL targetURL, final String proxyTicket) {
		if (proxyTicket == null) {
			return null;
		}

        BasicClientCookie2 cookie = new BasicClientCookie2("SSO-Proxy", proxyTicket);
        cookie.setDomain(targetURL.getHost());
		cookie.setPath("/");
		return cookie;
	}

	public final AttributeAuthorityResponseFetcher getAttributeAuthorityResponseFetcher() {
		if (_attributeAuthorityResponseFetcher == null) {
			return new AttributeAuthorityResponseFetcherImpl(getConfig());
		}
		return _attributeAuthorityResponseFetcher;
	}

	public final void setAttributeAuthorityResponseFetcher(final AttributeAuthorityResponseFetcher fetcher) {
		_attributeAuthorityResponseFetcher = fetcher;
	}

	public final SSOConfiguration getConfig() {
		if (_config == null) {
			return SSOConfiguration.getConfig();
		}
		return _config;
	}

	public final void setConfig(final SSOConfiguration config) {
		_config = config;
	}

}
