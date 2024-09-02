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
public abstract class ComponentIterable implements Iterable<Automaton> {

    private Set<Automaton> plants;
    private Set<Automaton> specs;

    private Comparator<Automaton> ordering;

    private Set<Automaton> gPrime;
    private Set<Automaton> hPrime;

    /**
     * A list of components ordered as specified by the heuristic.
     */
    private List<Automaton> heuristicAppliedComponents;

    /**
     * Constructs a new {@code HeuristicBasedComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    protected ComponentIterable(Set<Automaton> plants, Set<Automaton> specs) {
        this(plants, specs, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Constructs a new {@code HeuristicBasedComponentIterable}.
     * 
     * @param plants the set of plants
     * @param specs the set of specifications
     * @param gPrime the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @throws NullPointerException if any one of the arguments is {@code null}
     */
    protected ComponentIterable(Set<Automaton> plants, Set<Automaton> specs, Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this.plants = Objects.requireNonNull(plants);
        this.specs = Objects.requireNonNull(specs);
        this.gPrime = Objects.requireNonNull(gPrime);
        this.hPrime = Objects.requireNonNull(hPrime);
    }

    /**
     * Sets the {@link Comparator} to be used for ordering the components.
     * 
     * @param ordering the comparator that defines heuristic-based ordering
     * 
     * @throws NullPointerException if argument is {@code null}
     * @throws UnsupportedOperationException if this component iterable does not support
     *                                       {@link Comparator}-based ordering
     */
    protected void setOrdering(Comparator<Automaton> ordering) {
        this.ordering = Objects.requireNonNull(ordering);
        heuristicAppliedComponents = null;
    }

    /**
     * Builds the ordering of components with the heuristics.
     * The ordering must be set using {@link #setOrdering(Comparator)}
     * before calling this method.
     * 
     * @throws IllegalStateException if ordering is not yet set
     */
    protected void buildHeuristic() {
        if (this.ordering == null) {
            throw new IllegalStateException();
        }
        heuristicAppliedComponents = new ArrayList<>();
        heuristicAppliedComponents.addAll(plants);
        heuristicAppliedComponents.addAll(specs);
        Collections.sort(heuristicAppliedComponents, ordering);
    }

    /**
     * Returns the iterator of components.
     * The order of elements returned by the iterator
     * is defined by the ordering.
     * 
     * @return an iterator
     * 
     * @throws IllegalStateException if ordering is not yet set
     */
    @Override
    public Iterator<Automaton> iterator() {
        if (heuristicAppliedComponents == null) {
            buildHeuristic();
        }
        return IteratorUtils.filteredIterator(heuristicAppliedComponents.iterator(), aut -> !gPrime.contains(aut) && !hPrime.contains(aut));
    }

    /**
     * Sets the filters to be used by this iterable.
     * 
     * @param the set of "checked" plants
     * @param hPrime the set of "checked" specifications
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public final void setFilters(Set<Automaton> gPrime, Set<Automaton> hPrime) {
        this.gPrime = Objects.requireNonNull(gPrime);
        this.hPrime = Objects.requireNonNull(hPrime);
    }

    /**
     * Returns the list of components in the order as specified by the heuristic.
     * @return the list of components
     */
    protected final List<Automaton> getOrderedList(boolean forceInit) {
        if (forceInit)
            heuristicAppliedComponents = new ArrayList<>();
        return heuristicAppliedComponents;
    }

    /**
     * Returns the set of plants used in this iterable.
     * 
     * @return the set of plants
     */
    public final Set<Automaton> getPlants() {
        return plants;
    }

    /**
     * Returns the set of specifications used in this iterable.
     * 
     * @return the set of specifications
     */
    public final Set<Automaton> getSpecs() {
        return specs;
    }

}
