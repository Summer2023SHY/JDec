/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.Collections;
import java.util.Set;

import com.github.automaton.automata.Automaton;

/**
 * A generator interface for {@link ComponentIterable}s,
 * with an option to specify extra filters.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
@FunctionalInterface
public interface FilteredComponentIterableGenerator extends ComponentIterableGenerator {

    @Override
    public default ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs) {
        return generate(plants, specs, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Generates a new component iterable with the specified system components.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @param gPrime the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @return a new component iterable
     * 
     * @throws NullPointerException if any one of the arguments is {@code null}
     */
    public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime);

}
