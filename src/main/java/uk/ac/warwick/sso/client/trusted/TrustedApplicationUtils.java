package uk.ac.warwick.sso.client.trusted;

import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;

public abstract class TrustedApplicationUtils {

    /**
     * Generate a string to use when verify the received signature.
     * the format of the base string depends on the protocol version.
     *
     * @param timestamp the received timestamp.
     * @param requestUrl the url the request was received on.
     * @param username the received username.
     * @return a protocol specific concatenation of the parameters.
     */
    public static byte[] generateSignatureBaseString(DateTime timestamp, String requestUrl, String username) throws UnsupportedEncodingException {
        if (requestUrl != null && requestUrl.contains(EncryptionProvider.SIGNATURE_BASE_SEPARATOR)) {
            throw new IllegalStateException("URL to sign contains illegal character [" + EncryptionProvider.SIGNATURE_BASE_SEPARATOR + "]");
        }

        if (username != null && username.contains(EncryptionProvider.SIGNATURE_BASE_SEPARATOR)) {
            throw new IllegalStateException("Username contains illegal character [" + EncryptionProvider.SIGNATURE_BASE_SEPARATOR + "]");
        }

        String signatureBaseString =
            Long.toString(timestamp.getMillis()) +
            EncryptionProvider.SIGNATURE_BASE_SEPARATOR +
            requestUrl +
            EncryptionProvider.SIGNATURE_BASE_SEPARATOR +
            username;

        return signatureBaseString.getBytes("UTF-8");
    }

}
