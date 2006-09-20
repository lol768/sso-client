/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;

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

    static void loadSSOConfig(ServletContext servletContext) {
        Enumeration params = servletContext.getInitParameterNames();
		while (params.hasMoreElements()) {
			String paramName = (String) params.nextElement();
			if (paramName.startsWith("ssoclient.config")) {

				String ssoConfigLocation = servletContext.getInitParameter(paramName);
				LOGGER.info("Found context param " + paramName + "=" + ssoConfigLocation);
				if (ssoConfigLocation == null) {
					LOGGER.warn("Could not find ssoclient.config context param");
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

				String configSuffix = paramName.replaceFirst("ssoclient.config", "");
				LOGGER.info("Using suffix for config:" + configSuffix);

				setupHttpsProtocol(config.getString("shire.keystore.location"), config.getString("shire.keystore.password"),
						config.getString("cacertskeystore.location"), config.getString("cacertskeystore.password"));

				servletContext.setAttribute(SSO_CONFIG_KEY + configSuffix, config);

				servletContext.setAttribute(SSO_CACHE_KEY + configSuffix, getCache());

			}
		}
    }

	protected static UserCache getCache() {
		return new InMemoryUserCache();
	}

	private static void setupHttpsProtocol(final String shireKeystoreLoc, final String shireKeystorePass,
			final String cacertsKeystoreLoc, final String cacertsKeystorePass) {
		final int standardHttpsPort = 443;
		try {
			Protocol authhttps = new Protocol("https", new AuthSSLProtocolSocketFactory(new URL(shireKeystoreLoc),
					shireKeystorePass, new URL(cacertsKeystoreLoc), cacertsKeystorePass), standardHttpsPort);
			Protocol.registerProtocol("https", authhttps);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Could not setup SSL protocols", e);
		}
	}

	public final void contextDestroyed(final ServletContextEvent arg0) {
		// do nothing
	}

}
