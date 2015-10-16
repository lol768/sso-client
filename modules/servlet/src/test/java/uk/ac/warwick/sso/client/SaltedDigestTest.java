package uk.ac.warwick.sso.client;

import static org.junit.Assert.*;

import org.junit.Test;


public class SaltedDigestTest {
	@Test public void generate() {
		String hash = SaltedDigest.generate("hello");
		String hash2 = SaltedDigest.generate("hello");
		
		assertFalse(hash.equals(hash2));
		assertTrue(SaltedDigest.matches(hash, "hello"));
		assertTrue(SaltedDigest.matches(hash2, "hello"));
		
		// check there's no character encoding vulnerability
		String unicodeHash = SaltedDigest.generate("\u0999\u0999");
		assertFalse("Non-ASCII characters shouldn't be converted to question marks", SaltedDigest.matches(unicodeHash, "??"));
		assertTrue(SaltedDigest.matches(unicodeHash, "\u0999\u0999"));
	}
}
