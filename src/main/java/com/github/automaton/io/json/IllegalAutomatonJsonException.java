package com.github.automaton.io.json;

import com.google.gson.JsonParseException;

/**
 * Thrown to indicate that an error occurred when loading an automaton from
 * a JSON object.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class IllegalAutomatonJsonException extends JsonParseException {

    /**
     * Constructs a {@code MalformedAutomatonJsonException} with the
     * specified detail message.
     * 
     * @param message the detail message
     */
    public IllegalAutomatonJsonException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code MalformedAutomatonJsonException} with the
     * specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     */
    public IllegalAutomatonJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code MalformedAutomatonJsonException} with the
     * specified cause.
     * 
     * @param cause the cause
     */
    public IllegalAutomatonJsonException(Throwable cause) {
        super(cause);
    }

}
