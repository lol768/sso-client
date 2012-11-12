/**
 * 
 */
package uk.ac.warwick.userlookup;

import org.apache.commons.httpclient.HttpClient;

interface HttpClientFactory {
	HttpClient getHttpClient();
}