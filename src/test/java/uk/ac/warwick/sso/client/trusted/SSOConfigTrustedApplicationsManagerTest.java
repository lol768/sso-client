package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import uk.ac.warwick.sso.client.SSOConfiguration;

import static org.junit.Assert.*;

public class SSOConfigTrustedApplicationsManagerTest {

    @Test
    public void testInitFromConfig() throws Exception {
        SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("/sso-config-trustedapps.xml")));

        SSOConfigTrustedApplicationsManager manager = new SSOConfigTrustedApplicationsManager(config);
        assertNotNull(manager.getCurrentApplication());
        assertNotNull(manager.getTrustedApplication("urn:tabula.warwick.ac.uk:tabula:service"));
        assertNotNull(manager.getTrustedApplication("urn:start.warwick.ac.uk:portal:service"));
        assertNull(manager.getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service"));
    }

}