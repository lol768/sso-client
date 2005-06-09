/*
 * Created on 11-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;

/**
 * @author Kieran Shaw
 * 
 */
public class ShireServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ShireServlet.class);

	private Configuration _config;

	public ShireServlet() {
		super();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req, res);

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws HttpException
	 */
	private void process(HttpServletRequest req, HttpServletResponse res) throws IOException, HttpException {

		String saml64 = req.getParameter("SAMLResponse");
		String target = req.getParameter("TARGET");

		ShireCommand command = new ShireCommand();

		AttributeAuthorityResponseFetcher fetcher = new AttributeAuthorityResponseFetcherImpl();
		fetcher.setConfig(_config);
		command.setAaFetcher(fetcher);
		command.setConfig(_config);
		Cookie cookie = command.process(saml64, target);

		if (cookie != null) {
			LOGGER.debug("Adding SSC (" + cookie.getValue() + " ) to response");
			res.addCookie(cookie);
		} else {
			LOGGER.warn("No SSC cookie returned to client");
		}

		res.setHeader("Location", target);
		res.setStatus(302);

	}

	public void init(ServletConfig ctx) throws ServletException {
		super.init(ctx);

		_config = (Configuration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

	}
}
