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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

import uk.ac.warwick.sso.client.SSOConfiguration;

public class SSOLoginLinkTag extends BodyTagSupport {

	private String _target;

	public SSOLoginLinkTag() {
		super();
	}

	public final int doEndTag() throws JspException {

		// Configuration config = (Configuration)
		// pageContext.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

		Configuration config = (new SSOConfiguration()).getConfig();

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setConfig(config);
		generator.setTarget(_target);
		generator.setRequest((HttpServletRequest) pageContext.getRequest());

		try {
			String linkUrl = generator.getLoginUrl();
			pageContext.getOut().write(linkUrl);
		} catch (IOException e) {
			throw new JspTagException("IOException writing url to pageContext.getOut().write: " + e.toString());
		} catch (ConfigurationException e) {
			throw new JspTagException("ConfigurationException getting logout url: " + e.toString());
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
