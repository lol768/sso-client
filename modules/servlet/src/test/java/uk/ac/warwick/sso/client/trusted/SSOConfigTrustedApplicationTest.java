package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.util.encoders.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.ac.warwick.sso.client.core.ServletRequestAdapter;

import java.security.PublicKey;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class SSOConfigTrustedApplicationTest {

    private final Mockery m = new JUnit4Mockery();

    private static final String PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmP5gpPXUwXix9MieRN1bgI4odNVuRkPj+nmyKiLZ5zNhH" +
        "cecKzL9YFOf4LGvCjaukOt9thYdUBtgUdW0NjqqvtQ9PcnfDlNSdgfX/ag4X7WqKbJ8M+Z2yYsfQb32ItJYe/01OO" +
        "U6/k6twt1X4qIAIvJsflBh6Qqtjualwek28kSGCtC1JC/4m2pLvWrfxJqZF8UrMeZt2Zge0TanXz1Dnbjnz8dAH61" +
        "fABf7iFlCgkNn6U6jlYCoke9hMVMIWUe9Q5oBAre/jg5n8oCVLINgK7wWpIovBIT8foB7PL9SuM6gNkmtTVSBIvX3" +
        "t+P8ghvi8xafgoicl06PzwOwthYCPwIDAQAB";

    private static final String PROVIDER_ID = "urn:myapp.warwick.ac.uk:myapp:service";

    private SSOConfigTrustedApplication trustedApplication;

    private final EncryptionProvider provider = m.mock(EncryptionProvider.class);

    @Before
    public void setup() throws Exception {
        PublicKey publicKey = new BouncyCastleEncryptionProvider().toPublicKey(Base64.decode(PUBLIC_KEY));

        trustedApplication = new SSOConfigTrustedApplication(PROVIDER_ID, publicKey);
        trustedApplication.setEncryptionProvider(provider);
    }

    @Test
    public void testVerifySignature() throws Exception {
        ZonedDateTime base = ZonedDateTime.of (2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384, ZoneId.of("UTC"));
        String url = "http://warwick.ac.uk?external=true";
        String username = "cuscav";

        final byte[] signatureBase = TrustedApplicationUtils.generateSignatureBaseString(base, url, username);
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        m.checking(new Expectations() {{
            one(provider).verifySignature(
                trustedApplication.getPublicKey(),
                signatureBase,
                signature
            ); will(returnValue(true));
        }});

        // Check that it's just passed through to the encryption provider
        assertTrue(trustedApplication.verifySignature(base, url, username, signature));

        m.assertIsSatisfied();
    }

    @Test
    public void testDecode() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        ZonedDateTime creationTime = ZonedDateTime.now().minusSeconds(5);
        final ApplicationCertificate applicationCertificate = new ApplicationCertificateImpl(PROVIDER_ID, "cuscav", creationTime);

        final EncryptedCertificate encryptedCertificate = new EncryptedCertificateImpl(PROVIDER_ID, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");

        m.checking(new Expectations() {{
            one(provider).decodeEncryptedCertificate(
                encryptedCertificate,
                trustedApplication.getPublicKey(),
                PROVIDER_ID
            ); will(returnValue(applicationCertificate));
        }});

        assertEquals(applicationCertificate, trustedApplication.decode(encryptedCertificate, new ServletRequestAdapter(request)));

        m.assertIsSatisfied();
    }

    @Test(expected = CertificateTimeoutException.class)
    public void testDecodeExpired() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        ZonedDateTime creationTime = ZonedDateTime.now().minus(SSOConfigTrustedApplication.CERTIFICATE_TIMEOUT).minusSeconds(1);
        final ApplicationCertificate applicationCertificate = new ApplicationCertificateImpl(PROVIDER_ID, "cuscav", creationTime);

        final EncryptedCertificate encryptedCertificate = new EncryptedCertificateImpl(PROVIDER_ID, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");

        m.checking(new Expectations() {{
            one(provider).decodeEncryptedCertificate(
                    encryptedCertificate,
                    trustedApplication.getPublicKey(),
                    PROVIDER_ID
            ); will(returnValue(applicationCertificate));
        }});

        trustedApplication.decode(encryptedCertificate, new ServletRequestAdapter(request));

        fail("Should have thrown exception");
    }
}