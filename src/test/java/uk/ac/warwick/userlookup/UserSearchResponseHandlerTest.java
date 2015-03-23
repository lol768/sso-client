package uk.ac.warwick.userlookup;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import uk.ac.warwick.userlookup.SSOUserLookup.UserSearchResponseHandler;

public class UserSearchResponseHandlerTest extends TestCase {

	private UserSearchResponseHandler handler;
	
	protected void setUp() throws Exception {
		handler = new UserSearchResponseHandler();
	}
	
	public void testUsersAreRead() throws Exception {
		InputStream stream = getClass().getResourceAsStream("/resources/sso-userSearch-result.xml");

        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        response.setEntity(new InputStreamEntity(stream));

		handler.processResults(response);
		List<User> users = handler.getResult();
		assertEquals(3, users.size());
		
		//do return logindisabled users. we filter them out
		//later if we need to
		
		User firstUser = users.get(0);
		assertEquals("Craig", firstUser.getFirstName());
		User secondUser = users.get(1);
		assertEquals("Craig", secondUser.getFirstName());
	}

}
