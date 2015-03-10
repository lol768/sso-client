package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.http.HttpContext;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.mortbay.servlet.MultiPartFilter;
import org.mortbay.util.InetAddrPort;
import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupAdapter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TrustedApplicationEndToEndTest {

    protected static final int PORT = (int)(Math.random() * 10000.0D) + 20000;
    protected String serverAddress;
    private static Server server;

    public TrustedApplicationEndToEndTest() {
        this.serverAddress = "http://localhost:" + PORT + "/";
    }

    @BeforeClass
    public static void setupServer() throws Exception {
        System.setProperty("org.mortbay.util.URI.charset", "ISO-8859-1");
        server = new Server();
        server.setTrace(true);
        server.addListener(new InetAddrPort(PORT));

        WebApplicationHandler handler = new WebApplicationHandler();
        handler.addServlet("/user", UsernameEchoingServlet.class.getName());

        handler.defineFilter("mpfilter", MultiPartFilter.class.getName());
        handler.addFilterPathMapping("*", "mpfilter", 1);

        handler.defineFilter("ssoclientfilter", SSOClientFilter.class.getName());
        handler.addFilterPathMapping("*", "ssoclientfilter", 1);

        FilterHolder taFilterHolder = handler.defineFilter("trustedappsfilter", TrustedApplicationFilter.class.getName());
        handler.addFilterPathMapping("*", "trustedappsfilter", 1);

        HttpContext context = server.getContext("/");
        context.setAttribute("SSO-CONFIG", new SSOConfiguration(new XMLConfiguration(TrustedApplicationEndToEndTest.class.getResource("/sso-config-trustedapps.xml"))));

        context.addHandler(handler);
        context.setInitParameter("ssoclient.config", "/sso-config-trustedapps.xml");

        server.start();
        ((TrustedApplicationFilter)taFilterHolder.getFilter()).setUserLookup(
            new UserLookupAdapter(null) {
                @Override
                public User getUserByUserId(String userId) {
                    User user = new User(userId);
                    user.setFoundUser(true);
                    return user;
                }
            }
        );
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stop();
        server = null;
        System.setProperty("org.mortbay.util.URI.charset", "UTF-8");
    }

    private TrustedApplicationsManager appManager;

    @Before
    public void setup() throws Exception {
        SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(TrustedApplicationEndToEndTest.class.getResource("/sso-config-trustedapps.xml")));
        this.appManager = new SSOConfigTrustedApplicationsManager(config);
    }

    @Test
    public void testGet() throws Exception {
        HttpGet request = new HttpGet(serverAddress + "user?yes=no");
        HttpClient httpClient = HttpClientBuilder.create().build();

        TrustedApplicationUtils.signRequest(appManager.getCurrentApplication(), "cuscav", request);

        HttpResponse response = httpClient.execute(request);
        for (Header header: response.getAllHeaders()) {
            System.out.println(header.getName() + "=" + header.getValue());
        }

        assertEquals("cuscav", EntityUtils.toString(response.getEntity()).trim());
    }

    @Test
    public void testPost() throws Exception {
        HttpPost request = new HttpPost(serverAddress + "user");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("username", "vip"));
        nvps.add(new BasicNameValuePair("password", "secret"));
        request.setEntity(new UrlEncodedFormEntity(nvps));

        TrustedApplicationUtils.signRequest(appManager.getCurrentApplication(), "cuscav", request);

        HttpClient httpClient = HttpClientBuilder.create().build();

        assertEquals("cuscav", EntityUtils.toString(httpClient.execute(request).getEntity()).trim());
    }

    public static class UsernameEchoingServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(SSOClientFilter.getUserFromRequest(req).getUserId());
            resp.getWriter().write("\n");
            resp.getWriter().flush();
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(SSOClientFilter.getUserFromRequest(req).getUserId());
            resp.getWriter().write("\n");
            resp.getWriter().flush();
        }
    }

}
