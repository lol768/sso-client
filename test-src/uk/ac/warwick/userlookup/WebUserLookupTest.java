package uk.ac.warwick.userlookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class WebUserLookupTest extends TestCase {
	
	int port = 19099;
	
	int requests;
	
	public void testConnect() throws Exception {
		requests = 0;
		Handler handler = new AbstractHandler() {
			public void handle(String target, Request req,
					HttpServletRequest hreq, HttpServletResponse res)
					throws IOException, ServletException {
				requests++;
				String[] values = req.getParameterValues("user");
				PrintWriter writer = res.getWriter();
				for (int i=0; i<values.length; i++) {
					writer.println("returnType=4");
					writer.println("user="+values[i]);
					writer.println("-------");
				}
				req.setHandled(true);
			}
		};
		
		final List<String> manyUsers = new ArrayList<String>();
		for (int i=1; i<=250; i++) {
			manyUsers.add("cuxx"+i);
		}
		
		TestSentryServer.runServer(port, handler, new Runnable(){
			public void run() {
				try {
					WebServiceTimeoutConfig config = new WebServiceTimeoutConfig(0, 0);
					String version = null;
					String apiKey = null;
					WebUserLookup wul = new WebUserLookup("http://127.0.0.1:"+port, config, version, apiKey);
					Map<String, User> usersById = wul.getUsersById(manyUsers);
					assertEquals(3, requests);
					assertEquals(250, usersById.size());
				} catch (UserLookupException e) {
					e.printStackTrace();
					fail("Error looking up users");
				}
			}
		});
	}

	

}
