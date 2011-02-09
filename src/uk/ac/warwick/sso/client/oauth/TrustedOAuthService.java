package uk.ac.warwick.sso.client.oauth;

import java.util.concurrent.Future;

import net.oauth.OAuthConsumer;

public interface TrustedOAuthService extends OAuthService {

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

}