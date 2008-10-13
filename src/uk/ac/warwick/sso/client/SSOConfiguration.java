/*
 * Created on 02-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;

/**
 * Holder for a Thread-local SSOConfiguration.
 * 
 * These methods could be static.
 */
public class SSOConfiguration {

	private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

	public static final Configuration getConfig() {
		return (Configuration) THREAD_LOCAL.get();
	}

	public static final void setConfig(final Configuration config) {
		THREAD_LOCAL.set(config);
	}

}
