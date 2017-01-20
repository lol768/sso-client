package uk.ac.warwick.sso.client;

import uk.ac.warwick.sso.client.core.HttpRequest;
import uk.ac.warwick.sso.client.core.Response;

import java.io.IOException;

public interface SSOHandler {

    Response handle(HttpRequest request) throws IOException;

}
