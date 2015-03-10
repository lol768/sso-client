package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.configuration.XMLConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import uk.ac.warwick.sso.client.SSOConfiguration;

import java.security.PrivateKey;

import static org.junit.Assert.*;

public class SSOConfigCurrentApplicationTest {

    private final Mockery m = new JUnit4Mockery();

    @Test
    public void testInit() throws Exception {
        SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("sso-config-trustedapps.xml")));

        SSOConfigCurrentApplication currentApplication = new SSOConfigCurrentApplication(config);

        assertEquals("urn:myapp.warwick.ac.uk:myapp:service", currentApplication.getProviderID());
        assertNotNull(currentApplication.getPublicKey());
    }

    @Test
    public void testEncode() throws Exception {
        SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("sso-config-trustedapps.xml")));
        SSOConfigCurrentApplication currentApplication = new SSOConfigCurrentApplication(config);

        final EncryptionProvider provider = m.mock(EncryptionProvider.class);
        currentApplication.setEncryptionProvider(provider);

        final String username = "cuscav";
        final String url = "http://warwick.ac.uk?external=true";

        final EncryptedCertificate cert = m.mock(EncryptedCertificate.class);
        m.checking(new Expectations() {{
            one(provider).createEncryptedCertificate(
                with(equal(username)),
                with(aNonNull(PrivateKey.class)),
                with(equal("urn:myapp.warwick.ac.uk:myapp:service")),
                with(equal(url))
            ); will(returnValue(cert));
        }});

        EncryptedCertificate encrypted = currentApplication.encode(username, url);
        assertEquals(cert, encrypted);

        m.assertIsSatisfied();

        // Test caching, shouldn't call again
        currentApplication.encode(username, url);

        m.assertIsSatisfied();
    }

}