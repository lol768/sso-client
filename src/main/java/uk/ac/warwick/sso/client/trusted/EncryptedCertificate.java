package uk.ac.warwick.sso.client.trusted;

import java.io.Serializable;

/**
 * Contains the encoded certificate information to be included in the trusted requests between applications. The
 * provided information to be set in the request header using the following parameters:
 * {@link TrustedApplication#HEADER_PROVIDER_ID}
 * {@link TrustedApplication#HEADER_CERTIFICATE}
 */
public interface EncryptedCertificate extends Serializable {

    /**
     * Provider ID of the trusted application that encrypted this certificate
     */
    String getProviderID();

    /**
     * String that contains three lines:
     * <p>
     * Encrypted with the secret key and Base64 encoded
     */
    String getCertificate();

    /**
     * A Base64-encoded signature, of a timestamp and the URL this request is intended for,
     * signed with the private key of the client trusted application.
     */
    String getSignature();

}
