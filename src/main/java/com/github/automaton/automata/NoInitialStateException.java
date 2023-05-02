package com.github.automaton.automata;

/**
 * Thrown to indicate that an operation failed because
 * the automaton has no initial state.
 *
 * @author Sung Ho Yoon
 * @since 1.2
 **/
public class NoInitialStateException extends AutomatonException {
    /**
     * Constructs a {@code NoInitialStateException} with no
     * detail message.
     */
    public NoInitialStateException() {
        super();
    }

    /**
     * Constructs a {@code NoInitialStateException} with the
     * specified detail message.
     * @param message the detail message
     */
    public NoInitialStateException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code NoInitialStateException} with the
     * specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public NoInitialStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code NoInitialStateException} with the
     * specified cause.
     * @param cause the cause
     */
    public NoInitialStateException(Throwable cause) {
        super(cause);
    }
}