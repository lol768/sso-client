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
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.Cookie;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLStatement;

import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserCacheItem;
import uk.ac.warwick.userlookup.UserLookup;

public class ShireCommand {

	private Configuration _config;

	private static final Logger LOGGER = Logger.getLogger(ShireCommand.class);

	private AttributeAuthorityResponseFetcher _aaFetcher;

	/**
	 * @param saml64
	 * @param target
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws HttpException
	 */
	public Cookie process(String saml64, String target) throws IOException, MalformedURLException, HttpException {
		SAMLResponse samlResponse = null;
		LOGGER.debug("TARGET:" + target);
		LOGGER.debug("SAML64:" + saml64);
		if (target == null || saml64 == null) {
			LOGGER.error("Must have a SAMLResponse and a TARGET");
			throw new RuntimeException("Must have a SAMLResponse and a TARGET");
		}
		// check we've got a valid SAML request
		try {
			samlResponse = SAMLPOSTProfile.accept(saml64.getBytes(), _config.getString("shire.providerid"), 5, false);
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
		LOGGER.info("Subject name:" + authStatement.getSubject().getName().toString());
		LOGGER.info("Subject name:" + authStatement.getSubject().getName().getName());

		SAMLResponse samlResp = getAaFetcher().getSAMLResponse(authStatement);

		Properties attributes = getAttributesFromResponse(samlResp);

		User user = createUserFromAttributes(attributes);

		String SSC = attributes.getProperty("urn:websignon:ssc");
		if (SSC != null) {
			user.setToken(SSC);
			user.setIsLoggedIn(true);
			UserCacheItem item = new UserCacheItem(user, new Date().getTime(), SSC);
			UserLookup.getInstance().getUserCache().put(SSC, item);
			UserLookup.getInstance().getUserByToken(SSC, false);
			Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), SSC);
			cookie.setPath(_config.getString("shire.sscookie.path"));
			cookie.setDomain(_config.getString("shire.sscookie.domain"));
			// create cookie so that service can retrieve user from cache
			return cookie;
		}
		
		// no SSC found, so can't create cookie
		return null;
		



		
	}

	/**
	 * @param samlResponse
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private boolean verifySAMLResponse(SAMLResponse samlResponse) throws IOException, MalformedURLException {
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
	private Certificate getCertificate(final String alias) throws IOException, MalformedURLException {

		try {
			KeyStore keyStore = getKeyStore();
			Certificate originCert = keyStore.getCertificate(alias);
			return originCert;
		} catch (KeyStoreException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (CertificateException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
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

	/**
	 * @param samlResp
	 * @return
	 */
	private User createUserFromAttributes(Properties attributes) {
		User user = new User();
		user.setUserId((String) attributes.get("cn"));
		user.setLastName((String) attributes.get("sn"));
		user.setFirstName((String) attributes.get("givenName"));
		user.setWarwickId((String) attributes.get("warwickuniid"));
		user.setDepartmentCode((String) attributes.get("warwickdeptcode"));
		user.setDepartment((String) attributes.get("ou"));
		user.setEmail((String) attributes.get("email"));

		user.getExtraProperties().putAll(attributes);

		return user;
	}

	/**
	 * @param samlResp
	 * @return
	 */
	private Properties getAttributesFromResponse(final SAMLResponse samlResp) {
		Properties attributes = new Properties();
		
		if (samlResp.getAssertions() == null || !samlResp.getAssertions().hasNext()) {
			return attributes;
		}
		
		SAMLAssertion attributeAssertion = (SAMLAssertion) samlResp.getAssertions().next();
		SAMLAttributeStatement attributeStatement = (SAMLAttributeStatement) attributeAssertion.getStatements().next();
		Iterator it = attributeStatement.getAttributes();
		while (it.hasNext()) {
			SAMLAttribute attribute = (SAMLAttribute) it.next();
			String name = attribute.getName();
			String value = (String) attribute.getValues().next();
			LOGGER.info(name + "=" + value);
			attributes.put(name, value);
		}
		return attributes;
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

}
