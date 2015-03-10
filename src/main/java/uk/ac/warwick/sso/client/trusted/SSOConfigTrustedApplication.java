package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

public class SSOConfigTrustedApplication implements TrustedApplication {

    public static final Duration CERTIFICATE_TIMEOUT = Duration.standardMinutes(5);

    private final EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private final String providerID;

    private final PublicKey publicKey;

    public SSOConfigTrustedApplication(final String providerID, final PublicKey publicKey) {
        this.providerID = providerID;
        this.publicKey = publicKey;
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public PublicKey getPublicKey() { return null; }

    @Override
    public boolean verifySignature(DateTime timestamp, String requestUrl, String username, String receivedSignature) throws SignatureVerificationFailedException {
        try {
            return encryptionProvider.verifySignature(
                    this.publicKey,
                    TrustedApplicationUtils.generateSignatureBaseString(timestamp, requestUrl, username),
                    receivedSignature
            );
        } catch (UnsupportedEncodingException e) {
            throw new SignatureVerificationFailedException(e);
        }
    }

    @Override
    public ApplicationCertificate decode(EncryptedCertificate encCert, HttpServletRequest request) throws InvalidCertificateException {
        ApplicationCertificate cert = encryptionProvider.decodeEncryptedCertificate(encCert, publicKey, getProviderID());

        // Check expiry of cert
        DateTime created = cert.getCreationTime();

        if (created.plus(CERTIFICATE_TIMEOUT).isBefore(DateTime.now())) {
            throw new CertificateTimeoutException(cert, CERTIFICATE_TIMEOUT.getMillis());
        }

        return cert;
    }
}
