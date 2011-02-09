package uk.ac.warwick.sso.client.oauth;

import static uk.ac.warwick.sso.client.util.XMLParserUtils.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;

import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.warwick.sso.client.util.Xml;

public final class OAuthServiceResponse {

    /** The URI for XMLNS spec*/
    public static final String NAMESPACE_SPEC_NS = "http://www.w3.org/2000/xmlns/";
    
    private static final long FIVE_MINUTES_IN_MS = 5 * 60 * 1000L;
    
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    private static final String NS = "urn:websignon:oauth";

    private final String verb;

    private final String resource;
    
    private final Map<String, String> attributes;

    public OAuthServiceResponse(String verb, String resource, Map<String, String> attributes) {
        this.verb = verb;
        this.resource = resource;
        this.attributes = attributes;
    }

    public String toXML() {
        try {
            DocumentBuilder builder = Xml.newDocumentBuilder();
            
            Document doc = builder.newDocument();
            Element oauthElement = doc.createElementNS(NS, "OAuth");
            oauthElement.setAttributeNS(NAMESPACE_SPEC_NS, "xmlns", NS);
            doc.appendChild(oauthElement);
            
            Element requestElement = doc.createElementNS(NS, "Response");
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
            
            Element atts = doc.createElementNS(NS, "Attributes");
            
            for (Entry<String, String> entry : attributes.entrySet()) {
                addAttribute(atts, entry.getKey(), entry.getValue());
            }
            
            requestElement.appendChild(atts);
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLUtils.outputDOMc14nWithComments(doc, os);
            return os.toString("UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't generate XML", e);
        }
    }
    
    protected final void addAttribute(final Element attrs, final String key, final String value) {
        if (value == null) {
            return;
        }
        
        Element argument = attrs.getOwnerDocument().createElement(key);
        argument.appendChild(attrs.getOwnerDocument().createTextNode(value));
        attrs.appendChild(argument);
    }
    
    public static OAuthServiceResponse fromXML(InputStream is) {        
        try {
        	DocumentBuilder builder = Xml.newDocumentBuilder();
            
            Document doc = builder.parse(is);
            is.close();
            
            Element element = getFirstMatchingElement(doc.getDocumentElement(), "Response");
            
            String issuedString = element.getAttribute("Issued");
            
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            Date issued = formatter.parse(issuedString);
            
            long nowL = new Date().getTime();
            long issuedL = issued.getTime();
            
            // check that the difference in MS is acceptable
            long difference = nowL - issuedL;
            if (difference < 0) {
                difference = -difference;
            }
            
            if (difference > FIVE_MINUTES_IN_MS) {
                throw new IllegalArgumentException("Response expired - difference was " + difference + "ms");
            }
            
            String resource = element.getAttribute("Resource");
            String verb = getText(getFirstMatchingElement(element, "Verb"));
            
            Map<String, String> attributes = new HashMap<String, String>();
            NodeList args = getFirstMatchingElement(element, "Attributes").getChildNodes();
            for (int i = 0; i < args.getLength(); i++) {
                Node node = args.item(i);
                if (node.getNodeType() != Element.ELEMENT_NODE) {
                    continue;
                }
                
                attributes.put(node.getNodeName(), getText(node));
            }

            return new OAuthServiceResponse(verb, resource, attributes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't parse XML", e);
        }
    }

    public String getVerb() {
        return verb;
    }

    public String getResource() {
        return resource;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
