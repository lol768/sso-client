package uk.ac.warwick.userlookup.webgroups;

import org.apache.http.message.BasicHttpResponse;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;
import uk.ac.warwick.userlookup.webgroups.WarwickGroupsService.ExecuteAndParseEngine;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
//import static org.hamcrest.collection.

public class WarwickGroupsServiceUnitTest {
	
	/**
	 * If some web error occurs (can't retrieve HTTP response or can't parse it),
	 * we return an empty group.
	 * 
	 * I'm not sure this is the right behaviour, but I thought this test needed
	 * to exist to confirm that it does happen.
	 */
	@Test
	public void httpExceptionThrown() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("http://example.com");
		service.setEngine(new ExecuteAndParseEngine() {
			public void execute(String urlPath, ResultAwareWebServiceResponseHandler<?> handler)
					throws WebServiceException {
				throw new HttpMethodWebService.WebServiceException("http error is a happen, oh bother");
			}
		});
		
		try {
			service.getGroupByName("in-serbia");
			fail("Expected exception");
		} catch (GroupServiceException e) {
			//hooray
		}
	}

	@Test
	public void badInputs() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("http://example.com");
		service.setEngine(new ExecuteAndParseEngine() {
			public void execute(String urlPath, ResultAwareWebServiceResponseHandler<?> handler) throws WebServiceException {
				throw new HttpMethodWebService.WebServiceException("If you get this, the code isn't rejecting the bad inputs as it should be.");
			}
		});

		// Any forward slash in an API call should fail fast and return nothing.
		// we're checking that the request engine is never called.
		try {
			service.getGroupByName("/nope");
		} catch (GroupNotFoundException gnfe) {}

		try {
			service.getGroupInfo("cd /home");
		} catch (GroupNotFoundException gnfe) {}

		assertThat( service.getGroupsForDeptCode("%2Fdeptcode"), is(empty()) );
		assertThat( service.getGroupsForQuery("a/a"), is(empty()) );
		assertThat( service.getGroupsForUser("ads/user"), is(empty()) );
		assertThat( service.getGroupsNamesForUser("a///a"), is(empty()) );
		assertThat(service.getRelatedGroups("a/a"), is(empty()));
	}


	static class PathRecordingEngine implements ExecuteAndParseEngine {
		public String path;
		@Override
		public void execute(String urlPath, ResultAwareWebServiceResponseHandler<?> handler) throws WebServiceException {
			path = urlPath;
			throw new HttpMethodWebService.WebServiceException("No generic way to handle this here");
		}
	}

	@Test
	public void escaping() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("http://example.com");
		PathRecordingEngine engine = new PathRecordingEngine();
		service.setEngine(engine);

		// Any forward slash in an API call should fail fast and return nothing.
		// we're checking that the request engine is never called.
		try {
			service.getGroupByName("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/group/good%20egg%3F/details", engine.path);

		try {
			service.getGroupInfo("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/group/good%20egg%3F/info", engine.path);

		try {
			service.getGroupsForDeptCode("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/search/deptcode/good%20egg%3F", engine.path);

		try {
			service.getGroupsForQuery("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/search/name/good%20egg%3F", engine.path);

		try {
			service.getGroupsForUser("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/user/good%20egg%3F/groups", engine.path);

		try {
			// pass in a + to check it is treated as a space for backcompat
			service.getGroupsNamesForUser("goodly+egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/user/goodly%20egg%3F/groups", engine.path);

		try {
			service.getRelatedGroups("good egg?");
		} catch (GroupServiceException e) {}
		assertEquals("http://example.com/query/group/good%20egg%3F/groups", engine.path);
	}
}
