package uk.ac.warwick.sso.client.trusted;

public class EncryptedCertificateImpl implements EncryptedCertificate {

    private final String providerID;

    private final String certificate;

    public EncryptedCertificateImpl(String providerID, String certificate) {
        this.providerID = providerID;
        this.certificate = certificate;
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }
}
