package uk.ac.warwick.userlookup;

import org.apache.commons.httpclient.HttpClient;

/**
 * Static factory that creates HttpClient instances for UserLookup.
 * The default is to use a multi-threaded connection manager that will
 * pool connections, but you can set your own HttpClientFactory that
 * can use whatever method you like (such as using a connection manager
 * you've already created, perhaps.)
 */
public final class HttpPool {
	
	private static HttpClientFactory factory = new ConnectionManagerHttpClientFactory();
	
	public static final HttpClient getHttpClient() {
		return factory.getHttpClient();
	}
	
	/**
	 * Replace the factory used to generate HttpClients. The default is a 
	 * {@link ConnectionManagerHttpClientFactory} - you can either replace it with
	 * another instance of this (with your own parameters), or you can do
	 * any implementation you like. Note that {@link ConnectionManagerHttpClientFactory}
	 * stores a single HttpClient instance, so you should only use a multithreaded
	 * connection manager with that.
	 */
	public static final void setHttpClientFactory(HttpClientFactory fact) {
		factory = fact;
	}
}
