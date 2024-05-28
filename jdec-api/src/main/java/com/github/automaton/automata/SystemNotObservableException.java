/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

/**
 * Thrown to indicate that a system is not observable.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class SystemNotObservableException extends AutomatonException {
    /**
     * Constructs a {@code SystemNotObservableException} with no
     * detail message.
     */
    public SystemNotObservableException() {
        super();
    }

    /**
     * Constructs a {@code SystemNotObservableException} with the
     * specified detail message.
     * @param message the detail message
     */
    public SystemNotObservableException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code SystemNotObservableException} with the
     * specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public SystemNotObservableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code SystemNotObservableException} with the
     * specified cause.
     * @param cause the cause
     */
    public SystemNotObservableException(Throwable cause) {
        super(cause);
    }
}
