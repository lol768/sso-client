/*
 * Created on 17-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.Cookie;
import org.apache.log4j.Logger;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public final class SSOProxyCookieHelper {

	private SSOProxyCookieHelper() {
		// static utility class
	}

	public static final Logger LOGGER = Logger.getLogger(SSOProxyCookieHelper.class);

	/**
	 * If you are going to proxy through to another SSO enabled application, you need a proxy cookie. Passing in an SSO
	 * config, a target url and id and a user, the helper returns an HttpClient cookie which just needs to be added into
	 * the HttpState for the HttpClient connection that is being made to the target
	 * 
	 * @param config
	 * @param targetURL
	 * @param proxyTargetServiceId
	 * @param user
	 * @return
	 */
	public static Cookie getProxyCookie(final Configuration config, final URL targetURL, final User user) {

		String pgt = (String) user.getExtraProperty(SSOToken.PROXY_GRANTING_TICKET_TYPE);

		if (pgt == null) {
			return null;
		}

		AttributeAuthorityResponseFetcher fetcher = new AttributeAuthorityResponseFetcherImpl();
		fetcher.setConfig(config);

		String proxyTicket = null;

		try {
			SAMLNameIdentifier nameId = new SAMLNameIdentifier(pgt, config.getString("shire.providerid"),
					SSOToken.PROXY_GRANTING_TICKET_TYPE);
			SAMLSubject subject = new SAMLSubject(nameId, null, null, null);

			proxyTicket = fetcher.getProxyTicket(subject, targetURL.toExternalForm());
			LOGGER.info("Got proxyticket:" + proxyTicket);

		} catch (SSOException e) {
			LOGGER.error("Can not connect to AA", e);
			throw new RuntimeException("Can not connect to AA", e);
		} catch (SAMLException e) {
			LOGGER.error("Can not generate SAMLSubject", e);
			throw new RuntimeException("Can not generate SAMLSubject", e);
		}

		Cookie cookie = new Cookie();
		cookie.setDomain(targetURL.getHost());
		cookie.setPath("/");
		cookie.setName("SSO-Proxy");
		cookie.setValue(proxyTicket);
		return cookie;

	}

	public static Cookie getProxyCookie(final URL targetURL, final User user) {

		SSOConfiguration config = new SSOConfiguration();
		return getProxyCookie(config.getConfig(), targetURL, user);
	}

}
