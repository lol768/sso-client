package uk.ac.warwick.userlookup;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.Test;

public class UserLookupConfigTest {
	@After
	public void tidyup() {
		System.getProperties().remove("ssoclient.oncampus.ranges");
		UserLookup.setConfigProperties(null);
	}
	
	@Test
	public void nonexistent() {
		assertNull(UserLookup.getConfigProperty("this doesnt exist"));
	}
	
	@Test
	public void defaultValues() {
		assertEquals("137.,172.31,172.29", UserLookup.getConfigProperty("ssoclient.oncampus.ranges"));
	}
	
	@Test
	public void overrideDefaultsFromSystemProperties() {
		System.setProperty("ssoclient.oncampus.ranges", "137.,192.");
		assertEquals("137.,192.", UserLookup.getConfigProperty("ssoclient.oncampus.ranges"));
	}
	
	@Test
	public void overrideDefaultsFromProperties() {
		Properties p = new Properties();
		p.setProperty("ssoclient.oncampus.ranges", "137.,123.");
		UserLookup.setConfigProperties(p);
		assertEquals("137.,123.", UserLookup.getConfigProperty("ssoclient.oncampus.ranges"));
	}
}
