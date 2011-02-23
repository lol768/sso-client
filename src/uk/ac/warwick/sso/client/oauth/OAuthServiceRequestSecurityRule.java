package uk.ac.warwick.sso.client.oauth;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Element;

public interface OAuthServiceRequestSecurityRule {
	void run(HttpServletRequest req, List<X509Certificate> cert, XMLSignature signature, Element signedContent);
}