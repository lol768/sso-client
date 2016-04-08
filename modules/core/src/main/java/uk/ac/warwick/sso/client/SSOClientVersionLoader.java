/*
 * Created on 8 Jun 2007
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SSOClientVersionLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(SSOClientVersionLoader.class);

	private SSOClientVersionLoader() {
		// hidden constructor
	}

	public static String getVersion() {

		try {
			InputStream resourceAsStream = SSOClientVersionLoader.class.getResourceAsStream("/ssoclient.version");
			Properties props = new Properties();
			props.load(resourceAsStream);
			return props.getProperty("version");
		} catch (IOException e1) {
			LOGGER.debug("Couldn't find ssoclient version");
		}

		return "";

	}

}
