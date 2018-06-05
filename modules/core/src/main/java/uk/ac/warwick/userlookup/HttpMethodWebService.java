package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.util.core.StringUtils;
import uk.ac.warwick.util.web.Uri;
import uk.ac.warwick.util.web.UriBuilder;

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
	static final Logger LOGGER = LoggerFactory.getLogger(HttpMethodWebService.class);

	private final HttpMethodFactory _methodFactory;

	private final WebServiceTimeoutConfig _timeoutConfig;

	private String _version;
	
	private String _apiKey;
	
	private static String userAgentString;

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
	
	public static final String getUserAgent(String version) {
		if (userAgentString == null) {
			StringBuilder sb = new StringBuilder("SSOClient");
	        if (version != null) {
	            sb.append(" ").append(version);
	        }
	        SSOConfiguration config = SSOConfiguration.getConfig();
			if (config != null) {
				String providerId = config.getString("shire.providerid");
				if (providerId != null) {
					sb.append(" (providerId=").append(providerId).append(")");
				}
			}
			userAgentString = sb.toString();
		}
		return userAgentString;
	}

	@SuppressWarnings("deprecation")
	public void doRequest(final Map<String, Object> parameters, final WebServiceResponseHandler responseHandler) throws WebServiceException,
			HandlerException {
		HttpClient client = HttpPool.getHttpClient();
		if (_timeoutConfig == null) {
			throw new RuntimeException("Must set a WebServiceTimeoutConfig");
		}

        RequestConfig.Builder config =
            RequestConfig.copy(ConnectionManagerHttpClientFactory.DEFAULT_REQUEST_CONFIG)
                .setConnectionRequestTimeout(_timeoutConfig.getConnectionTimeout())
                .setConnectTimeout(_timeoutConfig.getConnectionTimeout())
                .setSocketTimeout(_timeoutConfig.getDataTimeout());

        HttpRequestBase request = _methodFactory.getMethod(_location);
        request.setConfig(config.build());

		request.setHeader("User-Agent", getUserAgent(_version));

		addApiKeyToUrl(request);

		if (parameters.containsKey("requestType")) {
			try {
				UriBuilder builder = new UriBuilder(Uri.fromJavaUri(_location.toURI()));
				builder.addQueryParameter("requestType", parameters.remove("requestType").toString());
				request.setURI(builder.toUri().toJavaUri());
			} catch (URISyntaxException e) {
				LOGGER.warn("Exception when adding query parameter to sso url", e);
			}
		}

		_methodFactory.setMethodParams(request, parameters);
		LOGGER.debug("Connecting to WebService on " + _location.toExternalForm());

        HttpResponse response = null;
		try {
            response = client.execute(request);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Got back response from SSO with status=" + response.getStatusLine().getStatusCode());
			}

			if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("response code " + response.getStatusLine().getStatusCode());
			}
			
			responseHandler.processResults(response);
			responseHandler.processClearGroupHeader(response);
		} catch (IOException e) {
			// Could be ProtocolException or IOException but I don't care which
			LOGGER.error("Error setting up web request for url: " + _location.toExternalForm(), e);
			throw new WebServiceException("Error setting up web request: " + _location.toExternalForm(), e);
		} finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
		}
	}

	/**
	 * If an API key is set, append it to the query string.
	 */
	void addApiKeyToUrl(HttpRequestBase request) {
		if (StringUtils.hasText(_apiKey)) {
            UriBuilder builder = new UriBuilder(Uri.fromJavaUri(request.getURI()));
            builder.addQueryParameter(WSOS_API_KEY_PARAM, _apiKey);

			request.setURI(builder.toUri().toJavaUri());
		}
	}


	public interface WebServiceResponseHandler {

		void processResults(HttpResponse response) throws HandlerException;
		void processClearGroupHeader(HttpResponse response);

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

	interface HttpMethodFactory<T extends HttpRequestBase> {
		T getMethod(URL theUrl);

		void setMethodParams(T method, Map<String, Object> params);
	}

	public static class GetMethodFactory implements HttpMethodFactory<HttpGet> {

		public final HttpGet getMethod(final URL url) {
			return new HttpGet(url.toExternalForm());
		}

		public final void setMethodParams(final HttpGet get, final Map<String, Object> params) {
            UriBuilder builder = new UriBuilder(Uri.fromJavaUri(get.getURI()));

			String queryString = builder.getQuery();
			if (StringUtils.hasText(queryString)) {
				throw new RuntimeException("Can't handle get requests with preset query strings!");
			}

			for (Entry<String, Object> entry : params.entrySet()) {
				if (entry.getValue() instanceof Iterable) {
					for (String value : (Iterable<String>)entry.getValue()) {
						builder.addQueryParameter(entry.getKey(), value);
					}
				} else {
					builder.addQueryParameter(entry.getKey(), entry.getValue().toString());
				}
			}

			get.setURI(builder.toUri().toJavaUri());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Building GET request: query string is" + builder.getQuery());
			}
		}
	}

	public static class PostMethodFactory implements HttpMethodFactory<HttpPost> {

		public final HttpPost getMethod(final URL theUrl) {
			return new HttpPost(theUrl.toExternalForm());
		}

		public final void setMethodParams(final HttpPost post, final Map<String,Object> params) {
            List<NameValuePair> nvps = new ArrayList<>();

			for (Entry<String,Object> entry : params.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (Iterable.class.isInstance(value)) {
					for (Object o : (Iterable<?>)value) {
                        nvps.add(new BasicNameValuePair(key, o.toString()));
					}
				} else {
                    nvps.add(new BasicNameValuePair(key, (String)value));
				}
			}

            try {
                post.setEntity(new UrlEncodedFormEntity(nvps));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
		}
	}
}