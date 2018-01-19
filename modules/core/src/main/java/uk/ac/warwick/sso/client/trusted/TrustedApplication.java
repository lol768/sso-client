package uk.ac.warwick.sso.client.trusted;

import uk.ac.warwick.sso.client.core.HttpRequest;

import java.time.Duration;
import java.time.ZonedDateTime;

public interface TrustedApplication extends Application {

    String HEADER_PREFIX = "X-Trusted-App-";

    // Request headers
    String HEADER_PROVIDER_ID = HEADER_PREFIX + "ProviderID";
    String HEADER_CERTIFICATE = HEADER_PREFIX + "Cert";
    String HEADER_SIGNATURE = HEADER_PREFIX + "Signature";

    // Response headers
    String HEADER_STATUS = HEADER_PREFIX + "Status";
    String HEADER_ERROR_CODE = HEADER_PREFIX + "Error-Code";
    String HEADER_ERROR_MESSAGE = HEADER_PREFIX + "Error-Message";

    enum Status {
        OK, Error
    }

    Duration CERTIFICATE_TIMEOUT = Duration.ofMinutes(5);

    /**
     * This method decodes and validates the received certificate.
     *
     * @param certificate - certificate string claiming to have come from this application
     *
     * @return {@link ApplicationCertificate} object if validation succeeds
     *
     * @throws InvalidCertificateException - if either decryption or validation fails
     */
    ApplicationCertificate decode(EncryptedCertificate certificate, HttpRequest request) throws InvalidCertificateException;

    /**
     * Verify a received signature.
     * @param timestamp the timestamp from the incoming request, forms part of the signature base string.
     * @param requestUrl the request URL from the incoming request, forms part of the signature base string.
     * @param username the username from the incoming request, forms part of the signature base string.
     * @param signature the signature from the incoming request to verify.
     * @return true if the signature is verified, false otherwise.
     * @throws SignatureVerificationFailedException
     */
    boolean verifySignature(ZonedDateTime timestamp, String requestUrl, String username, String signature) throws SignatureVerificationFailedException;

}
