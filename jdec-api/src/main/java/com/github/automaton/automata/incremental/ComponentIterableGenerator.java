/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.Set;

import com.github.automaton.automata.Automaton;

/**
 * A generator interface for {@link ComponentIterable}s.
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
@FunctionalInterface
public interface ComponentIterableGenerator {

    /**
     * Generates a new component iterable with the specified system components.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @return a new component iterable
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs);

}
