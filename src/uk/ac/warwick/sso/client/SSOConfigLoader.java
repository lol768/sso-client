/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import uk.ac.warwick.sso.client.cache.UserCache;

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

	}

	public final void contextDestroyed(final ServletContextEvent arg0) {
		// do nothing
	}

}
