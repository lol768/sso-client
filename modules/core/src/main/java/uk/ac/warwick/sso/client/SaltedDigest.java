package uk.ac.warwick.sso.client;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;


/**
 * Salted hash used for client-side caching of passwords.
 */
abstract class SaltedDigest {

	private static final Charset PASSWORD_ENCODING = Charset.forName("UTF-8");
	// For turning base64 bytes to string, so we know ASCII is fine
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final String ALGORITHM = "SHA-1";
	
	/**
	 * Length of the salt. Changing this will break comparisons against
	 * ALL existing hashes. So, leave it alone :)
	 */
	private static final int SALT_LENGTH = 12;
	
	/**
	 * @param password The plain, unencrypted password input
	 * @return Whether this password matches this hash
	 */
	public static boolean matches(final String hash, final String password) {
		final String salt = hash.substring(0, SALT_LENGTH);
		return (hash.equals(computeHash(password, salt)));
	}
	
	public static String generate(final String password) {
		return computeHash(password, newSalt());
	}

	private static String computeHash(final String password, final String salt) {
		MessageDigest algorithm = null;
		try {
			algorithm = MessageDigest.getInstance(ALGORITHM);
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		final String string = salt + password;
		final byte[] input = string.getBytes(PASSWORD_ENCODING);
		return salt + base64(algorithm.digest(input));
	}

	private static String newSalt() {
		final byte[] bytes = new byte[SALT_LENGTH];
		new Random().nextBytes(bytes);
		final String salt = base64(bytes).substring(0, SALT_LENGTH);

		if (salt.length() != SALT_LENGTH) {
			throw new IllegalStateException("Salt wrong length - was " + salt.length());
		}

		return salt;
	}

	private static String base64(final byte[] input) {
		return Base64.getEncoder().encodeToString(input);
	}
}
