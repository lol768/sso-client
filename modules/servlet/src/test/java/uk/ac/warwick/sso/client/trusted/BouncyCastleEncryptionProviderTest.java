package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import uk.ac.warwick.util.core.DateTimeUtils;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class BouncyCastleEncryptionProviderTest {

    private static final String PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmP5gpPXUwXix9MieRN1bgI4odNVuRkPj+nmyKiLZ5zNhH" +
        "cecKzL9YFOf4LGvCjaukOt9thYdUBtgUdW0NjqqvtQ9PcnfDlNSdgfX/ag4X7WqKbJ8M+Z2yYsfQb32ItJYe/01OO" +
        "U6/k6twt1X4qIAIvJsflBh6Qqtjualwek28kSGCtC1JC/4m2pLvWrfxJqZF8UrMeZt2Zge0TanXz1Dnbjnz8dAH61" +
        "fABf7iFlCgkNn6U6jlYCoke9hMVMIWUe9Q5oBAre/jg5n8oCVLINgK7wWpIovBIT8foB7PL9SuM6gNkmtTVSBIvX3" +
        "t+P8ghvi8xafgoicl06PzwOwthYCPwIDAQAB";

    private static final String PRIVATE_KEY =
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCY/mCk9dTBeLH0yJ5E3VuAjih01W5GQ+P6ebIqI" +
        "tnnM2Edx5wrMv1gU5/gsa8KNq6Q6322Fh1QG2BR1bQ2Oqq+1D09yd8OU1J2B9f9qDhftaopsnwz5nbJix9BvfYi0l" +
        "h7/TU45Tr+Tq3C3VfiogAi8mx+UGHpCq2O5qXB6TbyRIYK0LUkL/ibaku9at/EmpkXxSsx5m3ZmB7RNqdfPUOduOf" +
        "Px0AfrV8AF/uIWUKCQ2fpTqOVgKiR72ExUwhZR71DmgECt7+ODmfygJUsg2ArvBakii8EhPx+gHs8v1K4zqA2Sa1N" +
        "VIEi9fe34/yCG+LzFp+CiJyXTo/PA7C2FgI/AgMBAAECggEAIy4rTwNwXuTAFweizTcReWg3CVaiuumVnN0rCOFmt" +
        "fFsnYpu8MgS13mjQ+nX1ENqtMxR5fMD3o3NAkRf4jBvXt4zDuhCsGqchaOcGSn7fJargFcYlF6kZgflshpaZPt1eV" +
        "1qRaEAhcXV0v9O3EBgQ6j3JbyaJxpbeoazCvnztpWLJh6CIPxjC8h9JG5H59z4TZTL/zZjaGxyNY+Xt9wvS9DNHVF" +
        "xNgy0/mS0dafjLyjzqNi2e+hxz0C59BABO7d/iSlqvQHyxxVeu3w1+xntFrKUsNYuFckc0OpWaC49CbTURxCUbL2W" +
        "odnTbT6QFgexas74+QYMbdSiXxe8F0Jn8QKBgQDmNNra9XHAIx7xUsCtBTDxO6EiepkcLjRKLgMCxjfbRBOI2Y0o1" +
        "HRLSAaAXXBwvrQ23ezlC/XONwz9D9wH81tJ/IRwyM+OZKbcLfj81TVVNTuUQBBaNuPvf7mq1vaSvrkxflXhzHT5cF" +
        "9M7rc8i+2dKLs+6d9RlVBB0k01tuloEwKBgQCqIsh94bakx1PdecsdNIr7bbQ67juS2nKQnq+7KpIcjWwBdhCH5Ri" +
        "dBsJOLcqZ/lVaQmFzK7AyL1qBZ1FV4igwz9EJtnsaR3g7ECQ6WDCVXZZMLbpMvBybWV4mNYdt/yXxy4qMusQWzxXq" +
        "O4i7II0SWBfW1CJApcSLSEVbuRwapQKBgCeWU0RwDN2jrICHYIbga6gwPud0+bt03p0bCH2DpLtaG5ne/31T+6Oug" +
        "R+18c4RnWAKDeDdi6mood0qywW6/andeNEEV1z/Rgp7BWRFLeS3QMWftrAs3EmlR0JvsPtPPP2b4hzwUfVLM7hBHN" +
        "WWoofyJzEMetDGwoRvK8Pe7ohtAoGBAJeQWwmBXWMXy0dfK6m92B46Ube56Upe3nalVymmt/lFpzT6B6n4Vl/02H4" +
        "q3vUmlMTOo9+kyNc8RiVHdDbNPT6Ws8MyVCJKDvqW2586VzWI5M7CYdfgMJ/YEj55q0c7aIMp7yiFbRBgtUYweRMy" +
        "4Vm5LquL2WO8CQaHgHpAwp+dAoGAVN5iVD8mZIzl7+lYBy9qbzB13xHCP9Z+AHVuUO2/djO6VRR4j1gDEC8RLICr1" +
        "ue0bObmHY1FzFh1ClC4Bh+Kd4bERviu3C0p+wv5Mau+TW1dcAqcrlmZS9rNEHNmgBXZbBbMTS4x2/vmjwnrQ6R7/e" +
        "1szkwZnureQ30ObXrOoMs=";

    private final BouncyCastleEncryptionProvider provider = new BouncyCastleEncryptionProvider();

    @Test
    public void testToPublicKey() throws Exception {
        PublicKey publicKey = provider.toPublicKey(Base64.decode(PUBLIC_KEY));
        assertNotNull(publicKey);

        String base64Encoded = new String(Base64.encode(publicKey.getEncoded()), "UTF-8");
        assertEquals(PUBLIC_KEY, base64Encoded);
    }

    @Test
    public void testToPrivateKey() throws Exception {
        PrivateKey privateKey = provider.toPrivateKey(Base64.decode(PRIVATE_KEY));
        assertNotNull(privateKey);

        String base64Encoded = new String(Base64.encode(privateKey.getEncoded()), "UTF-8");
        assertEquals(PRIVATE_KEY, base64Encoded);
    }

    @Test
    public void testGenerateSignature() throws Exception {
        ZonedDateTime base = ZonedDateTime.of(2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384000000, ZoneId.of("UTC"));
        String url = "http://warwick.ac.uk?external=true";
        String username = "cuscav";

        PrivateKey privateKey = provider.toPrivateKey(Base64.decode(PRIVATE_KEY));
        byte[] signatureBase = TrustedApplicationUtils.generateSignatureBaseString(base, url, username);

        String expectedSignature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";
        String signature = provider.generateSignature(privateKey, signatureBase);

        assertEquals(expectedSignature, signature);
    }

    @Test
    public void testVerifySignature() throws Exception {
        ZonedDateTime base = ZonedDateTime.of(2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384000000, ZoneId.of("UTC"));
        String url = "http://warwick.ac.uk?external=true";
        String username = "cuscav";

        PublicKey publicKey = provider.toPublicKey(Base64.decode(PUBLIC_KEY));
        PrivateKey privateKey = provider.toPrivateKey(Base64.decode(PRIVATE_KEY));
        byte[] signatureBase = TrustedApplicationUtils.generateSignatureBaseString(base, url, username);

        String signature = provider.generateSignature(privateKey, signatureBase);

        assertTrue(provider.verifySignature(publicKey, signatureBase, signature));
    }

    @Test
    public void testVerifySignatureKeyMismatch() throws Exception {
        PublicKey mismatchedPublicKey = KeyPairGenerator.getInstance("RSA", BouncyCastleEncryptionProvider.PROVIDER).generateKeyPair().getPublic();

        ZonedDateTime base = ZonedDateTime.of(2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384000000, ZoneId.of("UTC"));
        String url = "http://warwick.ac.uk?external=true";
        String username = "cuscav";

        PrivateKey privateKey = provider.toPrivateKey(Base64.decode(PRIVATE_KEY));
        byte[] signatureBase = TrustedApplicationUtils.generateSignatureBaseString(base, url, username);

        String signature = provider.generateSignature(privateKey, signatureBase);

        assertFalse(provider.verifySignature(mismatchedPublicKey, signatureBase, signature));
    }

    @Test
    public void testCreateEncryptedCertificate() throws Exception {
        final String username = "cuscav";
        final String providerID = "urn:tabula.warwick.ac.uk:tabula:service";
        final PrivateKey privateKey = provider.toPrivateKey(Base64.decode(PRIVATE_KEY));
        final ZonedDateTime base = ZonedDateTime.of(2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384000000, ZoneId.of("UTC"));
        final String url = "http://warwick.ac.uk?external=true";

        final String expectedCertString = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";

        final String expectedSignature =
            "E2DIaXFajtZU1abI51sWR+5WomVPYt/RpYLDA0XTkTfxATYm3cyX7IQPM8A9ZmkPBpHqKqG6pz9YBXraARhB9Fwjx+" +
            "skXI4GY5SCFJeosq3NDjj4Nkp5mFS8270hYsGisxQaoz9CwEnMT490DxqIB6ay801JGHXY68GSs0Cfv22IGumn3GhZ" +
            "3TYxaGHv63QYUsGATINoHlNkbnqmT5RfbnmywAb24rLrU5Scxa8Up3XWBNpmflmF//JybOhufRk7ewDLmtpfFFdwi6" +
            "elBjYtofUekVbxK811zzp1yd/IUhxq9nkODIMeSMYRdrZUCJcdJ963RCQBixzCxmkfN7Wiyw==";

        DateTimeUtils.useMockDateTime(base.toInstant(), () -> {
			try {
				EncryptedCertificate cert = provider.createEncryptedCertificate(username, privateKey, providerID, url);

				assertEquals(expectedCertString, cert.getCertificate());
				assertEquals(providerID, cert.getProviderID());
				assertEquals(expectedSignature, cert.getSignature());
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		});
    }

    @Test
    public void testDecodeEncryptedCertificate() throws Exception {
        String providerID = "urn:tabula.warwick.ac.uk:tabula:service";
        String certString = "MTQxOTQ5OTg4OTM4NApjdXNjYXY=";

        EncryptedCertificate cert = new EncryptedCertificateImpl(providerID, certString);

        PublicKey publicKey = provider.toPublicKey(Base64.decode(PUBLIC_KEY));
        ApplicationCertificate appCert = provider.decodeEncryptedCertificate(cert, publicKey, providerID);

        assertEquals(
                ZonedDateTime.of(2014, Month.DECEMBER.getValue(), 25, 9, 31, 29, 384000000, ZoneId.of("UTC")),
                appCert.getCreationTime()
        );
        assertEquals(providerID, appCert.getProviderID());
        assertEquals("cuscav", appCert.getUsername());
    }
}