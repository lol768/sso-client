package uk.ac.warwick.sso.client.trusted;

public class CertificateTimeoutException extends InvalidCertificateException {
    public CertificateTimeoutException(ApplicationCertificate certificate, long certificateTimeout) {
        super(new TransportErrorMessage(TransportErrorMessage.Code.OLD_CERT, "Certificate too old. Application: {0} Certificate Created: {1} Timeout: {2}", certificate.getProviderID(), String.valueOf(certificate.getCreationTime()), String.valueOf(certificateTimeout)));
    }
}
