/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLResponse;

import uk.ac.warwick.sso.client.cache.InMemoryUserCache;

import junit.framework.TestCase;

public class ShireCommandTests extends TestCase {

	public final void testShireCommand() throws Exception {

		ShireCommand command = new ShireCommand();

		Configuration config = new XMLConfiguration(getClass().getResource("/sso-config.xml"));

		MockAttributeAuthorityResponseFetcher fetcher = new MockAttributeAuthorityResponseFetcher();
		fetcher.setConfig(config);

		SAMLResponse resp = generateMockResponse();
		fetcher.setResponse(resp);
		command.setAaFetcher(fetcher);

		String saml64 = "PFJlc3BvbnNlIHhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoxLjA6cHJvdG9jb2wiIHht%0D%0AbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjEuMDphc3NlcnRpb24iIHhtbG5zOnNh%0D%0AbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoxLjA6cHJvdG9jb2wiIElzc3VlSW5zdGFudD0i%0D%0AMjAwNi0wNS0xOFQwOTozNTowNS4xNDNaIiBNYWpvclZlcnNpb249IjEiIE1pbm9yVmVyc2lvbj0i%0D%0AMSIgUmVjaXBpZW50PSJodHRwczovL21vbGVtYW4ud2Fyd2ljay5hYy51ay9vcmlnaW4vc2hpcmUi%0D%0AIFJlc3BvbnNlSUQ9ImIxMDMzZTcyNTBhNWNiY2FjZDg0NDhkOTFiMjNhNjQyIj48ZHM6U2lnbmF0%0D%0AdXJlIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj4KPGRzOlNp%0D%0AZ25lZEluZm8%2BCjxkczpDYW5vbmljYWxpemF0aW9uTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3%0D%0Ady53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIj48L2RzOkNhbm9uaWNhbGl6YXRpb25NZXRo%0D%0Ab2Q%2BCjxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAw%0D%0ALzA5L3htbGRzaWcjcnNhLXNoYTEiPjwvZHM6U2lnbmF0dXJlTWV0aG9kPgo8ZHM6UmVmZXJlbmNl%0D%0AIFVSST0iI2IxMDMzZTcyNTBhNWNiY2FjZDg0NDhkOTFiMjNhNjQyIj4KPGRzOlRyYW5zZm9ybXM%2B%0D%0ACjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRz%0D%0AaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSI%2BPC9kczpUcmFuc2Zvcm0%2BCjxkczpUcmFuc2Zvcm0gQWxn%0D%0Ab3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiPjxlYzpJbmNs%0D%0AdXNpdmVOYW1lc3BhY2VzIHhtbG5zOmVjPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1l%0D%0AeGMtYzE0biMiIFByZWZpeExpc3Q9ImNvZGUgZHMga2luZCBydyBzYW1sIHNhbWxwIHR5cGVucyAj%0D%0AZGVmYXVsdCI%2BPC9lYzpJbmNsdXNpdmVOYW1lc3BhY2VzPjwvZHM6VHJhbnNmb3JtPgo8L2RzOlRy%0D%0AYW5zZm9ybXM%2BCjxkczpEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8y%0D%0AMDAwLzA5L3htbGRzaWcjc2hhMSI%2BPC9kczpEaWdlc3RNZXRob2Q%2BCjxkczpEaWdlc3RWYWx1ZT5E%0D%0AejZ6aVd5Q1F3bmRZMFZZT2IyODhrb0xVQlk9PC9kczpEaWdlc3RWYWx1ZT4KPC9kczpSZWZlcmVu%0D%0AY2U%2BCjwvZHM6U2lnbmVkSW5mbz4KPGRzOlNpZ25hdHVyZVZhbHVlPgp0U3l2aUJ0dVNRQU5LWmNv%0D%0ARHZodFVOTHBwS0NSeW1DWVRMYzE3YTBFSzNhMzJ3Um53eDdLSXBsRVJScHM5U2pUTCs5L2diWWZx%0D%0AYVlrCjFXd0pYeGtKOEorbFNxaDZtOFZjOThmM2xCc28vR0RlejJJNGFhaW91blhKK3haZVg4Vld5%0D%0AdlNGekN2WXdpOGNLZCtscXdOMldQeVIKRUNOYzhVTG5UOWU1c3F4b0lvOD0KPC9kczpTaWduYXR1%0D%0AcmVWYWx1ZT4KPGRzOktleUluZm8%2BCjxkczpYNTA5RGF0YT4KPGRzOlg1MDlDZXJ0aWZpY2F0ZT4K%0D%0ATUlJRE16Q0NBaHVnQXdJQkFnSUJCVEFOQmdrcWhraUc5dzBCQVFRRkFEQ0JtakVUTUJFR0ExVUVB%0D%0AeE1LVkdWemRGTlRUeUJEUVRFZQpNQndHQTFVRUNoTVZWVzVwZG1WeWMybDBlU0J2WmlCWFlYSjNh%0D%0AV05yTVJRd0VnWURWUVFMRXd0SlZDQlRaWEoyYVdObGN6RVdNQlFHCkExVUVDQk1OVjJWemRDQk5h%0D%0AV1JzWVc1a2N6RUxNQWtHQTFVRUJoTUNSMEl4S0RBbUJna3Foa2lHOXcwQkNRRVdHV3RwWlhKaGJp%0D%0ANXoKYUdGM1FIZGhjbmRwWTJzdVlXTXVkV3N3SGhjTk1EVXdOakk0TVRVMU16UTFXaGNOTURZd05q%0D%0ASTRNVFUxTXpRMVdqQ0JrekVlTUJ3RwpBMVVFQXhNVmJXOXNaVzFoYmk1M1lYSjNhV05yTG1Gakxu%0D%0AVnJNU0l3SUFZRFZRUUtFeGxVYUdVZ1ZXNXBkbVZ5YzJsMGVTQnZaaUJYCllYSjNhV05yTVNnd0pn%0D%0AWURWUVFMRXg5SmJtWnZjbTFoZEdsdmJpQlVaV05vYm05c2IyZDVJRk5sY25acFkyVnpNUll3RkFZ%0D%0ARFZRUUkKRXcxWFpYTjBJRTFwWkd4aGJtUnpNUXN3Q1FZRFZRUUdFd0pIUWpDQm56QU5CZ2txaGtp%0D%0ARzl3MEJBUUVGQUFPQmpRQXdnWWtDZ1lFQQo0Vlp5Q0lPeVh0aVpQWlh3Qy9SMWhZb0NYZS9iMk5T%0D%0AQXd6UmJoc3djcEhmM3dqdE82MzJ2ZTFSTWZjRWpsZ3NvTGZMUGkzak1peFVXCmRvM0padkYxUHl1%0D%0AOTZ3M3ovaURkRmhsejlFSTNMZzJlbUNLQkw0TDNFQ3BvQkJiMnROb01GdTRZcmJzR28xczUwMmlz%0D%0ANi95NG9nL2cKamFHS3V3M1JKSVBDQzRrTzJ0RUNBd0VBQWFNTk1Bc3dDUVlEVlIwVEJBSXdBREFO%0D%0AQmdrcWhraUc5dzBCQVFRRkFBT0NBUUVBVG9tZwpxbU1oQ2NPYXU0a29HMzVYSXhOa2lCRTFzVW51%0D%0ASm0zdkJBMGpXNXI4dXBOMDYwekxxSTQ4QWQvbXRyYnJwQ2Z6dUhzdmNlN1pKMm9oCnd2anFPU2hx%0D%0AUnBGeU9ZWG0zNXJvZ2dHYzRzZmsveFo4WEVyVitPZ3B5d0xZTEJmU29uanFMenM0TlZRYVhrb2t6%0D%0AMTlxNGJDVWRNTHEKVUpwNHFob2hDWk5lRkw5RTNBUUJnUFZMV3BkRnRnNlYyR0x5bTdIcStCeWhJ%0D%0AMnhUYkJSWkdGOU55MDF2bjAvbUJCd3h0b01LTUxoUwpMOWgyNTU2MGR6VXVUYnhBNWdTYk93WVFK%0D%0AVCtHWjZoMXQ1cmhORXI4a0dXM3Q0bkRQeStMREpTcE9GK3kwMG5DcnhNeFZHMkEzK3BhClRBcEM3%0D%0AZXdxUmtiTTMxNzh0SE1EVnBXbytjWFBIUU90SHc9PQo8L2RzOlg1MDlDZXJ0aWZpY2F0ZT4KPC9k%0D%0AczpYNTA5RGF0YT4KPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxTdGF0dXM%2BPFN0YXR1c0Nv%0D%0AZGUgVmFsdWU9InNhbWxwOlN1Y2Nlc3MiPjwvU3RhdHVzQ29kZT48L1N0YXR1cz48QXNzZXJ0aW9u%0D%0AIHhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoxLjA6YXNzZXJ0aW9uIiBBc3NlcnRpb25J%0D%0ARD0iZGM0ZjVhMmViYTUwOTAyNGZkMDgwMzUxYjFiMDVkMzEiIElzc3VlSW5zdGFudD0iMjAwNi0w%0D%0ANS0xOFQwOTozNTowNS4xNDNaIiBJc3N1ZXI9InVybjphdGhlbnNhbXMubmV0OnRlc3RGZWRlcmF0%0D%0AaW9uOm1vbGVtYW4ud2Fyd2ljay5hYy51ayIgTWFqb3JWZXJzaW9uPSIxIiBNaW5vclZlcnNpb249%0D%0AIjEiPjxDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAwNi0wNS0xOFQwOTozNTowNS4xNDNaIiBOb3RP%0D%0Abk9yQWZ0ZXI9IjIwMDYtMDUtMThUMDk6NDA6MDUuMTQzWiI%2BPEF1ZGllbmNlUmVzdHJpY3Rpb25D%0D%0Ab25kaXRpb24%2BPEF1ZGllbmNlPnVybjptb2xlbWFuLndhcndpY2suYWMudWs6c3NvOnNlcnZpY2U8%0D%0AL0F1ZGllbmNlPjwvQXVkaWVuY2VSZXN0cmljdGlvbkNvbmRpdGlvbj48L0NvbmRpdGlvbnM%2BPEF1%0D%0AdGhlbnRpY2F0aW9uU3RhdGVtZW50IEF1dGhlbnRpY2F0aW9uSW5zdGFudD0iMjAwNi0wNS0xOFQw%0D%0AOTozNTowNS4xMTFaIiBBdXRoZW50aWNhdGlvbk1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNB%0D%0ATUw6MS4wOmFtOnVuc3BlY2lmaWVkIj48U3ViamVjdD48TmFtZUlkZW50aWZpZXIgRm9ybWF0PSJ1%0D%0Acm46d2Vic2lnbm9uOnV1aWQiIE5hbWVRdWFsaWZpZXI9InVybjphdGhlbnNhbXMubmV0OnRlc3RG%0D%0AZWRlcmF0aW9uOm1vbGVtYW4ud2Fyd2ljay5hYy51ayI%2BcWRneWRzZWtsbHB5dGptZGN3ZHBmeW14%0D%0AaWVodGRtcmZpbmdycnByYWxwbWd4dHB3cXA8L05hbWVJZGVudGlmaWVyPjxTdWJqZWN0Q29uZmly%0D%0AbWF0aW9uPjxDb25maXJtYXRpb25NZXRob2Q%2BdXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4wOmNt%0D%0AOmJlYXJlcjwvQ29uZmlybWF0aW9uTWV0aG9kPjwvU3ViamVjdENvbmZpcm1hdGlvbj48L1N1Ympl%0D%0AY3Q%2BPFN1YmplY3RMb2NhbGl0eSBJUEFkZHJlc3M9IjEzNy4yMDUuMTk0LjIyMiI%2BPC9TdWJqZWN0%0D%0ATG9jYWxpdHk%2BPC9BdXRoZW50aWNhdGlvblN0YXRlbWVudD48L0Fzc2VydGlvbj48L1Jlc3BvbnNl%0D%0APg%3D%3D";
		String target = "https%3A%2F%2Fmoleman.warwick.ac.uk%2Forigin%2Fsysadmin%2Fviewauthlogs.htm";

		saml64 = URLDecoder.decode(saml64, "UTF-8");
		target = URLDecoder.decode(target, "UTF-8");

		command.setConfig(config);
		command.setCache(new InMemoryUserCache());
		Cookie cookie = command.process(saml64, target);

		assertNotNull(cookie);
		
		assertEquals("Should have right cookie","Testing123",cookie.getValue());

	}

	/**
	 * @return
	 * @throws SAMLException
	 */
	private SAMLResponse generateMockResponse() throws SAMLException {
		SAMLResponse resp = new SAMLResponse();
		SAMLAssertion assertion = new SAMLAssertion();
		SAMLAttributeStatement statement = new SAMLAttributeStatement();
		List values = new ArrayList();
		values.add("SSC_VALUE");
		SAMLAttribute attr = new SAMLAttribute("urn:websignon:ssc",null,null,0,values);
		statement.addAttribute(attr);
		assertion.addStatement(statement);
		resp.addAssertion(assertion);
		return resp;
	}

}
