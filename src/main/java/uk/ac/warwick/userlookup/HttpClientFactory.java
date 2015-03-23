/**
 * 
 */
package uk.ac.warwick.userlookup;

import org.apache.http.client.HttpClient;

interface HttpClientFactory {
	HttpClient getHttpClient();
}