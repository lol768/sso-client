/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLStatement;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.sso.client.core.Cookie;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.CacheEntry;

public class ShireCommand {

	public static final String CSRF_TOKEN_PROPERTY_NAME = "urn:websignon:csrf";

	private static final Logger LOGGER = LoggerFactory.getLogger(ShireCommand.class);

	private Configuration _config;

	private AttributeAuthorityResponseFetcher _aaFetcher;

	private String _remoteHost;

	private UserCache _cache;
	
	private final Cache<String, User> _userIdCache;
	
	public ShireCommand(Cache<String, User> userIdCache) {
		this._userIdCache = userIdCache;
	}

	public ShireCommand(
		SSOConfiguration config,
		UserCache userCache,
		Cache<String, User> userIdCache
	) {
		this(userIdCache);
		setCache(userCache);
		setConfig(config);
	}

	public final Cookie process(final String saml64, final String target) throws SSOException {
		try {
			SAMLResponse samlResponse = null;
			if (LOGGER.isDebugEnabled()) LOGGER.debug("TARGET:" + target);
			if (LOGGER.isDebugEnabled()) LOGGER.debug("SAML64:" + saml64);
			if (target == null) {
				LOGGER.error("Must have a TARGET");
				throw new IllegalArgumentException("Must have a TARGET");
			}
			if (saml64 == null) {
				LOGGER.error("Must have a SAMLResponse");
				throw new IllegalArgumentException("Must have a SAMLResponse");
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
				LOGGER.warn("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
				throw new RuntimeException("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
			}
	
			try {
				String targetHost = new URL(target).getHost();
				String cookieHost = _config.getString("shire.sscookie.domain");
				if (!targetHost.endsWith(cookieHost)) {
					throw new RuntimeException("Target is on a different host from the host that the cookie is being set on: "
							+ targetHost + " != " + cookieHost);
				}
			} catch (MalformedURLException e) {
				throw new RuntimeException("Target is not a valid URL: " + target);
			}
	
			LOGGER.info("Accepted Shire request for target:" + target);
	
			if (LOGGER.isDebugEnabled()) LOGGER.debug("SAML:" + samlResponse.toString());
			SAMLAssertion assertion = (SAMLAssertion) samlResponse.getAssertions().next();
			if (LOGGER.isDebugEnabled()) LOGGER.debug("Assertion:" + assertion.toString());
	
			String issuer = assertion.getIssuer();
			if (!issuer.equals(getConfig().getString("origin.originid"))) {
				LOGGER.warn("Someone trying to authenticate from wrong origin:" + issuer);
				throw new RuntimeException("Someone trying to authenticate from wrong origin:" + issuer);
			}
	
			SAMLStatement statement = (SAMLStatement) assertion.getStatements().next();
			if (LOGGER.isDebugEnabled()) LOGGER.debug("Statement:" + statement.toString());
			SAMLAuthenticationStatement authStatement = (SAMLAuthenticationStatement) statement;
			if (LOGGER.isDebugEnabled()) LOGGER.debug("Auth Statement:" + authStatement.toString());
			SAMLSubject subject = authStatement.getSubject();
			if (LOGGER.isDebugEnabled()) LOGGER.debug("Subject name:" + subject.getName().toString());
	
			User user = getUserFromAuthSubject(subject);
			user.getExtraProperties().put(CSRF_TOKEN_PROPERTY_NAME, UUID.randomUUID().toString());
	
			if (user.getExtraProperty("urn:websignon:ipaddress") != null) {
				if (user.getExtraProperty("urn:websignon:ipaddress").equals(getRemoteHost())) {
					LOGGER.info("Users shire submission is from same host as they logged in from: Shire&Login=" + getRemoteHost());
				} else {
					LOGGER.warn("Users shire submission is NOT from same host as they logged in from. Login="
							+ user.getExtraProperty("urn:websignon:ipaddress") + ", Shire=" + getRemoteHost());
				}
			}
	
			if (user.getExtraProperty(SSOToken.SSC_TICKET_TYPE) != null) {
				return setupSSC(user);
			}
	
			// no SSC found, so can't create cookie
			return null;
			
		} catch (RuntimeException e) {
			logParameters(saml64, target);
			throw e;
		} catch (SSOException e) {
			logParameters(saml64, target);
			throw e;
		}
	}



	private void logParameters(final String saml64, final String target) {
		LOGGER.warn("target:"+target);
		LOGGER.warn("saml64:"+saml64);
	}
	
	

	public Cookie setupSSC(final User user) {
		SSOToken token = new SSOToken((String) user.getExtraProperty(SSOToken.SSC_TICKET_TYPE), SSOToken.SSC_TICKET_TYPE);
		user.setIsLoggedIn(true);
		UserCacheItem item = new UserCacheItem(user, new Date().getTime(), token);
		getCache().put(token, item);
		
		// also place the new user in the UserLookup user-by-id cache
		final String userId = user.getUserId();
		if (user.isFoundUser() && userId != null && !"".equals(userId.trim())) {
			_userIdCache.put(new CacheEntry<>(userId, user));
		}		
		
		Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), token.getValue());
		cookie.setPath(_config.getString("shire.sscookie.path"));
		cookie.setDomain(_config.getString("shire.sscookie.domain"));
		cookie.setSecure(_config.getBoolean("shire.sscookie.secure", false));
		cookie.setHttpOnly(true);

		if (_config.getBoolean("shire.sscookie.indefinite", false)) {
			cookie.setMaxAge(Integer.MAX_VALUE);
		}

		// create cookie so that service can retrieve user from cache
		return cookie;
	}

	private User getUserFromAuthSubject(final SAMLSubject subject) throws SSOException {
		return getAaFetcher().getUserFromSubject(subject);
	}

	private boolean verifySAMLResponse(final SAMLResponse samlResponse) {
		try {
			samlResponse.verify();
			return true;
		} catch (SAMLException e) {
			LOGGER.error("Could not verify SAMLResponse", e);
			return false;
		}

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

	public final String getRemoteHost() {
		return _remoteHost;
	}

	public final void setRemoteHost(final String remoteHost) {
		_remoteHost = remoteHost;
	}

}
