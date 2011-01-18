package uk.ac.warwick.sso.client;


public class ConfigHelper {
	
	/**
	 * Gets a string configuration value, and throws an IllegalArgumentException
	 * if it is missing (or the value is otherwise null).
	 */
	public static String getRequiredString(SSOConfiguration config, String key) {
		String value = config.getString(key);
		if (value == null) {
			throw new IllegalArgumentException("Missing from SSO configuration: " + key);
		}
		return value;
	}
}
