package trafficsim.exception;

public class InvalidNetworkException extends SimulationException {
    public InvalidNetworkException(String message) {
        super(message);
    }

    public InvalidNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
