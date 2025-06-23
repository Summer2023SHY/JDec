/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.Set;

import com.github.automaton.automata.Automaton;

/**
 * Predefined heuristics for selecting system components.
 * 
 * @see ComponentIterable
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
public enum ComponentHeuristics implements FilteredComponentIterableGenerator {

    /**
     * Heuristic that alters between plants and specifications.
     * 
     * @see AlternatingComponentIterable
     */
    ALTERNATING("Alternating") {
        @Override
        public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime,
                Set<Automaton> hPrime) {
            return new AlternatingComponentIterable(plants, specs, gPrime, hPrime);
        }
    },

    /**
     * Heuristic that favors plants over specifications.
     * 
     * @see PlantOverSpecComponentIterable
     */
    PLANT_OVER_SPEC("Plant over Spec") {
        @Override
        public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime,
                Set<Automaton> hPrime) {
            return new PlantOverSpecComponentIterable(plants, specs, gPrime, hPrime);
        }
    },

    /**
     * Heuristic that favors specifications over plants.
     * 
     * @see SpecOverPlantComponentIterable
     */
    SPEC_OVER_PLANT("Spec over Plant") {
        @Override
        public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime,
                Set<Automaton> hPrime) {
            return new SpecOverPlantComponentIterable(plants, specs, gPrime, hPrime);
        }
    },

    /**
     * Heuristic that uses random order.
     * 
     * @see RandomOrderComponentIterable
     */
    RANDOM("Random") {
        @Override
        public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime,
                Set<Automaton> hPrime) {
            return new RandomOrderComponentIterable(plants, specs, gPrime, hPrime);
        }
    };

    private final String repr;

    private ComponentHeuristics(String repr) {
        this.repr = repr;
    }

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
