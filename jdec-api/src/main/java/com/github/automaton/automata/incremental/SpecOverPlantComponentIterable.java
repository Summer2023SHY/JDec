/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Automaton;

/**
 * An iterable of system components that favor specifications over plants.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class SpecOverPlantComponentIterable extends ComponentIterable {

    Comparator<Automaton> ordering = (aut1, aut2) -> {
        if (getSpecs().contains(aut1)) {
            if (getSpecs().contains(aut2))
                return 0;
            else
                return -1;
        } else if (getSpecs().contains(aut2)) {
            return 1;
        } else {
            return 0;
        }
    };

    /**
     * Constructs a new {@code SpecOverPlantComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public SpecOverPlantComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        super(plants, specs);
        setOrdering(ordering);
    }

    /**
     * Constructs a new {@code SpecOverPlantComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @param gPrime the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @throws NullPointerException if any one of the arguments is {@code null}
     */
    public SpecOverPlantComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this(plants, specs);
        setFilters(gPrime, hPrime);
    }

}
