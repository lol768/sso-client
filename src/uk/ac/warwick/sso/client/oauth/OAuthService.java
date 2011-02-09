package uk.ac.warwick.sso.client.oauth;

import java.util.concurrent.Future;

/**
 * Service to fetch and post information to websignon's OAuth database.
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

}
