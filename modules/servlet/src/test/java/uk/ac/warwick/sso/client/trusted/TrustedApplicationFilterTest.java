package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.configuration.XMLConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.core.ServletRequestAdapter;
import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupInterface;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Vector;

import static org.junit.Assert.*;

public class TrustedApplicationFilterTest {

    private static final String REQUEST_URL = "http://warwick.ac.uk?external=true";

    private final Mockery m = new JUnit4Mockery();

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = m.mock(FilterChain.class);

    private final TrustedApplicationFilter filter = new TrustedApplicationFilter();

    private final UserLookupInterface userLookup = m.mock(UserLookupInterface.class);
    private final TrustedApplicationsManager appManager = m.mock(TrustedApplicationsManager.class);

    @Before
    public void setup() throws Exception {
        filter.setUserLookup(userLookup);
        filter.setTrustedApplicationsManager(appManager);

        request.addHeader("X-Requested-URI", REQUEST_URL);

        SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("/sso-config-trustedapps.xml")));
        SSOConfiguration.setConfig(config);
        filter.setConfig(config);
    }

    @After
    public void assertIsSatisfied() { m.assertIsSatisfied(); }

    @Test
    public void testNoCert() throws Exception {
        m.checking(new Expectations() {{
            one(chain).doFilter(with(any(HttpServletRequest.class)), with(equal(response)));
        }});

        filter.doFilter(request, response, chain);
    }

    @Test
    public void testUserAlreadyProcessed() throws Exception {
        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");
        request.setAttribute(SSOClientFilter.getUserKey(), new User() {{ setFoundUser(true); }});

        m.checking(new Expectations() {{
            one(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);
    }

    @Test
    public void testMissingProviderID() throws Exception {
        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");

        m.checking(new Expectations() {{
            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("PROVIDER_ID_NOT_FOUND", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Provider ID not found in request", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testInvalidProviderID() throws Exception {
        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, "urn:myapp.warwick.ac.uk:myapp:service");

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(null));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("APP_UNKNOWN", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Unknown Application: urn:myapp.warwick.ac.uk:myapp:service", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testInvalidCertificate() throws Exception {
        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, "urn:myapp.warwick.ac.uk:myapp:service");

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(throwException(new InvalidCertificateException(new TransportErrorMessage.System(new RuntimeException(), "urn:myapp.warwick.ac.uk:myapp:service"))));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("SYSTEM", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Exception: java.lang.RuntimeException occurred serving request for application: urn:myapp.warwick.ac.uk:myapp:service", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testMissingSignature() throws Exception {
        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, "MTQxOTQ5OTg4OTM4NApjdXNjYXY=");
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, "urn:myapp.warwick.ac.uk:myapp:service");

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(returnValue(appCertificate));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("BAD_SIGNATURE", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Missing signature in request", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testBadSignature() throws Exception {
        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, certificate);
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, providerID);
        request.addHeader(TrustedApplication.HEADER_SIGNATURE, signature);

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        final DateTime base = DateTime.now();
        final String username = "cuscav";

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(returnValue(appCertificate));

            allowing(appCertificate).getCreationTime(); will(returnValue(base));
            allowing(appCertificate).getUsername(); will(returnValue(username));

            one(trustedApp).verifySignature(base, REQUEST_URL, username, signature);
                will(returnValue(false));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("BAD_SIGNATURE", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Bad signature for URL: http://warwick.ac.uk?external=true", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testErrorProcessingSignature() throws Exception {
        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, certificate);
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, providerID);
        request.addHeader(TrustedApplication.HEADER_SIGNATURE, signature);

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        final DateTime base = DateTime.now();
        final String username = "cuscav";

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(returnValue(appCertificate));

            allowing(appCertificate).getCreationTime(); will(returnValue(base));
            allowing(appCertificate).getUsername(); will(returnValue(username));

            one(trustedApp).verifySignature(base, REQUEST_URL, username, signature);
                will(throwException(new SignatureVerificationFailedException(new RuntimeException())));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("BAD_SIGNATURE", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Bad signature for URL: http://warwick.ac.uk?external=true", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testUserNotFound() throws Exception {
        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, certificate);
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, providerID);
        request.addHeader(TrustedApplication.HEADER_SIGNATURE, signature);

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        final DateTime base = DateTime.now();
        final String username = "cuscav";

        final User user = new AnonymousUser();

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(returnValue(appCertificate));

            allowing(appCertificate).getCreationTime(); will(returnValue(base));
            allowing(appCertificate).getUsername(); will(returnValue(username));

            one(trustedApp).verifySignature(base, REQUEST_URL, username, signature);
                will(returnValue(true));

            one(userLookup).getUserByUserId("cuscav"); will(returnValue(user));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("USER_UNKNOWN", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("Unknown User: cuscav", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testUserDisabled() throws Exception {
        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, certificate);
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, providerID);
        request.addHeader(TrustedApplication.HEADER_SIGNATURE, signature);

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        final DateTime base = DateTime.now();
        final String username = "cuscav";

        final User user = new User() {{
            setFoundUser(true);
            setLoginDisabled(true);
        }};

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
                will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
                will(returnValue(appCertificate));

            allowing(appCertificate).getCreationTime(); will(returnValue(base));
            allowing(appCertificate).getUsername(); will(returnValue(username));

            one(trustedApp).verifySignature(base, REQUEST_URL, username, signature);
                will(returnValue(true));

            one(userLookup).getUserByUserId("cuscav"); will(returnValue(user));

            never(chain).doFilter(request, response);
        }});

        filter.doFilter(request, response, chain);

        assertEquals("Error", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals("USER_DISABLED", response.getHeader(TrustedApplication.HEADER_ERROR_CODE));
        assertEquals("User disabled: cuscav", response.getHeader(TrustedApplication.HEADER_ERROR_MESSAGE));
    }

    @Test
    public void testItWorks() throws Exception {
        final String certificate = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";
        final String providerID = "urn:myapp.warwick.ac.uk:myapp:service";
        final String signature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        request.addHeader(TrustedApplication.HEADER_CERTIFICATE, certificate);
        request.addHeader(TrustedApplication.HEADER_PROVIDER_ID, providerID);
        request.addHeader(TrustedApplication.HEADER_SIGNATURE, signature);

        final TrustedApplication trustedApp = m.mock(TrustedApplication.class);
        final ApplicationCertificate appCertificate = m.mock(ApplicationCertificate.class);

        final DateTime base = DateTime.now();
        final String username = "cuscav";

        final User user = new User() {{
            setFoundUser(true);
            setLoginDisabled(false);
        }};

        m.checking(new Expectations() {{
            one(appManager).getTrustedApplication("urn:myapp.warwick.ac.uk:myapp:service");
            will(returnValue(trustedApp));

            one(trustedApp).decode(with(equal(new EncryptedCertificateImpl("urn:myapp.warwick.ac.uk:myapp:service", "MTQxOTQ5OTg4OTM4NApjdXNjYXY="))), with(any(ServletRequestAdapter.class)));
            will(returnValue(appCertificate));

            allowing(appCertificate).getCreationTime(); will(returnValue(base));
            allowing(appCertificate).getUsername(); will(returnValue(username));

            one(trustedApp).verifySignature(base, REQUEST_URL, username, signature);
            will(returnValue(true));

            one(userLookup).getUserByUserId("cuscav"); will(returnValue(user));

            one(chain).doFilter(with(any(HttpServletRequest.class)), with(equal(response)));
        }});

        filter.doFilter(request, response, chain);

        assertEquals("OK", response.getHeader(TrustedApplication.HEADER_STATUS));
        assertEquals(user, request.getAttribute(SSOClientFilter.getUserKey()));
        assertTrue(user.isTrustedApplicationsUser());
    }

}