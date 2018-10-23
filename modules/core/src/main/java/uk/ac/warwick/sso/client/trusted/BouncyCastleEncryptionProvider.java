package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class BouncyCastleEncryptionProvider implements EncryptionProvider {

    public static Clock CLOCK_IMPLEMENTATION = Clock.systemDefaultZone();

    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    public static final Provider PROVIDER = new BouncyCastleProvider();

    private static final Logger LOGGER = LoggerFactory.getLogger(BouncyCastleEncryptionProvider.class);

    private static final String ASYM_CIPHER = "RSA/NONE/NoPadding";

    private static final String ASYM_ALGORITHM = "RSA";

    @Override
    public String generateSignature(PrivateKey privateKey, byte[] signatureBaseString) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        sig.initSign(privateKey);
        sig.update(signatureBaseString);
        return new String(Base64.encode(sig.sign()), "UTF-8");
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, byte[] signatureBaseString, String signatureToVerify) throws SignatureVerificationFailedException {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
            sig.initVerify(publicKey);
            sig.update(signatureBaseString);
            return sig.verify(Base64.decode(signatureToVerify));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception verifying signature " + signatureToVerify, e);
            }

            throw new SignatureVerificationFailedException(e);
        }
    }

    @Override
    public PublicKey toPublicKey(byte[] encodedForm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encodedForm);
        final KeyFactory keyFactory = KeyFactory.getInstance(ASYM_ALGORITHM, PROVIDER);
        return keyFactory.generatePublic(pubKeySpec);
    }

    @Override
    public PrivateKey toPrivateKey(byte[] encodedForm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedForm);
        final KeyFactory keyFactory = KeyFactory.getInstance(ASYM_ALGORITHM, PROVIDER);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    @Override
    public ApplicationCertificate decodeEncryptedCertificate(EncryptedCertificate encCert, PublicKey publicKey, String providerID) throws InvalidCertificateException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(Base64.decode(encCert.getCertificate())), "UTF-8"));

            final String created = in.readLine();
            final String username = in.readLine();

            final ZonedDateTime timeCreated = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(created)), ZoneId.systemDefault());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Application certificate decrypted: providerID=%s, username=%s, timeCreated=%s,", providerID, username, timeCreated.toString()));
            }

            return new ApplicationCertificateImpl(providerID, username, timeCreated);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception decoding encrypted certificate", e);
            }

            throw new InvalidCertificateException(new TransportErrorMessage.System(e, providerID));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public EncryptedCertificate createEncryptedCertificate(String username, PrivateKey privateKey, String providerID, String urlToSign) throws Exception {
        ZonedDateTime timeStamp = ZonedDateTime.now(CLOCK_IMPLEMENTATION);
        final String certificate = generateCertificate(username, timeStamp);
        final String signature = generateSignature(privateKey, TrustedApplicationUtils.generateSignatureBaseString(timeStamp, urlToSign, username));

        return new EncryptedCertificateImpl(providerID, new String(Base64.encode(certificate.getBytes("UTF-8")), "UTF-8"), signature);
    }

    /**
     * Generate a certificate representing the credentials being passed to the remote application.
     * @param username the username of the current user
     * @param timeStamp the creation timestamp of the certificate
     * @return a string representation of the certificate.
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String generateCertificate(String username, ZonedDateTime timeStamp) throws IllegalBlockSizeException, BadPaddingException {
        final StringWriter writer = new StringWriter();
        writer.write(Long.toString(timeStamp.toInstant().toEpochMilli()));
        writer.write('\n');
        writer.write(username);
        writer.flush();
        return writer.toString();
    }
}
