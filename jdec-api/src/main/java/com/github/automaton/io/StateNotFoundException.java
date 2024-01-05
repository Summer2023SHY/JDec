/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io;

import com.github.automaton.automata.AutomatonException;

/**
 * Thrown to indicate that the requested state does not exist.
 *
 * @author Sung Ho Yoon
 * @since 1.3
 **/
public class StateNotFoundException extends AutomatonException {
    /**
     * Constructs a {@code StateNotFoundException} with no
     * detail message.
     */
    public StateNotFoundException() {
        super();
    }

    /**
     * Constructs a {@code StateNotFoundException} with the
     * default message using the provided ID.
     * 
     * @param id ID of the requested state
     */
    public StateNotFoundException(long id) {
        this("The state with the ID " + id + " does not exist.");
    }

    /**
     * Constructs a {@code StateNotFoundException} with the
     * specified detail message.
     * 
     * @param message the detail message
     */
    public StateNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code StateNotFoundException} with the
     * specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     */
    public StateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code StateNotFoundException} with the
     * specified cause.
     * 
     * @param cause the cause
     */
    public StateNotFoundException(Throwable cause) {
        super(cause);
    }
}
