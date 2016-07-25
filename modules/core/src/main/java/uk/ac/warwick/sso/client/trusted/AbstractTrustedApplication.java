package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.util.encoders.Base64;
import org.joda.time.DateTime;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.core.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

public abstract class AbstractTrustedApplication implements TrustedApplication {

    protected EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private final String providerID;

    private final PublicKey publicKey;

    public AbstractTrustedApplication(final String providerID, final PublicKey publicKey) {
        this.providerID = providerID;
        this.publicKey = publicKey;
    }

    public AbstractTrustedApplication(final SSOConfiguration config) throws Exception {
        this.providerID = config.getString("shire.providerid");
        this.publicKey = encryptionProvider.toPublicKey(Base64.decode(config.getString("trustedapps.publickey")));
    }

    @Override
    public final String getProviderID() {
        return providerID;
    }

    @Override
    public final PublicKey getPublicKey() { return publicKey; }

    @Override
    public final boolean verifySignature(DateTime timestamp, String requestUrl, String username, String receivedSignature) throws SignatureVerificationFailedException {
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
    public final ApplicationCertificate decode(EncryptedCertificate encCert, HttpRequest request) throws InvalidCertificateException {
        ApplicationCertificate cert = encryptionProvider.decodeEncryptedCertificate(encCert, publicKey, getProviderID());

        // Check expiry of cert
        DateTime created = cert.getCreationTime();

        if (created.plus(CERTIFICATE_TIMEOUT).isBefore(DateTime.now())) {
            throw new CertificateTimeoutException(cert, CERTIFICATE_TIMEOUT.getMillis());
        }

        return cert;
    }

    final void setEncryptionProvider(EncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }
}
