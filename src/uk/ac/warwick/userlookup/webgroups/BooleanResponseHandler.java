package uk.ac.warwick.userlookup.webgroups;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;
import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;

/**
 * <p>Implementation which expects a stream whose first character is either "t" or "f".</p>
 *
 * @author xusqac
 */
public final class BooleanResponseHandler implements ResultAwareWebServiceResponseHandler {
	private static final Logger LOGGER = Logger.getLogger(BooleanResponseHandler.class);
	private boolean _result;

	public void processResults(final InputStream fromServer) throws HandlerException {
		String firstChar = "f";
		try {
			firstChar = String.valueOf((char) fromServer.read());
		} catch (IOException e1) {
			LOGGER.error("Cannot read first char: " + e1);
		}
		_result = "t".equalsIgnoreCase(firstChar);
	}
	
	public Object getResult() {
		return Boolean.valueOf(_result);
	}
}
