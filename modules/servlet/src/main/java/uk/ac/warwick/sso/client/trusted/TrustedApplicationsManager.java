package uk.ac.warwick.sso.client.trusted;

/**
 * A container for trusted application representations. Also contains a reference to this application.
 */
public interface TrustedApplicationsManager {

    TrustedApplication getTrustedApplication(String providerID);

    CurrentApplication getCurrentApplication();

}
