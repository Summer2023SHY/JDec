/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

/**
 * Thrown when an operation fails for some reason.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 **/
public class OperationFailedException extends AutomatonException {
    /**
     * Constructs a {@code OperationFailedException} with no
     * detail message.
     */
    public OperationFailedException() {
        super();
    }

    /**
     * Constructs a {@code OperationFailedException} with the
     * specified detail message.
     * 
     * @param message the detail message
     * @since 1.2
     */
    public OperationFailedException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code OperationFailedException} with the
     * specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     * @since 1.2
     */
    public OperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code OperationFailedException} with the
     * specified cause.
     * 
     * @param cause the cause
     * @since 1.2
     */
    public OperationFailedException(Throwable cause) {
        super(cause);
    }
}
