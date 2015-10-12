package uk.ac.warwick.sso.client.oauth;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Element;

public class OAuthServiceRequestSecurityRuleChain implements
		OAuthServiceRequestSecurityRule {

	private final List<OAuthServiceRequestSecurityRule> rules;
	
	public OAuthServiceRequestSecurityRuleChain(List<OAuthServiceRequestSecurityRule> rules) {
		this.rules = rules;
	}

	public static OAuthServiceRequestSecurityRuleChain chainOf(OAuthServiceRequestSecurityRule... list) {
		return new OAuthServiceRequestSecurityRuleChain( Arrays.asList(list) );
	}

	public void run(HttpServletRequest req, List<X509Certificate> cert, XMLSignature signature, Element signedContent) {
		for (OAuthServiceRequestSecurityRule rule : rules) {
			rule.run(req, cert, signature, signedContent);
		}
	}

}
