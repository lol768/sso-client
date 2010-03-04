package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Class that represents a web service accessed via an HTTP POST.
 * 
 * Will send an API key with each request if one is set.
 * 
 * @author cusaab
 */
public final class HttpMethodWebService {

	public static final String WSOS_API_KEY_PARAM = "wsos_api_key";
	
	private final URL _location;

	// package-scoped so that inner classes can see it
	static final Logger LOGGER = Logger.getLogger(HttpMethodWebService.class);

	private final HttpMethodFactory _methodFactory;

	private final WebServiceTimeoutConfig _timeoutConfig;

	private String _version;
	
	private String _apiKey;

	/**
	 * @param server
	 * @param factory
	 * @param timeoutConf
	 */
	public HttpMethodWebService(final URL serviceLocation, final HttpMethodFactory httpMethodFactory,
			final WebServiceTimeoutConfig timeoutConf, final String version, String apiKey) {
		this._methodFactory = httpMethodFactory;
		this._timeoutConfig = timeoutConf;
		if (serviceLocation == null) {
			throw new RuntimeException("No WebService URL was configured!");
		}
		this._location = serviceLocation;
		_version = version;
		_apiKey = apiKey;
	}

	@SuppressWarnings("deprecation")
	public void doRequest(final Map<String,Object> parameters, final WebServiceResponseHandler responseHandler) throws WebServiceException,
			HandlerException {
		HttpClient client = HttpPool.getHttpClient();
		if (_timeoutConfig == null) {
			throw new RuntimeException("Must set a WebServiceTimeoutConfig");
		}
		
		client.setConnectionTimeout(_timeoutConfig.getConnectionTimeout());
		client.setTimeout(_timeoutConfig.getDataTimeout());

		HttpMethodBase method = _methodFactory.getMethod(_location);
		//method.addRequestHeader("Connection", "close");

		if (_version == null) {
			method.addRequestHeader("User-Agent", "Userlookup");
		} else {
			method.addRequestHeader("User-Agent", "Userlookup " + _version);
		}
		
		addApiKeyToUrl(method);
		
		try {
			System.out.println("This is me calling " + method.getURI().toString());
		} catch (URIException e1) {
			System.out.println("Urgh");
		}
		
		_methodFactory.setMethodParams(method, parameters);
		LOGGER.debug("Connecting to WebService on " + _location.toExternalForm());
		try {
			int status = client.executeMethod(method);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Got back response from SSO with status=" + status);
			}
			if (status != HttpURLConnection.HTTP_OK) {
				throw new IOException("response code " + status);
			}
			
			responseHandler.processResults(method.getResponseBodyAsStream());

		} catch (IOException e) {
			// Could be ProtocolException or IOException but I don't care which
			LOGGER.error("Error setting up web request for url: " + _location.toExternalForm(), e);
			throw new WebServiceException("Error setting up web request: " + _location.toExternalForm(), e);
		} finally {
			method.releaseConnection();
		}
	}

	/**
	 * If an API key is set, append it to the query string.
	 */
	void addApiKeyToUrl(HttpMethodBase method) {
		if (_apiKey != null && !"".equals(_apiKey)) {
			String queryString = method.getQueryString();
			if (queryString == null) {
				queryString = "";
			}
			if (queryString.length() > 0) {
				queryString += "&";
			}
			queryString += WSOS_API_KEY_PARAM + "=" + _apiKey;
			method.setQueryString(queryString);
		}
	}


	public interface WebServiceResponseHandler {

		void processResults(InputStream fromServer) throws HandlerException;
	}

	public static class WebServiceException extends Exception {
		private static final long serialVersionUID = -6340006166324965152L;

		WebServiceException(final String error, final Throwable cause) {
			super(error, cause);
		}

		public WebServiceException(final String message) {
			super(message);
		}
	}

	public static class HandlerException extends Exception {
		private static final long serialVersionUID = -9010548155025318183L;

		public HandlerException(final String message) {
			super(message);
		}

		public HandlerException(final Throwable e) {
			super(e);
		}

		public HandlerException(String message, Throwable e) {
			super(message, e);
		}
	}

	interface HttpMethodFactory {

		HttpMethodBase getMethod(URL theUrl);

		void setMethodParams(HttpMethodBase method, Map<String,Object> params);
	}

	public static class GetMethodFactory implements HttpMethodFactory {

		public final HttpMethodBase getMethod(final URL url) {
			GetMethod meth = new GetMethod(url.toExternalForm());
			return meth;
		}

		public final void setMethodParams(final HttpMethodBase method, final Map<String,Object> params) {
			GetMethod get = (GetMethod) method;
			String queryString = get.getQueryString();
			if (queryString != null) {
				throw new RuntimeException("Can't handle get requests with preset query strings!");
			}
			StringBuilder query = new StringBuilder("");
			for (Entry<String,Object> entry : params.entrySet()) {
				query.append("&")
					.append(entry.getKey())
					.append("=")
					.append(entry.getValue());
			}
			queryString = query.toString().replaceFirst("&", "");
			if (queryString.equals("")) {
				// important! Otherwise Httpclient puts a blank querystring on
				// the end which means no caching!!!!
				queryString = null;
			}
			get.setQueryString(queryString);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Building GET request: query string is" + get.getQueryString());
			}
		}
	}

	public static class PostMethodFactory implements HttpMethodFactory {

		public final HttpMethodBase getMethod(final URL theUrl) {
			PostMethod postMethod = new PostMethod(theUrl.toExternalForm());
			return postMethod;
		}

		public final void setMethodParams(final HttpMethodBase method, final Map<String,Object> params) {
			PostMethod post = (PostMethod) method;
			for (Entry<String,Object> entry : params.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (Iterable.class.isInstance(value)) {
					for (Object o : (Iterable<?>)value) {
						post.addParameter(key, o.toString());
					}
				} else {
					post.addParameter(key, (String)value);
				}
			}

		}
	}
}