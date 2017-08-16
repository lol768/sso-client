package uk.ac.warwick.sso.client;

import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BasicAuthLoggingServiceTest {
    @Test
    public void makeRequest() throws Exception {
        HttpPost result = BasicAuthLoggingService.makeRequest(
                "theUserCode.123",
                "123.123.123.123",
                "super-agent",
                "https://example.fake");

        Assert.assertEquals("POST", result.getMethod());
        Assert.assertEquals("https://example.fake", result.getURI().toString());

        InputStream resultStream = result.getEntity().getContent();
        byte[] bytes = new byte[resultStream.available()];
        resultStream.read(bytes, 0, resultStream.available());
        String contentString = new String(bytes, StandardCharsets.UTF_8);
        Assert.assertEquals("userCode=theUserCode.123&remoteIp=123.123.123.123&userAgent=super-agent", contentString);
    }

}