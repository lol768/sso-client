package uk.ac.warwick.sso.client.trusted;

public class SignatureVerificationFailedException extends Exception {
    public SignatureVerificationFailedException(Exception cause)
    {
        super(cause);
    }
}