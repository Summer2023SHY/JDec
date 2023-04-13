package automata;
/**
 * Thrown when the {@code .bdy} file is not able to be interpreted as indicated
 * by its header file. So when anything unexpected happens while reading the
 * {@code .bdy} file, this exception should be thrown.
 *
 * @author Micah Stairs
 **/
public class MissingOrCorruptBodyFileException extends java.io.IOException {
    /**
     * Constructs a {@code MissingOrCorruptBodyFileException} with no
     * detail message.
     */
    public MissingOrCorruptBodyFileException() {
        super();
    }

    /**
     * Constructs a {@code MissingOrCorruptBodyFileException} with the
     * specified detail message.
     * @param message the detail message
     */
    public MissingOrCorruptBodyFileException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code MissingOrCorruptBodyFileException} with the
     * specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public MissingOrCorruptBodyFileException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code MissingOrCorruptBodyFileException} with the
     * specified cause.
     * @param cause the cause
     */
    public MissingOrCorruptBodyFileException(Throwable cause) {
        super(cause);
    }
}