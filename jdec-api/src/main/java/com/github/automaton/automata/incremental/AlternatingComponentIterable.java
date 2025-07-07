/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Automaton;

/**
 * An iterable of system components that alternate between plants and specifications.
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
public class AlternatingComponentIterable extends ComponentIterable {

    /**
     * Constructs a new {@code AlternatingComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public AlternatingComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        super(plants, specs);
    }

    /**
     * Constructs a new {@code AlternatingComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @param gPrime the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @throws NullPointerException if any one of the arguments is {@code null}
     */
    public AlternatingComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime,
            Set<Automaton> hPrime) {
        super(plants, specs, gPrime, hPrime);
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    protected void setOrdering(Comparator<Automaton> ordering) {
        throw new UnsupportedOperationException();
    }

    /**
     * Builds the ordering of components with the heuristics.
     */
    @Override
    protected void buildHeuristic() {
        List<Automaton> components = getOrderedList(true);
        Iterator<Automaton> plantIterator = getPlants().iterator();
        Iterator<Automaton> specIterator = getSpecs().iterator();
        while (plantIterator.hasNext() || specIterator.hasNext()) {
            if (plantIterator.hasNext()) {
                components.add(plantIterator.next());
            }
            if (specIterator.hasNext()) {
                components.add(specIterator.next());
            }
        }
    }
    
}
