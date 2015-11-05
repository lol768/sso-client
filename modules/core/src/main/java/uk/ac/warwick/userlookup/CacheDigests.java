package uk.ac.warwick.userlookup;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This is for generating digests of sensitive values when caching them.
 */
public class CacheDigests {

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // for protection against rainbow tables and
    final private static byte[] salt = "websignoncachedigest".getBytes(Charset.forName("UTF-8"));

    public static String digest(String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            return bytesToHex(sha.digest(password.getBytes(Charset.forName("UTF-8"))));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
