package com.github.automaton.io.json;

import com.google.gson.JsonParseException;

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
