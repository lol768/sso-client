/*
 * Created on 22-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.UserCache;
import uk.ac.warwick.userlookup.UserLookup;

public class LogoutServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class);

	protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		processRequest(arg0, arg1);
	}

	private void processRequest(final HttpServletRequest req, final HttpServletResponse res) throws IOException {

		java.io.PrintWriter out = res.getWriter();

		String SSC = req.getParameter("logoutTicket");
		if (SSC == null || SSC.equals("")) {
			out.println("false");
			LOGGER.info("Logout attempt failed because no ssc was passed in");
			return;
		}

		UserCache cache = UserLookup.getInstance().getUserCache();

		if (cache.get(SSC) != null) {
			cache.remove(SSC);
			out.println("true");
			LOGGER.info("Logout attempt succeeded as ssc (" + SSC + ") was found in cache");
			return;
		}

		LOGGER.info("Logout attempt failed because the ssc (" + SSC + ") was not found in the user cache");
		out.println("false");
		return;

	}

}
