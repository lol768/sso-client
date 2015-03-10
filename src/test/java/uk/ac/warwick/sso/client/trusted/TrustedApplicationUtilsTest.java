package uk.ac.warwick.sso.client.trusted;

import org.apache.http.client.methods.HttpGet;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.jruby.ext.openssl.x509store.Trust;
import org.junit.Test;

import static org.junit.Assert.*;

public class TrustedApplicationUtilsTest {

    private final Mockery m = new JUnit4Mockery();

    @Test
    public void testGenerateSignatureBaseString() throws Exception {
        DateTime base = new DateTime(2014, DateTimeConstants.AUGUST, 19, 3, 47, 19, 283);
        String url = "http://warwick.ac.uk?external=true";
        String username = "cuscav";

        byte[] sigBase = TrustedApplicationUtils.generateSignatureBaseString(base, url, username);
        String sigBaseString = new String(sigBase, "UTF-8");

        assertEquals(
            "1408416439283\n" +
            "http://warwick.ac.uk?external=true\n" +
            "cuscav",
            sigBaseString
        );
    }

    @Test
    public void testSignRequest() throws Exception {
        final HttpGet request = new HttpGet("http://warwick.ac.uk?external=true");
        final String username = "cuscav";

        final CurrentApplication currentApplication = m.mock(CurrentApplication.class);

        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        final EncryptedCertificate encryptedCertificate = new EncryptedCertificateImpl(
            providerID,
            certificate,
            signature
        );

        m.checking(new Expectations() {{
            one(currentApplication).encode("cuscav", "http://warwick.ac.uk?external=true");
                will(returnValue(encryptedCertificate));
        }});

        TrustedApplicationUtils.signRequest(currentApplication, username, request);

        assertEquals(3, request.getAllHeaders().length);
        assertEquals(certificate, request.getFirstHeader(TrustedApplication.HEADER_CERTIFICATE).getValue());
        assertEquals(providerID, request.getFirstHeader(TrustedApplication.HEADER_PROVIDER_ID).getValue());
        assertEquals(signature, request.getFirstHeader(TrustedApplication.HEADER_SIGNATURE).getValue());

        m.assertIsSatisfied();
    }
}