/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

/**
 * Thrown when two automata are being combined in some way and they are not
 * compatible with one another. For example, this can happen if they have a
 * different number of controllers or if they both share an event with the
 * same name but with different properties.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 **/
public class IncompatibleAutomataException extends IllegalArgumentException {
    /**
     * Constructs a {@code IncompatibleAutomataException} with no
     * detail message.
     */
    public IncompatibleAutomataException() {
        super();
    }

    /**
     * Constructs a {@code IncompatibleAutomataException} with the
     * specified detail message.
     * 
     * @param message the detail message
     * @since 1.1
     */
    public IncompatibleAutomataException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code IncompatibleAutomataException} with the
     * specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     * @since 1.1
     */
    public IncompatibleAutomataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code IncompatibleAutomataException} with the
     * specified cause.
     * 
     * @param cause the cause
     * @since 1.1
     */
    public IncompatibleAutomataException(Throwable cause) {
        super(cause);
    }
}
