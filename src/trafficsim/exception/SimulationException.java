package trafficsim.exception;

public class SimulationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SimulationException(String message) {
        super(message);
    }

    public SimulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
