package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;

public class ApplicationCertificateImpl implements ApplicationCertificate {

    private final String providerID;

    private final String username;

    private final DateTime creationTime;

    public ApplicationCertificateImpl(String providerID, String username, DateTime creationTime) {
        this.providerID = providerID;
        this.username = username;
        this.creationTime = creationTime;
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public DateTime getCreationTime() {
        return creationTime;
    }
}
