/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;

/**
 * Requires a ServletContext Parameter to be set
 * "ssoclient.config=/sso-config.xml"
 * 
 * @author Kieran Shaw
 * 
 */
public class SSOConfigLoader implements ServletContextListener {

	public static final String SSO_CONFIG_KEY = "SSO-CONFIG";

	public static final String SSO_CACHE_KEY = "SSO-CACHE";

	public SSOConfigLoader() {
		super();
	}

	public final void contextInitialized(final ServletContextEvent event) {

		XMLConfiguration config;
		try {
			URL configUrl = getClass().getResource(event.getServletContext().getInitParameter("ssoclient.config"));
			config = new XMLConfiguration(new File(configUrl.getFile()));
		} catch (ConfigurationException e) {
			throw new RuntimeException("Could not setup configuration", e);
		}

		event.getServletContext().setAttribute(SSO_CONFIG_KEY, config);

		event.getServletContext().setAttribute(SSO_CACHE_KEY, new UserCache());
		
		final int standardHttpsPort = 443;
		try {
			Protocol authhttps = new Protocol("https", new AuthSSLProtocolSocketFactory(new URL(config
					.getString("shire.keystore.location")), config.getString("shire.keystore.password"), new URL(config
					.getString("cacertskeystore.location")), config.getString("cacertskeystore.password")), standardHttpsPort);
			Protocol.registerProtocol("https", authhttps);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Could not setup SSL protocols", e);
		}
		

	}

	public final void contextDestroyed(final ServletContextEvent arg0) {
		// do nothing
	}

}
