package uk.ac.warwick.sso.client.trusted;

import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.SSOHandler;
import uk.ac.warwick.userlookup.UserLookupInterface;

public interface TrustedApplicationHandler extends SSOHandler {

    void setUserLookup(UserLookupInterface userLookup);

    void setTrustedApplicationsManager(TrustedApplicationsManager appManager);

    void setConfig(SSOConfiguration config);

}
