/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.Objects;

/**
 * A simple data wrapper that uses the provided string
 * as the string representation of this wrapper.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public final class StringReprWrapper<T> {

    private final T data;
    private final String repr;

    /**
     * Creates and returns a new {@code StringReprWrapper}.
     * 
     * @param data the data to wrap
     * @param repr the string representation to be used
     * 
     * @return a new wrapper
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public static <T> StringReprWrapper<T> of(T data, String repr) {
        return new StringReprWrapper<T>(data, repr);
    }

    /**
     * Constructs a new {@code StringReprWrapper}.
     * 
     * @param data the data to wrap
     * @param repr the string representation to be used
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public StringReprWrapper(T data, String repr) {
        this.data = Objects.requireNonNull(data);
        this.repr = Objects.requireNonNull(repr);
    }

    /**
     * Returns the object that is stored by this wrapper.
     * 
     * @return the wrapped object
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the string representation of this wrapper.
     * 
     * @return the string representation of this wrapper
     */
    @Override
    public String toString() {
        return repr;
    }

    /**
     * Returns the hash code of this wrapper.
     * 
     * @return a hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(data, repr);
    }

    /**
     * Checks whether this wrapper is equal to the specified object.
     * Two wrappers are considered equal if they use the same string representation
     * and the two wrapped objects are equal.
     * 
     * @param obj the object to test for equality
     * @true if this wrapper is "equal to" the argument
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof StringReprWrapper<?> wrapper) {
            return Objects.equals(this.repr, wrapper.repr) && Objects.equals(this.data, wrapper.data);
        } else
            return false;
    }

}
