package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.util.encoders.Base64;
import uk.ac.warwick.sso.client.SSOConfiguration;

import java.security.PublicKey;
import java.security.PrivateKey;

public class SSOConfigCurrentApplication implements CurrentApplication {

    private final EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    private final String providerID;

    public SSOConfigCurrentApplication(SSOConfiguration config) throws Exception {
        this.providerID = config.getString("shire.providerid");
        this.publicKey = encryptionProvider.toPublicKey(Base64.decode(config.getString("trustedapps.publickey")));
        this.privateKey = encryptionProvider.toPrivateKey(Base64.decode(config.getString("trustedapps.privateKey")));
    }

    @Override
    public EncryptedCertificate encode(String username, String urlToSign) {
        return encryptionProvider.createEncryptedCertificate(username, privateKey, getProviderID(), urlToSign);
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

}
