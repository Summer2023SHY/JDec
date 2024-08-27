/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Automaton;

public class RandomOrderComponentIterable  extends HeuristicBasedComponentIterable {

    public RandomOrderComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        super(plants, specs);
        setOrdering(HashcodeBasedComparator.instance());
    }

    public RandomOrderComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this(plants, specs);
        setFilters(gPrime, hPrime);
    }

}
