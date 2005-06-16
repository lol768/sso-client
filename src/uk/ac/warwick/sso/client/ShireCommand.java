/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLStatement;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.userlookup.User;

public class ShireCommand {

	private Configuration _config;

	private static final Logger LOGGER = Logger.getLogger(ShireCommand.class);

	private AttributeAuthorityResponseFetcher _aaFetcher;
	
	private UserCache _cache;

	/**
	 * @param saml64
	 * @param target
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws HttpException
	 */
	public final Cookie process(final String saml64, final String target) throws SSOException {
		SAMLResponse samlResponse = null;
		LOGGER.debug("TARGET:" + target);
		LOGGER.debug("SAML64:" + saml64);
		if (target == null || saml64 == null) {
			LOGGER.error("Must have a SAMLResponse and a TARGET");
			throw new RuntimeException("Must have a SAMLResponse and a TARGET");
		}
		// check we've got a valid SAML request
		try {
			final int timeout = 5;
			samlResponse = SAMLPOSTProfile.accept(saml64.getBytes(), _config.getString("shire.providerid"), timeout, false);
		} catch (SAMLException e) {
			LOGGER.error("SAMLException accepting SAMLPOSTProfile", e);
			throw new RuntimeException("SAMLException thrown accepting POST profile", e);
		}

		boolean validResponse = verifySAMLResponse(samlResponse);
		if (!validResponse) {
			LOGGER.info("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
			throw new RuntimeException("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
		}

		LOGGER.info("SAML:" + samlResponse.toString());
		SAMLAssertion assertion = (SAMLAssertion) samlResponse.getAssertions().next();
		LOGGER.info("Assertion:" + assertion.toString());
		SAMLStatement statement = (SAMLStatement) assertion.getStatements().next();
		LOGGER.info("Statement:" + statement.toString());
		SAMLAuthenticationStatement authStatement = (SAMLAuthenticationStatement) statement;
		LOGGER.info("Auth Statement:" + authStatement.toString());
		SAMLSubject subject = authStatement.getSubject();
		LOGGER.info("Subject name:" + subject.getName().toString());
		LOGGER.info("Subject name:" + subject.getName().getName());

		User user = getUserFromAuthSubject(subject);

		String serviceSpecificCookie = (String) user.getExtraProperty(SSOToken.SSC_TICKET_TYPE);
		if (serviceSpecificCookie != null) {
			SSOToken token = new SSOToken(serviceSpecificCookie,SSOToken.SSC_TICKET_TYPE);
			user.setToken(token.getValue());
			user.setIsLoggedIn(true);
			UserCacheItem item = new UserCacheItem(user, new Date().getTime(), token);
			getCache().put(token,item);
			//UserLookup.getInstance().getUserCache().put(token, item);
			//UserLookup.getInstance().getUserByToken(serviceSpecificCookie, false);
			Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), token.getValue());
			cookie.setPath(_config.getString("shire.sscookie.path"));
			cookie.setDomain(_config.getString("shire.sscookie.domain"));
			// create cookie so that service can retrieve user from cache
			return cookie;
		}

		// no SSC found, so can't create cookie
		return null;

	}

	/**
	 * @param subject
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private User getUserFromAuthSubject(final SAMLSubject subject) throws SSOException {
		return getAaFetcher().getUserFromSubject(subject);
	}

	/**
	 * @param samlResponse
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private boolean verifySAMLResponse(final SAMLResponse samlResponse) {
		try {
			Certificate originCert = getCertificate(_config.getString("shire.keystore.origin-alias"));

			samlResponse.verify(originCert);
			return true;
		} catch (SAMLException e) {
			LOGGER.error("Could not verify SAMLResponse", e);
			return false;
		}

	}

	/**
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private Certificate getCertificate(final String alias) {

		try {
			KeyStore keyStore = getKeyStore();
			Certificate originCert = keyStore.getCertificate(alias);
			return originCert;
		} catch (Exception e) {
			LOGGER.error("Could not get certificate", e);
			throw new RuntimeException("Could not get certificate", e);
		}

	}

	/**
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			MalformedURLException {
		KeyStoreHelper helper = new KeyStoreHelper();
		KeyStore keyStore = helper.createKeyStore(new URL(_config.getString("shire.keystore.location")), _config
				.getString("shire.keystore.password"));
		return keyStore;
	}

	public final Configuration getConfig() {
		return _config;
	}

	public final void setConfig(final Configuration config) {
		_config = config;
	}

	public final AttributeAuthorityResponseFetcher getAaFetcher() {
		return _aaFetcher;
	}

	public final void setAaFetcher(final AttributeAuthorityResponseFetcher aaFetcher) {
		_aaFetcher = aaFetcher;
	}

	
	public final UserCache getCache() {
		return _cache;
	}

	
	public final void setCache(final UserCache cache) {
		_cache = cache;
	}

}
