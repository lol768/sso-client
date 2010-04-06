package uk.ac.warwick.userlookup.webgroups;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.HttpMethodWebService;
import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceException;
import uk.ac.warwick.userlookup.webgroups.WarwickGroupsService.ExecuteAndParseEngine;
import junit.framework.TestCase;

public class WarwickGroupsServiceUnitTest extends TestCase {
	
	/**
	 * If some web error occurs (can't retrieve HTTP response or can't parse it),
	 * we return an empty group.
	 * 
	 * I'm not sure this is the right behaviour, but I thought this test needed
	 * to exist to confirm that it does happen.
	 */
	public void testHttpExceptionThrown() throws Exception {
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
}
