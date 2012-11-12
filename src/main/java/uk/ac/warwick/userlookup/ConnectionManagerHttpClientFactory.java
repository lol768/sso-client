package uk.ac.warwick.userlookup;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * HttpClientFactory that, by default, will create its own multithreaded connection
 * manager and use that to manage a single HttpClient object. You can also
 * provide a different connection manager to use - maybe you already have one in your app.
 */
class ConnectionManagerHttpClientFactory implements HttpClientFactory {
	private HttpConnectionManager httpConnectionManager;
	private HttpClient client;
	
	/**
	 * Creates a factory using a provided {@link HttpConnectionManager} - it
	 * MUST be multi-threaded as this factory reuses a single HttpClient object.
	 */
	public ConnectionManagerHttpClientFactory(HttpConnectionManager manager) {
		httpConnectionManager = manager;
	}
	
	public ConnectionManagerHttpClientFactory() {
		MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
		manager.getParams().setMaxTotalConnections(50);
		manager.getParams().setDefaultMaxConnectionsPerHost(20);
		httpConnectionManager = manager;
	}
	
	public HttpClient getHttpClient() {
		if (client == null) {
			client = new HttpClient(getHttpConnectionManager());
		}
		return client;
	}
	
	private final HttpConnectionManager getHttpConnectionManager() {
		return httpConnectionManager;
	}
}
