/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

/**
 * Heuristics that can be used for selecting counterexamples.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public enum CounterexampleHeuristics {
    /**
     * Do not use any heuristics.
     */
    NONE("None"),
    /**
     * Favor short counterexamples.
     */
    SHORT_C("Short C"),
    /**
     * Favor long counterexamples.
     */
    LONG_C("Long C");

    private CounterexampleHeuristics(String repr) {
        this.repr = repr;
    }

    private final String repr;
    
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
