package uk.ac.warwick.sso.client.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;

import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.ssl.PKCS1EncodedKeySpec;

/**
 * Mish mash of methods for manipulating key and certificate formats, encodings, and objects.
 */
public class KeyAndCertUtils {
	
	/*
	 * PKCS#1 RSAPrivateKey** (PEM header: BEGIN RSA PRIVATE KEY)
     * PKCS#8 PrivateKeyInfo* (PEM header: BEGIN PRIVATE KEY)
	 */
	public static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
	public static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
	public static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
	public static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";

	/**
	 * Takes some printable format data and converts it to DER by selecting the
	 * Base64 data between the given delimiters and decoding it to binary.
	 */
	public static byte[] extractPem(String data, String start, String end) throws UnsupportedEncodingException {
		int i0 = data.indexOf(start);
		int i1 = data.indexOf(end);
		if (i0 > -1 && i1 > -1) {
			String base64 = data.substring(i0+start.length(), i1);
			return new Base64().decode(base64.replaceAll("\\s", "").getBytes("ASCII"));
		}
		return null;
	}

	public static PrivateKey decodeRSAPrivateKey(InputStream stream) throws UnsupportedEncodingException,
			IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey k;
		String data = new String(KeyAndCertUtils.readToArray(stream), "ASCII");
		byte[] bytes = extractPem(data, PKCS1_HEADER, PKCS1_FOOTER);
		if ((bytes = extractPem(data, PKCS1_HEADER, PKCS1_FOOTER)) != null) {
			k = keyFactory.generatePrivate(new PKCS1EncodedKeySpec(bytes).getKeySpec());
		} else if ((bytes = extractPem(data, PKCS8_HEADER, PKCS8_FOOTER)) != null) {
			k = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
		} else {
			throw new IllegalArgumentException("Unrecognised key format, must be RSA PEM key in PKCS1 or PKCS8");
		}
		return k;
	}

	public static byte[] readToArray(InputStream dis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[4096];
			int r = -1;
			while ((r=dis.read(buf)) > -1) {
				bos.write(buf, 0, r);
			}
		} finally {
			dis.close();
		}
		return bos.toByteArray();
	}

}
