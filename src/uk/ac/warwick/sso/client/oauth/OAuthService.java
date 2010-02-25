package uk.ac.warwick.sso.client.oauth;

import java.util.concurrent.Future;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

/**
 * Service to fetch and post information to websignon's OAuth database.
 * <p>
 * Most of the methods in here are deprecated; only {@link #getToken(String)} is
 * allowed for the vast majority of applications.
 */
public interface OAuthService {

    /**
     * Returns the {@link OAuthToken} specified by this token string. This can
     * either be a Request token or an Access token and may be marked as Invalid
     * or Expired.
     * <p>
     * It is important that the application checks {@link OAuthToken#getType()}
     * to query the {@link OAuthToken.Type} of this token. Only
     * {@link OAuthToken.Type#ACCESS} tokens are permitted to access resources
     * on behalf of the user.
     * 
     * @see OAuthFilter
     */
    Future<OAuthToken> getToken(String token);

    /**
     * Return the {@link OAuthConsumer} that is referred to be the unique
     * consumerKey that we have been passed.
     * <p>
     * This {@link OAuthConsumer} will have a special property "trusted" that
     * can be queried to check whether the consumer is trusted by Websignon - if
     * this is the case then the consumer is allowed to generate access tokens
     * programatically without explicit user authorisation.
     */
    Future<OAuthConsumer> getConsumerByConsumerKey(String consumerKey);

    /**
     * Store the {@link OAuthToken} in Websignon for access later via
     * {@link #getToken(String)}.
     */
    Future<OAuthToken> store(OAuthToken token);

    /**
     * @deprecated See SSO-840
     */
    Future<OAuthToken> generateRequestToken(OAuthToken token);

    /**
     * @deprecated See SSO-840
     */
    Future<OAuthServiceProvider> getServiceProvider();

}
