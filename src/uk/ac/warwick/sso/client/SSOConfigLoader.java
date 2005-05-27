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

/**
 * Requires a ServletContext Parameter to be set
 * "ssoclient.config=/sso-config.xml"
 * 
 * @author Kieran Shaw
 * 
 */
public class SSOConfigLoader implements ServletContextListener {

	public static final String SSO_CONFIG_KEY = "SSO-CONFIG";

	public SSOConfigLoader() {
		super();
	}

	public void contextInitialized(ServletContextEvent event) {

		XMLConfiguration config;
		try {
			URL configUrl = getClass().getResource(event.getServletContext().getInitParameter("ssoclient.config"));
			config = new XMLConfiguration(new File(configUrl.getFile()));
		} catch (ConfigurationException e) {
			throw new RuntimeException("Could not setup configuration", e);
		}

		event.getServletContext().setAttribute(SSO_CONFIG_KEY, config);
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		// do nothing
	}

}
