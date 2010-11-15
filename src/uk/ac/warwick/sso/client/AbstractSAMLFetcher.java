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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import uk.ac.warwick.sso.client.util.Xml;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.HttpPool;

public abstract class AbstractSAMLFetcher {

    private static final Log LOGGER = LogFactory.getLog(AbstractSAMLFetcher.class);

	private static boolean setFeatureSupported = true;
    
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
        xml.append( samlRequest.toString().replace("saml1:", "saml:" ) );
        xml.append( "</soap:Body></soap:Envelope>" );
        return xml.toString();
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
                        keystorePassword, ((cacertsLocation==null)?null:new URL(cacertsLocation)), cacertsPassword), standardHttpsPort);
            }
        } catch (MalformedURLException e) {
            throw new SSOException(e);
        }
        
        LOGGER.info("Connecting to " + location);
        HttpClient client = HttpPool.getHttpClient();
        client.getHostConfiguration().setHost(url.getHost(), url.getPort(), protocol);
        PostMethod method = new PostMethod(url.getPath());
        
        method.addRequestHeader("User-Agent", HttpMethodWebService.getUserAgent(_version));

        method.addRequestHeader("Content-Type", "text/xml");
        
        String fullRequest = generateSAMLRequestXml(subject, resource);

        
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
        String alias = ConfigHelper.getRequiredString(_config,"shire.keystore.shire-alias");
        List<Certificate> certChain = new ArrayList<Certificate>();
        certChain.add(getCertificate(alias));
        try {
            samlRequest.sign(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, getKey(alias), certChain);
        } catch (SAMLException e) {
            LOGGER.error("Could not sign SAML request", e);
            throw new RuntimeException("Could not sign SAML request", e);
        }
    }

    /**
     * Finds a Key entry in the given alias of the keystore.
     * An exception is thrown if the alias isn't found in the keystore,
     * or if the keystore couldn't be loaded.
     */
    private Key getKey(final String alias) {
        try {
            KeyStore keyStore = getKeyStore();
        	String string = _config.getString("shire.keystore.password");
        	if (string == null) {
        		throw new RuntimeException("No keystore password has been specified under shire.keystore.password");
        	}
			Key key = keyStore.getKey(alias, string.toCharArray());
        	if (key == null) {
        		StringBuilder sb = new StringBuilder("");
        		for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
        			String a = aliases.nextElement();
        			sb.append(" ").append(a);
        		}
        		throw new RuntimeException("Key with alias "+alias+" was not found in the keystore. Aliases in keystore:"+sb.toString());
        	}
        	return key;
        } catch (Exception e) {
            throw new RuntimeException("Could not load key from keystore", e);
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
            if (alias == null) {
            	throw new IllegalArgumentException("Tried to request a null alias from a keystore");
            }
            Certificate originCert = keyStore.getCertificate(alias);
            if (originCert == null) {
            	
            	throw new IllegalArgumentException("Couldn't find a certificate under alias '"
            			+alias+"' in keystore, aliases: " + toString(keyStore.aliases()));
            }
            return originCert;
        } catch (Exception e) {
            LOGGER.error("Could not load keystore", e);
            throw new RuntimeException("Could not load keystore", e);
        }

    }

	private String toString(Enumeration<String> enumer) {
		StringBuilder sb = new StringBuilder("[");
		while (enumer.hasMoreElements()) {
			sb.append(enumer.nextElement());
			sb.append(','); // Yeah trailing comma, big whoop.
		}
		sb.append("]");
		return sb.toString();
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
    	try {
    		URL uri = new URL(_config.getString("shire.keystore.location"));
    		String password = _config.getString("shire.keystore.password");
    		KeyStoreHelper helper = new KeyStoreHelper();
            KeyStore keyStore = helper.createKeyStore(uri, password);
            return keyStore;
    	} catch (MalformedURLException e) {
    		throw new IllegalArgumentException("shire.keystore.location could not be parsed as a URI", e);
    	}
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
