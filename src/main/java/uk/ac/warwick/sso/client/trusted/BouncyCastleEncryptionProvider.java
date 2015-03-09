package uk.ac.warwick.sso.client.trusted;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.joda.time.DateTime;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class BouncyCastleEncryptionProvider implements EncryptionProvider {

    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String ASYM_CIPHER = "RSA/NONE/NoPadding";
    private static final String ASYM_ALGORITHM = "RSA";

    public static final Provider PROVIDER = new BouncyCastleProvider();

    @Override
    public String generateSignature(PrivateKey privateKey, byte[] signatureBaseString) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        sig.initSign(privateKey);
        sig.update(signatureBaseString);
        return Base64.toBase64String(Base64.encode(sig.sign()));
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, byte[] signatureBaseString, String signatureToVerify) throws SignatureVerificationFailedException {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
            sig.initVerify(publicKey);
            sig.update(signatureBaseString);
            return sig.verify(Base64.decode(signatureToVerify));
        } catch (Exception e) {
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
        final PKCS8EncodedKeySpec pubKeySpec = new PKCS8EncodedKeySpec(encodedForm);
        final KeyFactory keyFactory = KeyFactory.getInstance(ASYM_ALGORITHM, PROVIDER);
        return keyFactory.generatePrivate(pubKeySpec);
    }

    @Override
    public ApplicationCertificate decodeEncryptedCertificate(EncryptedCertificate encCert, PublicKey publicKey, String providerID) throws InvalidCertificateException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(Base64.decode(encCert.getCertificate())), "UTF-8"));

            final String created = in.readLine();
            final String username = in.readLine();

            final DateTime timeCreated = new DateTime(Long.parseLong(created));

            return new ApplicationCertificateImpl(providerID, username, timeCreated);
        } catch (IOException e) {
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
}
