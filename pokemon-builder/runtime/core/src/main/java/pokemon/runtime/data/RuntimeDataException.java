package pokemon.runtime.data;

/** Thrown when the Builder runtime data is missing, unreadable or invalid. */
public class RuntimeDataException extends RuntimeException {
    public RuntimeDataException(String message) {
        super(message);
    }

    public RuntimeDataException(String message, Throwable cause) {
        super(message, cause);
    }
}