package uk.ac.warwick.sso.client;

import org.springframework.util.StreamUtils;
import uk.ac.warwick.userlookup.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class DiagnosticServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Deliberately not using getUserFromRequest as that provides a default value -
        // here we want to fail if SSOClientFilter hasn't run.
        User user = (User) req.getAttribute(SSOClientFilter.getUserKey());
        String request = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
        resp.addHeader("Request-Body-Length", String.valueOf(request.length()));
        resp.addHeader("User-Found", String.valueOf(user.isFoundUser()));
    }
}
