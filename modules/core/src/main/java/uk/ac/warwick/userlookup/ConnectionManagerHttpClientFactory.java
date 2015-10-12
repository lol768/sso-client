package uk.ac.warwick.userlookup;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

import java.net.ProxySelector;
import java.nio.charset.Charset;

/**
 * HttpClientFactory that, by default, will create its own multithreaded connection
 * manager and use that to manage a single HttpClient object. You can also
 * provide a different connection manager to use - maybe you already have one in your app.
 */
class ConnectionManagerHttpClientFactory implements HttpClientFactory {

    public static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(30000) // 30 seconds
            .setSocketTimeout(30000) // 30 seconds
            .setExpectContinueEnabled(true)
            .setCircularRedirectsAllowed(true)
            .setRedirectsEnabled(true)
            .setMaxRedirects(10)
            .build();

    private static final ConnectionManagerHttpClientFactory INSTANCE = new ConnectionManagerHttpClientFactory();

    private final HttpClient client;

    public ConnectionManagerHttpClientFactory(HttpClientConnectionManager connectionManager) {
        this.client = init(connectionManager);
    }

    public ConnectionManagerHttpClientFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // Set the default concurrent connections per route to 5
        connectionManager.setDefaultMaxPerRoute(5);

        this.client = init(connectionManager);
    }

    private HttpClient init(HttpClientConnectionManager connectionManager) {
        ConnectionConfig connectionConfig =
            ConnectionConfig.custom()
                .setBufferSize(8192)
                .setCharset(Charset.forName("UTF-8"))
                .build();

        SocketConfig socketConfig =
            SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        return
            HttpClientBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
                .setDefaultSocketConfig(socketConfig)
                .setConnectionManager(connectionManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(1, false)) // Retry each request once
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .build();
    }

    public HttpClient getHttpClient() {
        return client;
    }

    public static ConnectionManagerHttpClientFactory getInstance() {
        return INSTANCE;
    }

}
