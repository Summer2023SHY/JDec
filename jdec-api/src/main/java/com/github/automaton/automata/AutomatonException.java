/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

/**
 * {@code AutomatonException} is the superclass of those exceptions
 * that can be thrown during automaton operations but do not have
 * appropriate superclasses defined elsewhere.
 *
 * @author Sung Ho Yoon
 * @since 1.2
 **/
public class AutomatonException extends RuntimeException {
    /**
     * Constructs a {@code AutomatonException} with no
     * detail message.
     */
    public AutomatonException() {
        super();
    }

    /**
     * Constructs a {@code AutomatonException} with the
     * specified detail message.
     * @param message the detail message
     */
    public AutomatonException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code AutomatonException} with the
     * specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public AutomatonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code AutomatonException} with the
     * specified cause.
     * @param cause the cause
     */
    public AutomatonException(Throwable cause) {
        super(cause);
    }
}
