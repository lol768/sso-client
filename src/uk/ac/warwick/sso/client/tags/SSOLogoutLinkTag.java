/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.configuration.Configuration;
import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

import uk.ac.warwick.sso.client.SSOConfiguration;

public class SSOLogoutLinkTag extends BodyTagSupport {

	private String _target;

	public SSOLogoutLinkTag() {
		super();
	}

	public final int doEndTag() throws JspException {

		// Configuration config = (Configuration)
		// pageContext.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

		Configuration config = SSOConfiguration.getConfig();

		SSOLogoutLinkGenerator generator = new SSOLogoutLinkGenerator();
		generator.setConfig(config);
		generator.setTarget(_target);
		generator.setRequest((HttpServletRequest) pageContext.getRequest());

		try {
			String linkUrl = generator.getLogoutUrl();
			pageContext.getOut().write(linkUrl);
		} catch (IOException e) {
			throw new JspTagException("IOException writing url to pageContext.getOut().write: " + e.toString());
		}

		return EVAL_PAGE;
	}

	public final void setTarget(final String target) throws JspTagException {
		try {
			_target = (String) ExpressionEvaluatorManager.evaluate("target", target, String.class, pageContext);
		} catch (JspException e) {
			throw new JspTagException("Error:" + e.toString());
		}
	}

	public final String getTarget() {
		return _target;
	}

}
