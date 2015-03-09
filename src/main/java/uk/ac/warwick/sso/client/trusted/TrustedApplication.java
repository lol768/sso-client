package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;

public interface TrustedApplication extends Application {

    String HEADER_PREFIX = "X-Trusted-App-";
    String HEADER_PROVIDER_ID = HEADER_PREFIX + "ProviderID";
    String HEADER_CERTIFICATE = HEADER_PREFIX + "Cert";
    String HEADER_SECRET_KEY = HEADER_PREFIX + "Key";
    String HEADER_SIGNATURE = HEADER_PREFIX + "Signature";

    /**
     * This method decodes and validates the received certificate.
     *
     * @param certificate - certificate string claiming to have come from this application
     *
     * @return {@link ApplicationCertificate} object if validation succeeds
     *
     * @throws InvalidCertificateException - if either decryption or validation fails
     */
    ApplicationCertificate decode(EncryptedCertificate certificate, HttpServletRequest request) throws InvalidCertificateException;

    /**
     * Verify a received signature.
     * @param timestamp the timestamp from the incoming request, forms part of the signature base string.
     * @param requestUrl the request URL from the incoming request, forms part of the signature base string.
     * @param username the username from the incoming request, forms part of the signature base string.
     * @param signature the signature from the incoming request to verify.
     * @return true if the signature is verified, false otherwise.
     * @throws SignatureVerificationFailedException
     */
    boolean verifySignature(DateTime timestamp, String requestUrl, String username, String signature) throws SignatureVerificationFailedException;

}
