package uk.ac.warwick.sso.client.core;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;


import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ServletRequestAdapterTest {

    @Test
    public void getQueryParameter() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("field1=value1&forcebasic&field2=value2&field2=value3");

        ServletRequestAdapter request = new ServletRequestAdapter(servletRequest);
        assertEquals(asList("value1"), request.getQueryParameter("field1"));
        assertEquals(asList("value2","value3"), request.getQueryParameter("field2"));
        assertEquals(asList(""), request.getQueryParameter("forcebasic"));
        assertEquals(Collections.emptyList(), request.getQueryParameter("nonsense"));
    }
}