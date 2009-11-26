package uk.ac.warwick.sso.client.oauth;

import java.util.concurrent.Future;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

/**
 * Service to fetch and post information to websignon's OAuth database.
 */
public interface OAuthService {
    
    Future<OAuthToken> generateRequestToken(OAuthToken token);

    Future<OAuthConsumer> getConsumerByConsumerKey(String consumerKey);

    Future<OAuthToken> getToken(String token);
    
    Future<OAuthServiceProvider> getServiceProvider();

    Future<OAuthToken> store(OAuthToken token);

}
