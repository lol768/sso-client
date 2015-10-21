package uk.ac.warwick.sso.client.tags;

import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.sso.client.ShireCommand;
import uk.ac.warwick.userlookup.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

public class CSRFTokenTag extends BodyTagSupport {

    public final int doEndTag() throws JspException {
        User user = SSOClientFilter.getUserFromRequest((HttpServletRequest) pageContext.getRequest());

        String csrfToken = "";
        if (user != null && user.isFoundUser() && user.isLoggedIn()) {
            csrfToken = (String) user.getExtraProperty(ShireCommand.CSRF_TOKEN_PROPERTY_NAME);
        }

		try {
			pageContext.getOut().write(csrfToken);
		} catch (IOException e) {
			throw new JspTagException("IOException writing token to pageContext.getOut().write: " + e.toString());
		}

		return EVAL_PAGE;
	}

}
