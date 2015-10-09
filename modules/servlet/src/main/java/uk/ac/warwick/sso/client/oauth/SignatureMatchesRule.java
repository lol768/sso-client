package uk.ac.warwick.sso.client.oauth;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.w3c.dom.Element;

public class SignatureMatchesRule implements OAuthServiceRequestSecurityRule {

	public void run(HttpServletRequest req, List<X509Certificate> cert, XMLSignature signature, Element signedContent) {
		try {
			if (!signature.checkSignatureValue(cert.get(0).getPublicKey())) {
			    throw new IllegalArgumentException("Invalid signature");
			}
		} catch (XMLSignatureException e) {
			throw new IllegalArgumentException("Couldn't validate signature", e);
		}
	}

}
