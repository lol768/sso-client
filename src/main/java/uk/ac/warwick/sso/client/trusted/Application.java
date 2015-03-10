package uk.ac.warwick.sso.client.trusted;

import java.security.PublicKey;

public interface Application {

    /**
     * Public key of this application.
     */
    PublicKey getPublicKey();

    /**
     * @return Provider ID representing this application
     */
    String getProviderID();

}
