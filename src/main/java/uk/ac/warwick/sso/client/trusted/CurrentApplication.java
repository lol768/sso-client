package uk.ac.warwick.sso.client.trusted;

/**
 * Represents current application. This object is used when requesting privileged operations on other servers
 * from this application.
 */
public interface CurrentApplication extends Application {

    /**
     * <p>Generates a new certificate that will be sent to the remote server when asking to perform privileged operation
     * for this application.</p>
     *
     * @param urlToSign the target URL for this operation.
     * @return encrypted certificate representing this application
     */
    EncryptedCertificate encode(String username, String urlToSign);

}
