package uk.ac.warwick.sso.client.trusted;


import java.time.ZonedDateTime;

public class ApplicationCertificateImpl implements ApplicationCertificate {

    private final String providerID;

    private final String username;

    private final ZonedDateTime creationTime;

    public ApplicationCertificateImpl(String providerID, String username, ZonedDateTime creationTime) {
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

    public ZonedDateTime getCreationTime() {
        return creationTime;
    }
}
