package uk.ac.warwick.sso.client.oauth;

import static java.lang.Integer.*;
import static uk.ac.warwick.userlookup.UserLookup.*;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.xml.security.signature.XMLSignature;

import uk.ac.warwick.sso.client.SSOClientVersionLoader;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.SSOException;
import uk.ac.warwick.sso.client.oauth.OAuthToken.Type;
import uk.ac.warwick.sso.client.util.ImmediateFuture;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.HttpPool;
import uk.ac.warwick.util.cache.*;

public final class OAuthServiceImpl implements TrustedOAuthService {
    
    public static final String CONSUMER_CACHE_NAME = "OAuthConsumerCache";
    
    public static final String TOKEN_CACHE_NAME = "OAuthTokenCache";

    private static final Log LOGGER = LogFactory.getLog(OAuthServiceImpl.class);

    private SSOConfiguration _config;

    private String _version;

    //private Protocol protocol;
    
    // access/disabled tokens only
    private final Cache<String, OAuthConsumer> consumerCache
        = Caches.newCache(CONSUMER_CACHE_NAME, new OAuthConsumerEntryFactory(), parseInt(getConfigProperty("ssoclient.oauth.cache.consumer.timeout.secs")), Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));
    private final Cache<String, OAuthToken> tokenCache
        = Caches.newCache(TOKEN_CACHE_NAME, new OAuthTokenEntryFactory(), parseInt(getConfigProperty("ssoclient.oauth.cache.token.timeout.secs")), Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));

    protected OAuthServiceImpl() {
    }

    public OAuthServiceImpl(final SSOConfiguration config) {
        org.apache.xml.security.Init.init();
        
        _config = config;
        _version = SSOClientVersionLoader.getVersion();
    }

    @SuppressWarnings("deprecation")
    private Map<String, String> getResponse(OAuthServiceRequest request) throws SSOException {
        String location = getConfig().getString("oauth.service.location");

        LOGGER.info("Connecting to " + location);
        HttpClient client = HttpPool.getHttpClient();
        HttpPost method = new HttpPost(location);

        method.setHeader("User-Agent", HttpMethodWebService.getUserAgent(_version));
        method.setHeader("Content-Type", "text/xml");
        
        String fullRequest = signedRequest(request);
        method.setEntity(new StringEntity(fullRequest, ContentType.TEXT_XML));

        LOGGER.debug("Request:" + fullRequest);
        OAuthServiceResponse response;
        HttpResponse httpResponse = null;
        try {
            httpResponse = client.execute(method);
            response = OAuthServiceResponse.fromXML(httpResponse);
        } catch (IOException e) {
            LOGGER.error(location + " request failed at client.executeMethod", e);
            throw new SSOException(location + " request failed at client.executeMethod", e);
        } finally {
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }

        LOGGER.debug("Https response:" + response);
        
        if (response.getAttributes().containsKey(OAuthServiceRequest.ERROR_KEY)) {
            throw new SSOException("Error performing request; error is " + response.getAttributes().get(OAuthServiceRequest.ERROR_KEY));
        }

        // Sanity check
        if (!request.getVerb().equals(response.getVerb())) {
            throw new SSOException(String.format("Request verb %s didn't match response verb %s", request.getVerb(), response.getVerb()));
        }
        
        if (!request.getResource().equals(response.getResource())) {
            throw new SSOException(String.format("Request resource %s didn't match response verb %s", request.getResource(), response.getResource()));
        }
        
        return response.getAttributes();
    }

    private String signedRequest(final OAuthServiceRequest request) {
        List<Certificate> certChain = Arrays.asList( _config.getAuthenticationDetails().getCertificates() );
        PrivateKey key = _config.getAuthenticationDetails().getKey();
        
        return request.toSignedXML(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, key, certChain);
    }

    public Future<OAuthToken> store(OAuthToken token) {
        try {
            OAuthServiceRequest request = new OAuthServiceRequest.SaveTokenRequest(token, _config.getString("shire.providerid"));

            LOGGER.info("Trying to save token:" + request);

            Map<String, String> attributes = getResponse(request);
            return ImmediateFuture.of(OAuthToken.fromMap(attributes));
        } catch (SSOException e) {
            LOGGER.error("Could not get token details from token", e);
            throw new IllegalStateException("Couldn't save token", e);
        }
    }

    public Future<OAuthConsumer> getConsumerByConsumerKey(String consumerKey) {
        try {
            return ImmediateFuture.of(consumerCache.get(consumerKey));
        } catch (CacheEntryUpdateException e) {
            LOGGER.error("Couldn't get consumer from key " + consumerKey, e.getCause());
        }
        
        return ImmediateFuture.of(null);
    }

    public Future<OAuthToken> getToken(String tokenString) {
        try {
            return ImmediateFuture.of(tokenCache.get(tokenString));
        } catch (CacheEntryUpdateException e) {
            LOGGER.error("Couldn't get token for token string " + tokenString, e.getCause());
        }
        
        return ImmediateFuture.of(null);
    }

    private OAuthServiceProvider generateServiceProvider() {
        return new OAuthServiceProvider(getConfig().getString("oauth.requesttoken.location"), getConfig().getString(
                "oauth.authorise.location"), getConfig().getString("oauth.accesstoken.location"));
    }

    public final SSOConfiguration getConfig() {
        return _config;
    }

    public final void setConfig(final SSOConfiguration config) {
        _config = config;
    }
    
    private class OAuthConsumerEntryFactory extends SingularCacheEntryFactory<String, OAuthConsumer> {

        public OAuthConsumer create(String consumerKey) throws CacheEntryUpdateException {
            try {
                OAuthServiceRequest request = new OAuthServiceRequest.GetConsumerRequest(consumerKey, _config.getString("shire.providerid"));

                LOGGER.info("Trying to get consumer from key:" + request);

                Map<String, String> attributes = getResponse(request);
                
                OAuthConsumer consumer = null;
                if (attributes.containsKey("consumer_key") && attributes.containsKey("consumer_secret")) {
                    if (attributes.get("key_type").equals("RSA_PRIVATE")) {
                        consumer = new OAuthConsumer(attributes.get("callback_url"), attributes.get("consumer_key"), null, generateServiceProvider());
                        
                        // The oauth.net java code has lots of magic. By setting this
                        // property here, code thousands of lines away knows that the
                        // consumerSecret value in the consumer should be treated as an RSA
                        // private key and not an HMAC key.
                        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
                        consumer.setProperty(RSA_SHA1.PUBLIC_KEY, attributes.get("consumer_secret"));
                    } else {
                        consumer = new OAuthConsumer(attributes.get("callback_url"), attributes.get("consumer_key"), attributes.get("consumer_secret"), generateServiceProvider());
                        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
                    }
                    
                    consumer.setProperty("title", attributes.get("title"));
                    consumer.setProperty("trusted", Boolean.valueOf(attributes.get("trusted")));
                    consumer.setProperty("description", attributes.get("description"));
                    consumer.setProperty("technical_contact", attributes.get("technical_contact"));
                    
                    return consumer;
                }
                
                return null;
            } catch (SSOException e) {
                throw new CacheEntryUpdateException(e);
            }
        }

        public boolean shouldBeCached(OAuthConsumer consumer) {
            return true;
        }
        
    };
    
    private class OAuthTokenEntryFactory extends SingularCacheEntryFactory<String, OAuthToken> {

        public OAuthToken create(String tokenString) throws CacheEntryUpdateException {
            try {
                OAuthServiceRequest request = new OAuthServiceRequest.GetTokenRequest(tokenString, _config.getString("shire.providerid"));
    
                LOGGER.info("Trying to get token details from token:" + request);
    
                Map<String, String> attributes = getResponse(request);
    
                OAuthToken token = OAuthToken.fromMap(attributes);
    
                return token;
            } catch (SSOException e) {
                throw new CacheEntryUpdateException(e);
            }
        }

        public boolean shouldBeCached(OAuthToken token) {
            return token != null && token.getType() != Type.REQUEST;
        }
        
    };

}
