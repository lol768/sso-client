package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;
import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.userlookup.HttpPool;

public abstract class AbstractSAMLFetcher {

    private static final Log LOGGER = LogFactory.getLog(AbstractSAMLFetcher.class);
    
    private Configuration _config;

    private String _version;

    private String keystoreLocation;
    private String keystorePassword;
    private String cacertsLocation;
    private String cacertsPassword;
    
    private Protocol protocol;
    
    protected AbstractSAMLFetcher() {}
    
    protected AbstractSAMLFetcher(Configuration config) {
        _config = config;
        keystoreLocation = _config.getString("shire.keystore.location");
        keystorePassword = _config.getString("shire.keystore.password");
        cacertsLocation = _config.getString("cacertskeystore.location");
        cacertsPassword = _config.getString("cacertskeystore.password");
        _version = SSOClientVersionLoader.getVersion();
    }

    protected SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {
        return getSAMLResponse(subject, _config.getString("shire.providerid"));
    }

    @SuppressWarnings("deprecation")
    protected SAMLResponse getSAMLResponse(final SAMLSubject subject, final String resource) throws SSOException {
        String location = getEndpointLocation();

        final int standardHttpsPort = 443;

        URL url;
        try {
            url = new URL(location);
            if (protocol == null) {
                protocol = new Protocol("https", new AuthSSLProtocolSocketFactory(
                        new URL(keystoreLocation),
                        keystorePassword, new URL(cacertsLocation), cacertsPassword), standardHttpsPort);
            }
        } catch (MalformedURLException e) {
            throw new SSOException(e);
        }
        
        LOGGER.info("Connecting to " + location);
        HttpClient client = HttpPool.getHttpClient();
        client.getHostConfiguration().setHost(url.getHost(), url.getPort(), protocol);
        PostMethod method = new PostMethod(url.getPath());
        
        if (_version == null) {
            method.addRequestHeader("User-Agent", "SSOClient");
        } else {
            method.addRequestHeader("User-Agent", "SSOClient " + _version);
        }

        method.addRequestHeader("Content-Type", "text/xml");
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

        String fullRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<soap:Body>";
        fullRequest += samlRequest.toString();
        fullRequest += "</soap:Body></soap:Envelope>";
        method.setRequestBody(fullRequest);
        LOGGER.debug("SAMLRequest:" + fullRequest);
        String body;
        try {
            client.executeMethod(method);
            body = method.getResponseBodyAsString();
        } catch (IOException e) {
            LOGGER.error(location + " request failed at client.executeMethod", e);
            throw new SSOException(location + " request failed at client.executeMethod", e);
        } finally {
            method.releaseConnection();
        }

        LOGGER.debug("Https response:" + body);

        if (body.indexOf("<soap:Fault><faultcode>") > -1) {
            throw new RuntimeException("Got bad response from " + location + ":" + body);
        }

        // turn https response into a SAML document and get the attributes out
        SAMLResponse samlResp = null;
        try {
            /**
             * Replaced the XML.parserPool implementation with this because
             * otherwise it breaks using a recent Xalan XML parser, as the
             * SOAP response from SSO has a hack (not mine!!) which the parser
             * doesn't like. So we turn off validation here, and it can ignore it.
             * All we really care about is the block of SAML in the middle. 
             */
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
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

    private void signRequest(final SAMLRequest samlRequest) {
        String alias = _config.getString("shire.keystore.shire-alias");
        List<Certificate> certChain = new ArrayList<Certificate>();
        certChain.add(getCertificate(alias));
        try {
            samlRequest.sign(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, getKey(alias), certChain);
        } catch (SAMLException e) {
            LOGGER.error("Could not sign SAML request", e);
            throw new RuntimeException("Could not sign SAML request", e);
        }
    }

    private Key getKey(final String alias) {

        try {
            KeyStore keyStore = getKeyStore();
            Key key = keyStore.getKey(alias, _config.getString("shire.keystore.password").toCharArray());
            return key;
        } catch (Exception e) {
            LOGGER.error("Could not create keystore", e);
            throw new RuntimeException("Could not create keystore", e);
        }

    }

    /**
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws MalformedURLException
     */
    private Certificate getCertificate(final String alias) {

        try {
            KeyStore keyStore = getKeyStore();
            Certificate originCert = keyStore.getCertificate(alias);
            return originCert;
        } catch (Exception e) {
            LOGGER.error("Could not create keystore", e);
            throw new RuntimeException("Could not create keystore", e);
        }

    }

    /**
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws MalformedURLException
     */
    private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStoreHelper helper = new KeyStoreHelper();
        KeyStore keyStore = helper.createKeyStore(new URL(_config.getString("shire.keystore.location")), _config
                .getString("shire.keystore.password"));
        return keyStore;
    }

    protected String getValueFromAttribute(final String key, final Properties attributes) {

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

    public final Configuration getConfig() {
        return _config;
    }

    public final void setConfig(final Configuration config) {
        _config = config;
    }

}