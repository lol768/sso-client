package uk.ac.warwick.sso.client.oauth;

import static java.util.Arrays.*;
import static uk.ac.warwick.sso.client.util.XMLParserUtils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.content.x509.XMLX509Certificate;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.warwick.sso.client.util.Xml;

public abstract class OAuthServiceRequest {
    
    public static final String ERROR_KEY = "error";
    
    public static final String ERROR_NOT_FOUND = "not-found";

    /** The URI for XMLNS spec*/
    public static final String NAMESPACE_SPEC_NS = "http://www.w3.org/2000/xmlns/";
    
    private static final Log LOGGER = LogFactory.getLog(OAuthServiceRequest.class);
    
    private static final long FIVE_MINUTES_IN_MS = 5 * 60 * 1000L;
    
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    private static final String NS = "urn:websignon:oauth";

    private final String verb;

    private final String resource;
    
    private boolean subjectNameMatched;

    public OAuthServiceRequest(String verb, String resource) {
        this.verb = verb;
        this.resource = resource;
    }

    public String toSignedXML(String algorithm, Key key, List<Certificate> certChain) {
        try {
            DocumentBuilder builder = Xml.newDocumentBuilder();
            
            Document doc = builder.newDocument();
            Element oauthElement = doc.createElementNS(NS, "OAuth");
            oauthElement.setAttributeNS(NAMESPACE_SPEC_NS, "xmlns", NS);
            doc.appendChild(oauthElement);
            
            Element requestElement = doc.createElementNS(NS, "Request");
            requestElement.setAttributeNS(NS, "id", "Body");
            requestElement.setIdAttributeNS(NS, "id", true);
            
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            requestElement.setAttributeNS(NS, "Issued", formatter.format(new Date()));
            
            requestElement.setAttributeNS(NS, "Resource", resource);
            
            oauthElement.appendChild(requestElement);
            
            Element verbElement = doc.createElementNS(NS, "Verb");
            verbElement.appendChild(doc.createTextNode(verb));
            
            requestElement.appendChild(verbElement);
            
            Element args = doc.createElementNS(NS, "Arguments");
            
            addSpecificElements(args);
            
            requestElement.appendChild(args);
            
            Element signatureElement = doc.createElementNS(NS, "Signature");
            signatureElement.setAttributeNS(NS, "id", "Signature");
            signatureElement.setIdAttributeNS(NS, "id", true);
            oauthElement.appendChild(signatureElement);
            
            XMLSignature sig = new XMLSignature(doc, null, algorithm, Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signatureElement.appendChild(sig.getElement());
            
            sig.addDocument("#Body");
            
            // Add any X.509 certificates provided.
            X509Data x509 = new X509Data(doc);

            if (certChain != null) {
            	if (LOGGER.isDebugEnabled()) LOGGER.debug("Found " + certChain.size() + " X509 Certificates to add to signature");
                int count = 0;
                Iterator<Certificate> i = certChain.iterator();
                while (i.hasNext()) {
                    Certificate cert = i.next();
                    if (cert instanceof X509Certificate) {
                        if (!i.hasNext() && count > 0) {
                            // Last (but not only) cert in chain. Only add if
                            // it's not self-signed.
                            if (((X509Certificate) cert).getSubjectDN().equals(((X509Certificate) cert).getIssuerDN())) {
                            	if (LOGGER.isDebugEnabled()) LOGGER.debug("Skipping self-signed cert at end of chain");
                                break;
                            }
                        }
                        x509.addCertificate((X509Certificate) cert);
                    }
                    count++;
                }
            }
            
            if (x509.lengthCertificate()>0)
            {
            	if (LOGGER.isDebugEnabled()) LOGGER.debug("Adding a list of " + x509.lengthCertificate() + " X509 Certificates to signature");
                KeyInfo keyinfo = new KeyInfo(doc);
                keyinfo.add(x509);
                sig.getElement().appendChild(keyinfo.getElement());
            }
            
            // Finally, sign the thing.
            sig.sign(key);
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLUtils.outputDOMc14nWithComments(doc, os);
            return os.toString("UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't generate XML", e);
        }
    }
    
    abstract void addSpecificElements(Element args);
    
    protected void addArgument(Element args, String key, String value) {
        if (value == null) {
            return;
        }
        
        Element argument = args.getOwnerDocument().createElement(key);
        argument.appendChild(args.getOwnerDocument().createTextNode(value));
        args.appendChild(argument);
    }
    
    public static OAuthServiceRequest fromHttpServletRequest(HttpServletRequest request, OAuthServiceRequestSecurityRule securityRule) {
        if (!request.getMethod().equals("POST") || !request.getContentType().startsWith("text/xml"))
            throw new IllegalArgumentException("Bad HTTP method or content type");
        
        try {
            DocumentBuilder builder = Xml.newDocumentBuilder();
            
            Document doc = builder.parse(request.getInputStream());
            Element sigElement = (Element)doc.getElementsByTagNameNS(NS, "Signature").item(0);
            
            XMLSignature sig = new XMLSignature((Element)sigElement.getFirstChild(), null);            
            byte[] signedContent = sig.getSignedInfo().getSignedContentItem(0);
            
            Element element = builder.parse(new ByteArrayInputStream(signedContent)).getDocumentElement();
            
            List<X509Certificate> cert = extractCertificates(request, sig);
            
            securityRule.run(request, cert, sig, element);
            
            String issuedString = element.getAttribute("Issued");
            
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            Date issued = formatter.parse(issuedString);
            
            long nowL = new Date().getTime();
            long issuedL = issued.getTime();
            
            // check that the difference in MS is acceptable
            long difference = Math.abs( nowL - issuedL );
            if (difference > FIVE_MINUTES_IN_MS) {
                throw new IllegalArgumentException("Request expired - difference was " + difference + "ms");
            }
            
            String resource = element.getAttribute("Resource");
            String verb = getText(getFirstMatchingElement(element, "Verb"));
            
            Map<String, String> arguments = new HashMap<String, String>();
            NodeList args = getFirstMatchingElement(element, "Arguments").getChildNodes();
            for (int i = 0; i < args.getLength(); i++) {
                Node node = args.item(i);
                if (node.getNodeType() != Element.ELEMENT_NODE) {
                    continue;
                }
                
                arguments.put(node.getNodeName(), getText(node));
            }
            
            if (verb.equals(GetTokenRequest.VERB)) {
                return GetTokenRequest.of(resource, arguments);
            } else if (verb.equals(GetConsumerRequest.VERB)) {
                return GetConsumerRequest.of(resource, arguments);
            } else if (verb.equals(SaveTokenRequest.VERB)) {
                return SaveTokenRequest.of(resource, arguments);
            } else {
                throw new IllegalArgumentException("Invalid verb: " + verb);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't parse XML", e);
        }
    }

	private static List<X509Certificate> extractCertificates(
			HttpServletRequest request, XMLSignature sig)
			throws KeyResolverException {
		List<X509Certificate> cert = null;
		
		if (request.getAttribute("javax.servlet.request.X509Certificate") != null) {
		    X509Certificate[] x509Certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		    if (x509Certificates.length > 0) {
		    	LOGGER.debug("Found certificates in request " + x509Certificates[0].getSubjectDN().getName());
		    }
		    cert = asList(x509Certificates);
		} else {
		    LOGGER.info("OAuthServiceController receiving request without SSL certs");
		}
		
		if (cert == null) {
		    LOGGER.info("Checking against passed key in signature rather than client cert");
		    cert = new ArrayList<X509Certificate>();
		    final KeyInfo keyInfo = sig.getKeyInfo();
		    try {
			    if (keyInfo.lengthX509Data() > 0) { // should only be one X509Data element, containing the chain
			    	X509Data x509Data = keyInfo.itemX509Data(0);
			    	final int l = x509Data.lengthCertificate();
			    	for (int i=0; i<l; i++) {
			    		XMLX509Certificate c = x509Data.itemCertificate(i);
			    		cert.add( c.getX509Certificate() );
			    	}
			    }
		    } catch (XMLSecurityException e) {
				LOGGER.error("Error building certificates from XML signature", e);
			}
		}
		
		return cert;
	}

    public final String getVerb() {
        return verb;
    }

    public final String getResource() {
        return resource;
    }

    public static class GetTokenRequest extends OAuthServiceRequest {
        
        public static final String VERB = "GetToken";
        
        private final String token;

        public GetTokenRequest(String token, String resource) {
            super(VERB, resource);
            
            if (token == null) {
                throw new NullPointerException("Null token");
            }
            
            this.token = token;
        }

        @Override
        void addSpecificElements(Element args) {
            addArgument(args, "token", token);
        }
        
        public String getToken() {
            return token;
        }

        public static GetTokenRequest of(String resource, Map<String, String> arguments) {
            return new GetTokenRequest(arguments.get("token"), resource);
        }
        
    }
    
    public static class GetConsumerRequest extends OAuthServiceRequest {
        
        public static final String VERB = "GetConsumer";
        
        private final String consumerKey;

        public GetConsumerRequest(String consumerKey, String resource) {
            super(VERB, resource);
            
            if (consumerKey == null) {
                throw new NullPointerException("Null consumer key");
            }
            
            this.consumerKey = consumerKey;
        }

        @Override
        void addSpecificElements(Element args) {
            addArgument(args, "consumer_key", consumerKey);
        }
        
        public String getConsumerKey() {
            return consumerKey;
        }

        public static GetConsumerRequest of(String resource, Map<String, String> arguments) {
            return new GetConsumerRequest(arguments.get("consumer_key"), resource);
        }
        
    }
    
    public static class SaveTokenRequest extends OAuthServiceRequest {
        
        public static final String VERB = "SaveToken";
        
        private final OAuthToken token;
        
        public SaveTokenRequest(OAuthToken token, String resource) {
            super(VERB, resource);
            
            if (token == null) {
                throw new NullPointerException("Null token");
            }
            
            this.token = token;
        }
        
        @Override
        void addSpecificElements(Element args) {
            Map<String, String> map = token.toMap();
            for (Entry<String, String> entry : map.entrySet()) {
                addArgument(args, entry.getKey(), entry.getValue());
            }
        }
        
        public OAuthToken getToken() {
            return token;
        }

        public static SaveTokenRequest of(String resource, Map<String, String> arguments) {
            return new SaveTokenRequest(OAuthToken.fromMap(arguments), resource);
        }
        
    }
    
    public void setSubjectNameMatched() {
    	subjectNameMatched = true;
    }
    
    public boolean getSubjectNameMatched() {
    	return subjectNameMatched;
    }

}
