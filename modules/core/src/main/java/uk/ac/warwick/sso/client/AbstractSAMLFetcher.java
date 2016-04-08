package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import uk.ac.warwick.sso.client.util.Xml;
import uk.ac.warwick.userlookup.ClearGroupResponseHandler;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.HttpPool;

public abstract class AbstractSAMLFetcher {

    private static final Log LOGGER = LogFactory.getLog(AbstractSAMLFetcher.class);

	private static boolean setFeatureSupported = true;
    
    private SSOConfiguration _config;

    private String _version;

    protected AbstractSAMLFetcher() {}
    
    protected AbstractSAMLFetcher(SSOConfiguration config) {
        _config = config;
        _version = SSOClientVersionLoader.getVersion();
    }

    protected SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {
        return getSAMLResponse(subject, _config.getString("shire.providerid"));
    }
    
    /**
     * Given a SAMLSubject and a resource, wraps it in a SAML attribute query request
     * and signs it.
     */
    public String generateSAMLRequestXml(final SAMLSubject subject, final String resource) throws SSOException {
    	SAMLRequest samlRequest = new SAMLRequest();
        SAMLAttributeQuery query = new SAMLAttributeQuery();
        query.setResource(resource);
        try {
            query.setSubject(subject);
            samlRequest.setQuery(query);
            signRequest(samlRequest);
        } catch (SAMLException e) {
            LOGGER.error("SAMLException setting up samlRequest", e);
        }
        StringBuilder xml = new StringBuilder("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<soap:Body>");
        
        // SSO-1026 can't reproduce in a test, but on some servers it uses saml1 NS without declaring it 
        xml.append( samlRequest.toString() );
        xml.append( "</soap:Body></soap:Envelope>" );
        return xml.toString();
    }

    @SuppressWarnings("deprecation")
    protected SAMLResponse getSAMLResponse(final SAMLSubject subject, final String resource) throws SSOException {
        String location = getEndpointLocation();

        URL url;
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            throw new SSOException(e);
        }
        
        LOGGER.info("Connecting to " + location);
        HttpClient client = HttpPool.getHttpClient();
        HttpPost method = new HttpPost(url.toExternalForm());
        
        method.setHeader("User-Agent", HttpMethodWebService.getUserAgent(_version));
        method.setHeader("Content-Type", "text/xml");
        
        String fullRequest = generateSAMLRequestXml(subject, resource);
        method.setEntity(new StringEntity(fullRequest, ContentType.TEXT_XML));

        LOGGER.debug("SAMLRequest:" + fullRequest);
        String body;
        HttpResponse httpResponse = null;
        try {
            httpResponse = client.execute(method);
            body = EntityUtils.toString(httpResponse.getEntity());
            ClearGroupResponseHandler.staticProcessClearGroupHeader(httpResponse);
        } catch (IOException e) {
            LOGGER.error(location + " request failed at client.executeMethod", e);
            throw new SSOException(location + " request failed at client.executeMethod", e);
        } finally {
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }

        LOGGER.debug("Https response:" + body);

        if (body.indexOf("<soap:Fault><faultcode>") > -1) {
            throw new RuntimeException("Got bad response from " + location + ":" + body);
        }

        // turn https response into a SAML document and get the attributes out
        SAMLResponse samlResp = null;
        try {
            DocumentBuilder builder = Xml.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(body)));
            
            Element firstChild = (Element) document.getDocumentElement().getFirstChild().getFirstChild();
            
            LOGGER.debug("SAML Element: " + firstChild.getNodeName());
            
            samlResp = new SAMLResponse(firstChild);
        } catch (SAMLException e) {
            throw new SSOException("Could not create SAMLResponse from stream: " + e.getMessage());
        } catch (SAXException e) {
            throw new RuntimeException("Could not create SAMLResponse from stream", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not create SAMLResponse from stream", e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not create SAMLResponse from stream", e);
        } 
        return samlResp;
    }
    
    protected abstract String getEndpointLocation();

    private void signRequest(final SAMLRequest samlRequest) throws SAMLException {
        List<Certificate> certChain = new ArrayList<Certificate>();
        
        KeyAuthentication authenticationDetails = getConfig().getAuthenticationDetails();
        
        certChain.add(authenticationDetails.getCertificate());
        try {
            samlRequest.sign(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, authenticationDetails.getKey(), certChain);
        } catch (SAMLException e) {
            LOGGER.error("Could not sign SAML request", e);
            throw new RuntimeException("Could not sign SAML request", e);
        }
    }


    protected static String getValueFromAttribute(final String key, final Properties attributes) {

        if (attributes == null || attributes.get(key) == null) {
            return null;
        }

        return (String) ((SAMLAttribute) attributes.get(key)).getValues().next();
    }

    /**
     * @param samlResp
     * @return
     */
    protected Properties getAttributesFromResponse(final SAMLResponse samlResp) {
        Properties attributes = new Properties();

        if (samlResp.getAssertions() == null || !samlResp.getAssertions().hasNext()) {
            LOGGER.debug("Response has no assertions.");
            return attributes;
        }

        SAMLAssertion attributeAssertion = (SAMLAssertion) samlResp.getAssertions().next();
        SAMLAttributeStatement attributeStatement = (SAMLAttributeStatement) attributeAssertion.getStatements().next();
        Iterator<?> it = attributeStatement.getAttributes();
        while (it.hasNext()) {
            SAMLAttribute attribute = (SAMLAttribute) it.next();
            String name = attribute.getName();
            // String value = (String) attribute.getValues().next();
            LOGGER.debug(name + "=" + attribute);
            attributes.put(name, attribute);
        }
        return attributes;
    }

    public final SSOConfiguration getConfig() {
        return _config;
    }

    public final void setConfig(final SSOConfiguration config) {
        _config = config;
    }

}
