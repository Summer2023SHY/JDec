/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import org.apache.commons.collections4.IteratorUtils;

import com.github.automaton.automata.Automaton;

/**
 * An iterable of system components, that iterate in the order as defined by
 * the underlying heuristic.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public abstract class HeuristicBasedComponentIterable implements Iterable<Automaton> {

    private Set<Automaton> plants;
    private Set<Automaton> specs;

    private Comparator<Automaton> ordering;

    private Set<Automaton> gPrime;
    private Set<Automaton> hPrime;

    private List<Automaton> heuristicAppliedComponents;

    protected HeuristicBasedComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        this(plants, specs, Collections.emptySet(), Collections.emptySet());
    }

    protected HeuristicBasedComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this.plants = Objects.requireNonNull(plants);
        this.specs = Objects.requireNonNull(specs);
        this.gPrime = Objects.requireNonNull(gPrime);
        this.hPrime = Objects.requireNonNull(hPrime);
    }

    protected final void setOrdering(Comparator<Automaton> ordering) {
        this.ordering = Objects.requireNonNull(ordering);
        heuristicAppliedComponents = null;
    }

    protected final void buildHeuristic() {
        if (this.ordering == null) {
            throw new IllegalStateException();
        }
        heuristicAppliedComponents = new ArrayList<>();
        heuristicAppliedComponents.addAll(plants);
        heuristicAppliedComponents.addAll(specs);
        Collections.sort(heuristicAppliedComponents, ordering);
    }

    @Override
    public Iterator<Automaton> iterator() {
        if (heuristicAppliedComponents == null) {
            buildHeuristic();
        }
        return IteratorUtils.filteredIterator(heuristicAppliedComponents.iterator(), aut -> !gPrime.contains(aut) && !hPrime.contains(aut));
    }

    public final void setFilters(Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this.gPrime = Objects.requireNonNull(gPrime);
        this.hPrime = Objects.requireNonNull(hPrime);
    }

    public final Set<Automaton> getPlants() {
        return plants;
    }

    public final Set<Automaton> getSpecs() {
        return specs;
    }

}
