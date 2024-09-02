/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Automaton;

/**
 * An iterable of system components that uses random ordering.
 * Specifically, the {@link Object#hashCode() hash codes} of
 * the components are used for ordering.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class RandomOrderComponentIterable  extends ComponentIterable {

    /**
     * Constructs a new {@code RandomOrderComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public RandomOrderComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        super(plants, specs);
        setOrdering(HashcodeBasedComparator.instance());
    }

    /**
     * Constructs a new {@code RandomOrderComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @param gPrime the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @throws NullPointerException if any one of the arguments is {@code null}
     */
    public RandomOrderComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this(plants, specs);
        setFilters(gPrime, hPrime);
    }

}
