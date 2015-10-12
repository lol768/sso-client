package uk.ac.warwick.sso.client.trusted;

public abstract class TransportException extends Exception {
    private final TransportErrorMessage error;

    TransportException(TransportErrorMessage error) {
        super(error.getFormattedMessage());
        this.error = error;
    }

    TransportException(TransportErrorMessage error, Exception exception) {
        super(error.getFormattedMessage(), exception);
        this.error = error;
    }

    public final TransportErrorMessage getTransportErrorMessage()
    {
        return error;
    }
}
