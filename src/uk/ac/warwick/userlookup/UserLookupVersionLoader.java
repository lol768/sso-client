/*
 * Created on 8 Jun 2007
 *
 */
package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class UserLookupVersionLoader {

	private static final Logger LOGGER = Logger.getLogger(UserLookupVersionLoader.class);

	private static String version;
	
	private UserLookupVersionLoader() {
		// hidden constructor
	}

	/**
	 * Lazy-reads the current UserLookup version from a properties file embedded in the JAR.
	 */
	public static String getVersion() {
		if (version == null) {
			try {
				InputStream resourceAsStream = UserLookupVersionLoader.class.getResourceAsStream("/ssoclient.version");
				Properties props = new Properties();
				props.load(resourceAsStream);
				version = props.getProperty("version");
			} catch (IOException e1) {
				LOGGER.warn("Couldn't find userlookup version, returning empty string");
				version = "";
			}
		}

		return version;
	}

}
