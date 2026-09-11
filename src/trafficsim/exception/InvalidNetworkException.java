package trafficsim.exception;

public class InvalidNetworkException extends SimulationException {
    private static final long serialVersionUID = 1L;

    public InvalidNetworkException(String message) {
        super(message);
    }

    public InvalidNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
