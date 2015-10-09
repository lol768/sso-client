package uk.ac.warwick.userlookup;

import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceResponseHandler;


/**
 * @author xusqac
 */
public interface ResultAwareWebServiceResponseHandler<T> extends WebServiceResponseHandler {
	T getResult();
}
