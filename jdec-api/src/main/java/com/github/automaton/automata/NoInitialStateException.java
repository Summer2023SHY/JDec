package com.github.automaton.automata;

/* 
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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