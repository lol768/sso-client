package uk.ac.warwick.sso.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.userlookup.UserLookup;

public class BasicAuthLoggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthLoggingService.class);
    private final String postPath = UserLookup.getConfigProperty("ssoclient.basicauthlog.api");
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

    public CompletableFuture<LoggingResponse> log(String userCode, String remoteIp, String userAgent) {
        HttpPost request = makeRequest(userCode, remoteIp, userAgent, postPath);
        CompletableFuture<LoggingResponse> completableFuture = new CompletableFuture<>();
        httpClient.execute(request, makeFuture(completableFuture));
        return completableFuture;
    }

    public FutureCallback<HttpResponse> makeFuture(CompletableFuture<LoggingResponse> completableFuture) {
        return new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                LOGGER.debug("BasicAuth logging request to SSO server completed.");
                completableFuture.complete(new LoggingResponse(httpResponse.getStatusLine().getStatusCode()));
            }

            @Override
            public void failed(Exception e) {
                LOGGER.error("BasicAuth logging request failed with error: " + e.getMessage());
                completableFuture.complete(new LoggingResponse(500, e.getMessage()));
            }

            @Override
            public void cancelled() {
                LOGGER.debug("BasicAuth logging request has been canceled.");
                completableFuture.complete(new LoggingResponse(500, "Request has been canceled"));
            }
        };
    }

    public static HttpPost makeRequest(
            String userCode,
            String remoteIp,
            String userAgent,
            String postPath) {
        final HttpPost request = new HttpPost(postPath);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("userCode", userCode));
        params.add(new BasicNameValuePair("remoteIp", remoteIp));
        params.add(new BasicNameValuePair("userAgent", userAgent));
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        request.addHeader(
                "cache-control",
                "no-cache");
        request.addHeader(
                "User-Agent",
                BasicAuthLoggingService.class.getName());
        return request;
    }
}

class LoggingResponse {
    private String statusCode;
    private String error;

    public LoggingResponse(String statusCode) {
        this.statusCode = statusCode;
    }

    public LoggingResponse(int statusCode) {
        this(Integer.toString(statusCode));
    }

    public LoggingResponse(String statusCode, String error) {
        this.statusCode = statusCode;
        this.error = error;
    }

    public LoggingResponse(int statusCode, String error) {
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
