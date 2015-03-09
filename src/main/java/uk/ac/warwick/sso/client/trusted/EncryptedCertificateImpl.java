package uk.ac.warwick.sso.client.trusted;

public class EncryptedCertificateImpl implements EncryptedCertificate {

    private final String providerID;

    private final String certificate;

    private final String signature;

    public EncryptedCertificateImpl(String providerID, String certificate, String signature) {
        this.providerID = providerID;
        this.certificate = certificate;
        this.signature = signature;
    }

    public EncryptedCertificateImpl(String providerID, String certificate) {
        this(providerID, certificate, null);
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }

    @Override
    public String getSignature() {
        return signature;
    }
}
