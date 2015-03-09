package uk.ac.warwick.sso.client.trusted;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

/**
 * Abstracts out the provision of encryption to the trusted app service. For two applications to communicate
 * effectively, they <i>must</i> use the same encryption provider. In our experience, even using the same algorithms
 * but different providers will cause issues.
 * <p>
 * This abstraction is mostly used in unit testing, to avoid having to bring up a fully-fledged crypto provider
 */
public interface EncryptionProvider {
    /**
     * Char used to distinguish between the components used to make up a signature base string.
     */
    public static final String SIGNATURE_BASE_SEPARATOR = "\n";

    /**
     * Decode an encrypted certificate to retrieve its ApplicationCertificate
     *
     * @param encCert
     *            the encrypted certificate of the application
     * @param publicKey
     *            the application's public key
     * @param appId
     *            the application's ID
     * @return the decrypted ApplicationCertificate
     * @throws InvalidCertificateException
     *             if the certificate was malformed, or could not be decrypted
     */
    ApplicationCertificate decodeEncryptedCertificate(EncryptedCertificate encCert, PublicKey publicKey, String appId) throws InvalidCertificateException;

//    /**
//     * Create a new encrypted certificate for transmission to another application
//     *
//     * @param userName
//     *            the username to certify
//     * @param privateKey
//     *            the private key of this application
//     * @param appId
//     *            the ID of this application
//     * @return
//     *
//     *
//     * @deprecated use {@link EncryptionProvider#createEncryptedCertificate(String, java.security.PrivateKey, String, String)}
//     */
//    @Deprecated
//    EncryptedCertificate createEncryptedCertificate(String userName, PrivateKey privateKey, String appId);
//
//    /**
//     * Create a new encrypted certificate for transmission to another application
//     *
//     * defaults to a v3 protocol.
//     *
//     * @param userName the username to certify
//     * @param privateKey the private key of this application
//     * @param appId the ID of this application
//     * @param urlToSign the target URL of this request, or <code>null</code> for a v1 request
//     * @since 2.4
//     */
//    EncryptedCertificate createEncryptedCertificate(String userName, PrivateKey privateKey, String appId, String urlToSign);

    /**
     * Convert an encoded public key into a PublicKey instance
     *
     * @param encodedForm
     *            the byte-array representation of the key
     * @return the object representation of the key
     * @throws NoSuchAlgorithmException
     *             if the algorithm to generate the keypair is not available
     * @throws NoSuchProviderException
     *             if no appropriate cryptographic provider is available
     * @throws InvalidKeySpecException
     *             if the encoded form does not contain a valid key
     */
    PublicKey toPublicKey(byte[] encodedForm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;

    /**
     * Convert an encoded private key into a PrivateKey instance
     *
     * @param encodedForm
     *            the byte-array representation of the key
     * @return the object representation of the key
     * @throws NoSuchAlgorithmException
     *             if the algorithm to generate the keypair is not available
     * @throws NoSuchProviderException
     *             if no appropriate cryptographic provider is available
     * @throws InvalidKeySpecException
     *             if the encoded form does not contain a valid key
     */
    PrivateKey toPrivateKey(byte[] encodedForm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;

    /**
     * Generate a new signature from the provided base string and private key.
     * @param privateKey the key used to encrypt the signature.
     * @param signatureBaseString the base string to build the signature from.
     * @return a string representation of the signature.
     */
    String generateSignature(PrivateKey privateKey, byte[] signatureBaseString) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException;

    /**
     * Verify a received signature, by comparison with a locally generated signature.
     * @param signatureBaseString the string to use as the basis of the local signature.
     * @param signature the signature from the incoming request to verify.
     * @return true if the signature is verified, false otherwise.
     * @throws SignatureVerificationFailedException
     */
    boolean verifySignature(PublicKey publicKey, byte[] signatureBaseString, String signature) throws SignatureVerificationFailedException;
}

