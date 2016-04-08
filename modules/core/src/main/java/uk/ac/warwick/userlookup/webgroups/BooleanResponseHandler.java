package uk.ac.warwick.userlookup.webgroups;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.userlookup.ClearGroupResponseHandler;
import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;

/**
 * <p>Implementation which expects a stream whose first character is either "t" or "f".</p>
 */
final class BooleanResponseHandler extends ClearGroupResponseHandler implements ResultAwareWebServiceResponseHandler<Boolean> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BooleanResponseHandler.class);
	private boolean _result;

	public void processResults(final HttpResponse response) throws HandlerException {
		String firstChar = "f";
		try {
			firstChar = String.valueOf((char) response.getEntity().getContent().read());
		} catch (IOException e1) {
			LOGGER.error("Cannot read first char: ", e1);
		}
		_result = "t".equalsIgnoreCase(firstChar);
	}
	
	public Boolean getResult() {
		return Boolean.valueOf(_result);
	}
}
