package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;

/**
 * Represents the certificate received by the filter from a trusted client.
 */
public interface ApplicationCertificate {

    DateTime getCreationTime();

    String getUsername();

    String getProviderID();

}
