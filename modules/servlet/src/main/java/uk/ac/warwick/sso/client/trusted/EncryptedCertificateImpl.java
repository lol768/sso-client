package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
            .append("providerID", providerID)
            .append("certificate", certificate)
            .append("signature", signature)
            .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EncryptedCertificate)) {
            return false;
        }

        EncryptedCertificate other = (EncryptedCertificate) obj;
        return new EqualsBuilder()
            .append(providerID, other.getProviderID())
            .append(certificate, other.getCertificate())
            .append(signature, other.getSignature())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(providerID)
            .append(certificate)
            .append(signature)
            .toHashCode();
    }

}
