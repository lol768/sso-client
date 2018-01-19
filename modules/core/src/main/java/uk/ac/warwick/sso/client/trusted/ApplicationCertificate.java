package uk.ac.warwick.sso.client.trusted;


import java.time.ZonedDateTime;

/**
 * Represents the certificate received by the filter from a trusted client.
 */
public interface ApplicationCertificate {

    ZonedDateTime getCreationTime();

    String getUsername();

    String getProviderID();

}
