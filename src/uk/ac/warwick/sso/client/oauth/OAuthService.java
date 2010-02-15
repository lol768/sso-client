package uk.ac.warwick.sso.client.oauth;

import java.util.concurrent.Future;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

/**
 * Service to fetch and post information to websignon's OAuth database.
 * <p>
 * Most of the methods in here are deprecated; only {@link #getToken(String)}
 * will be allowed in future.
 */
public interface OAuthService {

    Future<OAuthToken> getToken(String token);
    
    /**
     * @deprecated See SSO-840
     */
    Future<OAuthToken> generateRequestToken(OAuthToken token);

    /**
     * @deprecated See SSO-840
     */
    Future<OAuthConsumer> getConsumerByConsumerKey(String consumerKey);

    /**
     * @deprecated See SSO-840
     */
    Future<OAuthServiceProvider> getServiceProvider();

    /**
     * @deprecated See SSO-840
     */
    Future<OAuthToken> store(OAuthToken token);

}
