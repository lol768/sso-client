package uk.ac.warwick.sso.client.trusted;

public class InvalidCertificateException extends TransportException {
    public InvalidCertificateException(TransportErrorMessage error, Exception cause)
    {
        super(error, cause);
    }

    public InvalidCertificateException(TransportErrorMessage error)
    {
        super(error);
    }

    public InvalidCertificateException(TransportException exception)
    {
        super(exception.getTransportErrorMessage(), exception);
    }

    public String getMessage()
    {
        Throwable cause = getCause();
        if (cause != null)
        {
            return cause.getMessage();
        }
        return super.getMessage();
    }
}
