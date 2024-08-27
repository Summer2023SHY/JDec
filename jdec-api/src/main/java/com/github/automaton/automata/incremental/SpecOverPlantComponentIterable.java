/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import com.github.automaton.automata.Automaton;

public class SpecOverPlantComponentIterable extends HeuristicBasedComponentIterable {

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

    public SpecOverPlantComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        super(plants, specs);
        setOrdering(ordering);
    }

    public SpecOverPlantComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this(plants, specs);
        setFilters(gPrime, hPrime);
    }

}
