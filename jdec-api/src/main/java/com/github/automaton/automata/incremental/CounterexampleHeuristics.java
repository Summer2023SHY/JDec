/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Word;

/**
 * Heuristics that can be used for selecting counterexamples.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public enum CounterexampleHeuristics implements Comparator<Counterexample> {
    /**
     * Do not use any heuristics.
     */
    NONE("None") {
        @Override
        public int compare(Counterexample o1, Counterexample o2) {
            return 0;
        }
    },
    /**
     * Favor short counterexamples.
     */
    SHORT_C("Short C") {
        @Override
        public int compare(Counterexample o1, Counterexample o2) {
            Objects.requireNonNull(o1);
            Objects.requireNonNull(o2);
            Word o1First = o1.getWords().isEmpty() ? Word.EPSILON : o1.getWords().iterator().next();
            Word o2First = o2.getWords().isEmpty() ? Word.EPSILON : o2.getWords().iterator().next();

            return Integer.compare(o1First.length(), o2First.length());
        }
    },
    /**
     * Favor long counterexamples.
     */
    LONG_C("Long C") {
        @Override
        public int compare(Counterexample o1, Counterexample o2) {
            return -SHORT_C.compare(o1, o2);
        }
    },
    /**
     * Use lexicographic ordering
     */
    LEXICOGRAPHIC("Lexicographic") {
        @Override
        public int compare(Counterexample o1, Counterexample o2) {
            Objects.requireNonNull(o1);
            Objects.requireNonNull(o2);
            Word o1First = o1.getWords().isEmpty() ? Word.EPSILON : o1.getWords().iterator().next();
            Word o2First = o2.getWords().isEmpty() ? Word.EPSILON : o2.getWords().iterator().next();

            return o1First.compareTo(o2First);
        }
    };

    private final String repr;

    private CounterexampleHeuristics(String repr) {
        this.repr = repr;
    }

    /**
     * Compares two counterexamples as specified by this heuristic's ordering.
     * The ordering provided by this method is not consistent with equals.
     * 
     * @param o1 the first counterexample
     * @param o2 the second counterexample
     * 
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second
     * 
     * @throws NullPointerException if an argument is {@code null} and the heuristic
     *                              does not permit {@code null} arguments
     */
    @Override
    public abstract int compare(Counterexample o1, Counterexample o2);

    /**
     * Returns the string representation of this heuristic.
     * 
     * @return the string representation of this heuristic
     */
    @Override
    public String toString() {
        return repr;
    }

}
