/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.File;
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
 * Requires a ServletContext Parameter to be set "ssoclient.config=/sso-config.xml"
 * 
 * @author Kieran Shaw
 * 
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

	public XMLConfiguration loadSSOConfig(final String ssoConfigLocation) {

		if (ssoConfigLocation == null) {
			LOGGER.warn("No ssoConfigLocation given");
			throw new RuntimeException("Could not setup configuration");
		}
		URL configUrl = SSOConfigLoader.class.getResource(ssoConfigLocation);
		if (configUrl == null) {
			LOGGER.warn("Could not find config as path is null");
			throw new RuntimeException("Could not setup configuration");
		}

		XMLConfiguration config;
		try {
			config = new XMLConfiguration(new File(configUrl.getFile()));
		} catch (ConfigurationException e) {
			throw new RuntimeException("Could not setup configuration", e);
		}

		if (shouldUseKeystore(config)) {
			setupHttpsProtocol(config.getString("shire.keystore.location"), config.getString("shire.keystore.password"), config
					.getString("cacertskeystore.location"), config.getString("cacertskeystore.password"));
		}
		
		return config;

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
		Enumeration params = servletContext.getInitParameterNames();
		while (params.hasMoreElements()) {
			String paramName = (String) params.nextElement();
			if (paramName.startsWith("ssoclient.config")) {

				String ssoConfigLocation = servletContext.getInitParameter(paramName);

				XMLConfiguration config = loadSSOConfig(ssoConfigLocation);

				String configSuffix = paramName.replaceFirst("ssoclient.config", "");
				LOGGER.info("Using suffix for config:" + configSuffix);

				servletContext.setAttribute(SSO_CONFIG_KEY + configSuffix, config);

				UserCache cache = getCache(config);
				servletContext.setAttribute(SSO_CACHE_KEY + configSuffix, cache);

			}
		}
	}

	private UserCache getCache(XMLConfiguration config) {

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

	/**
	 * Configures a new protocol, https+sso, which sends the client certificate out with the request.
	 */
	private void setupHttpsProtocol(final String shireKeystoreLoc, final String shireKeystorePass,
			final String cacertsKeystoreLoc, final String cacertsKeystorePass) {
		final int standardHttpsPort = 443;
		try {
			Protocol authhttps = new Protocol("https", new AuthSSLProtocolSocketFactory(new URL(shireKeystoreLoc),
					shireKeystorePass, new URL(cacertsKeystoreLoc), cacertsKeystorePass), standardHttpsPort);
			Protocol.registerProtocol("https+sso", authhttps);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Could not setup SSL protocols", e);
		}
	}

	public final void contextDestroyed(final ServletContextEvent arg0) {
		// do nothing
	}

}
