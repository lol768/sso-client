package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

public class SSOConfigTrustedApplication implements TrustedApplication {

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
        return encryptionProvider.decodeEncryptedCertificate(encCert, publicKey, getProviderID());
    }
}
