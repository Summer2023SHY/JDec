/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.Comparator;
import java.util.Objects;

/**
 * A simple, stupid comparator that uses the objects'
 * {@link Object#hashCode() hash codes} for ordering.
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
public final class HashcodeBasedComparator<T> implements Comparator<T> {

    private static final HashcodeBasedComparator<?> INSTANCE = new HashcodeBasedComparator<>();

    private HashcodeBasedComparator() {
    }

    /**
     * Returns the sole instance of {@code HashcodeBasedComparator}.
     * 
     * @param <T> the data type
     * @return the sole instance
     */
    @SuppressWarnings("unchecked")
    public static <T> HashcodeBasedComparator<T> instance() {
        return (HashcodeBasedComparator<T>) INSTANCE;
    }

    /**
     * Compares and returns the two objects' orders.
     * 
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second
     */
    @Override
    public int compare(T o1, T o2) {
        return Integer.compare(Objects.hashCode(o1), Objects.hashCode(o2));
    }

}
