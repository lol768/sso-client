package uk.ac.warwick.sso.client;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.LoggerFactory;

public class BasicAuthLoggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthLoggingService.class);
    private final String postPath = SSOConfiguration.getConfig().getString("ssoclient.basicauthlog.api");
    private final CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
            .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                            .setBufferSize(8192)
                            .setCharset(StandardCharsets.UTF_8)
                            .build()
            )
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(5000) // 5 seconds
                            .setSocketTimeout(5000) // 5 seconds
                            .setExpectContinueEnabled(true)
                            .setRedirectsEnabled(false)
                            .build()
            )
            .setMaxConnPerRoute(5) // Only allow 5 connections per host
            .build();

    public BasicAuthLoggingService() {
        this.httpClient.start();
    }

    public CompletableFuture<Response> log(String userCode, String remoteIp, String userAgent) throws MalformedURLException {
        StringEntity entity = makeEntity(userCode, remoteIp, userAgent);
        HttpPost request = makeRequest(entity);
        CompletableFuture<Response> completableFuture = new CompletableFuture<>();
        httpClient.execute(request, makeFuture(completableFuture));
        return completableFuture;
    }

    public FutureCallback<HttpResponse> makeFuture(CompletableFuture<Response> completableFuture) {
        return new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                LOGGER.debug("BasicAuth logging request to SSO server completed.");
                completableFuture.complete(new Response(httpResponse.getStatusLine().getStatusCode()));
            }

            @Override
            public void failed(Exception e) {
                LOGGER.error("BasicAuth logging request failed with error: " + e.getMessage());
                completableFuture.complete(new Response(500, e.getMessage()));
            }

            @Override
            public void cancelled() {
                LOGGER.debug("BasicAuth logging request has been canceled.");
                completableFuture.complete(new Response(500, "Request has been canceled"));
            }
        };
    }

    public HttpPost makeRequest(StringEntity entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        final HttpPost request = new HttpPost(postPath);
        request.addHeader(
                "cache-control",
                "no-cache");
        request.addHeader(
                "Content-type",
                "application/x-www-form-urlencoded");
        request.addHeader(
                "User-Agent",
                this.getClass().getName());
        request.setEntity(entity);
        return request;
    }

    public StringEntity makeEntity(String userCode, String remoteIp, String userAgent) {
        return new StringEntity("userCode=" + userCode + "&remoteIp=" + remoteIp + "&userAgent=" + userAgent, StandardCharsets.UTF_8);
    }
}

class Response {
    private String statusCode;
    private String error;

    public Response(String statusCode) {
        this.statusCode = statusCode;
    }

    public Response(int statusCode) {
        this(Integer.toString(statusCode));
    }

    public Response(String statusCode, String error) {
        this.statusCode = statusCode;
        this.error = error;
    }

    public Response(int statusCode, String error) {
        this(Integer.toString(statusCode), error);
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
