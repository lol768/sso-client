/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.cache.DatabaseUserCache;
import uk.ac.warwick.sso.client.cache.InMemoryUserCache;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;

/**
 * Listener to be inserted into web.xml which will load the SSO configuration on startup.
 * 
 * Requires a ServletContext Parameter to be set "ssoclient.config=/sso-config.xml"
 * 
 * <p><b>Example</b></p>
 * <pre>
 * &lt;context-param>
 *  &lt;param-name>ssoclient.config&lt;/param-name>
 *  &lt;param-value>/sso-config.xml&lt;/param-value>
 * &lt;/context-param>
 * 
 * &lt;listener>
 *  &lt;listener-class>uk.ac.warwick.sso.client.SSOConfigLoader&lt;/listener-class>
 * &lt;/listener>
 */
public class SSOConfigLoader implements ServletContextListener {

	private static final Logger LOGGER = Logger.getLogger(SSOConfigLoader.class);

	public static final String SSO_CONFIG_KEY = "SSO-CONFIG";

	public static final String SSO_CACHE_KEY = "SSO-CACHE";

	public SSOConfigLoader() {
		super();
	}

	public final void contextInitialized(final ServletContextEvent event) {
		loadSSOConfig(event.getServletContext());
	}

	public SSOConfiguration loadSSOConfig(final String ssoConfigLocation) {
		if (ssoConfigLocation == null) {
			String message = "Path to SSO config was null";
			LOGGER.error(message);
			throw new RuntimeException(message);
		}
		URL configUrl = SSOConfigLoader.class.getResource(ssoConfigLocation);
		if (configUrl == null) {
			String message = "Could not find SSO config at location " + ssoConfigLocation + " - check your classpath";
			LOGGER.error(message);
			throw new RuntimeException(message);
		}		

		XMLConfiguration config;
		try {
			config = new XMLConfiguration(configUrl);
		} catch (ConfigurationException e) {
			throw new RuntimeException("Could not setup configuration", e);
		}
		
		sanityCheck(config);
		
//		if (shouldUseKeystore(config)) {
//			String websignonLoginUrl = ConfigHelper.getRequiredString(config,"origin.login.location");
//			setupHttpsProtocol(websignonLoginUrl, config.getString("shire.keystore.location"), config.getString("shire.keystore.password"), config
//					.getString("cacertskeystore.location"), config.getString("cacertskeystore.password"));
//		}
		
		// wrap it in SSOConfiguration
		SSOConfiguration ssoConfiguration = new SSOConfiguration(config);
		ssoConfiguration.getAuthenticationDetails(); // eagerly load keys and certs, get any errors out fast
		return ssoConfiguration;

	}

	private void sanityCheck(Configuration config) {
		String mode = config.getString("mode");
		String loginLocation = config.getString("origin.login.location");
		if ("new".equals(mode) && loginLocation.contains("/slogin")) {
			LOGGER.error("It looks like you are using new mode with /slogin in the configuration. New mode should point to /hs");
		} else if ("old".equals(mode) && loginLocation.contains("/hs")) {
			LOGGER.error("It looks like you are using old mode with /hs in the configuration. Old mode should point to /slogin");
		}
	}
	
	/**
	 * Use client keystore for HTTPS connections, unless we're in old
	 * mode. In old mode, just use the default HTTPS setup. 
	 * SSO-591
	 */
	private boolean shouldUseKeystore(XMLConfiguration config) {
		return (! "old".equals(config.getString("mode")));
	}

	public void loadSSOConfig(ServletContext servletContext) {
		Enumeration<?> params = servletContext.getInitParameterNames();
		boolean foundConfigs = false;
		while (params.hasMoreElements()) {
			String paramName = (String) params.nextElement();
			if (paramName.startsWith("ssoclient.config")) {
				foundConfigs = true;

				String ssoConfigLocation = servletContext.getInitParameter(paramName);

				SSOConfiguration config = loadSSOConfig(ssoConfigLocation);

				String configSuffix = paramName.replaceFirst("ssoclient.config", "");
				LOGGER.info("Using suffix for config:" + configSuffix);

				servletContext.setAttribute(SSO_CONFIG_KEY + configSuffix, config);

				UserCache cache = getCache(config);
				servletContext.setAttribute(SSO_CACHE_KEY + configSuffix, cache);

			}
		}
		
		if (!foundConfigs) {
			throw new IllegalStateException("SSOConfigLoader found no ssoclient.config* element in the web.xml");
		}
	}

	private UserCache getCache(Configuration config) {

		if (config.containsKey("cluster.enabled") && config.getBoolean("cluster.enabled")) {
			final String dsName = config.getString("cluster.datasource");
			// key name is an override for the 'key' field in the objectcache database. This is for compatibility
			// reasons so that MySql can work
			final String keyName = config.getString("cluster.keyname");
			return getClusteredCache(dsName, keyName);
		}
		LOGGER.info("Loading standard InMemoryUserCache");
		return new InMemoryUserCache();

	}

	private UserCache getClusteredCache(final String dsName, final String keyName) {

		LOGGER.info("Loading clustered DatabaseUserCache");

		DatabaseUserCache dbCache = new DatabaseUserCache();
		if (StringUtils.isNotEmpty(keyName)) {
			dbCache.setKeyName(keyName);
		}

		dbCache.setDataSource(getDataSource(dsName));

		return dbCache;
	}

	private DataSource getDataSource(final String dsName) {
		InitialContext ctx;
		DataSource ds = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup(dsName);
		} catch (NoInitialContextException e) {
			LOGGER.warn("No InitialContext found, probably not running in a container, so ignoring");
		} catch (NamingException e) {
			throw new RuntimeException("Could not find datasource for clustered db cache under key " + dsName, e);
		}
		return ds;
	}

//	/**
//	 * Configures a new protocol, httpssso, which sends the client certificate out with the request.
//	 */
//	@SuppressWarnings("deprecation")
//	private void setupHttpsProtocol(final String websignonLoginUrl, final String shireKeystoreLoc, final String shireKeystorePass,
//			final String cacertsKeystoreLoc, final String cacertsKeystorePass) {
//		try {
//			//URL websignonUrl = new URL(websignonLoginUrl);
//			final int standardHttpsPort = 443;
//		
//			URL truststoreUrl = (cacertsKeystoreLoc == null)?null : new URL(cacertsKeystoreLoc);
//			Protocol authhttps = new Protocol(AttributeAuthorityResponseFetcher.ALTERNATE_PROTOCOL, new AuthSSLProtocolSocketFactory(
//					getConfig().getAuthenticationDetails(), 
//					truststoreUrl, cacertsKeystorePass), standardHttpsPort);
//			Protocol.registerProtocol(AttributeAuthorityResponseFetcher.ALTERNATE_PROTOCOL, authhttps);
//		} catch (MalformedURLException e) {
//			throw new RuntimeException("Could not setup SSL protocols", e);
//		}
//	}

	public final void contextDestroyed(final ServletContextEvent arg0) {
		// do nothing
	}

}
