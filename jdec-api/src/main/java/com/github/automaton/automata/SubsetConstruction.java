/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.collections4.*;

import com.google.gson.JsonObject;

/**
 * The subset construction.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class SubsetConstruction extends Automaton {

    /** The U-Structure that this subset construction is built from. */
    private transient final UStructure source;
    /** The controller that the subset construction is built with. */
    private final int controller;

    SubsetConstruction(JsonObject obj) {
        super(obj);
        source = null;
        controller = -1;
    }

    /**
     * Builds a new subset construction.
     * 
     * @param source     the U-Structure to build this subset construction with
     * @param controller the controller to build this subset construction with
     * 
     * @throws IndexOutOfBoundsException if {@code controller} is out of bounds
     * @throws NullPointerException      if {@code source} is {@code null}
     */
    SubsetConstruction(UStructure source, int controller) {
        super(Objects.requireNonNull(source).nControllers);
        this.source = source;
        if (controller < 0 || controller > source.nControllers) {
            throw new IndexOutOfBoundsException(controller);
        }
        this.controller = controller;
        super.states = MapUtils.predicatedMap(new LinkedHashMap<Long, State>(), Objects::nonNull,
                StateSet.class::isInstance);
        buildSubsetConstruction();
    }

    /**
     * Returns the U-Structure that this subset construction is built from.
     * 
     * @return the U-structure
     */
    public final UStructure getSource() {
        return this.source;
    }

    /**
     * Returns the controller that the subset construction is built with
     * 
     * @return the controller
     */
    public final int getController() {
        return this.controller;
    }

    /**
     * Runs subset construction w.r.t. the specified controller
     * 
     * @param controller the controller to perform subset construction with
     */
    private void buildSubsetConstruction() {

        this.addAllEvents(source.events);

        Queue<StateSet> stateQueue = new ArrayDeque<>();
        Set<StateSet> addedStates = new HashSet<>();

        {
            StateSet initialState = nullClosure(source.getState(source.initialState));
            this.addStateAt(initialState, true);
            stateQueue.add(initialState);
            addedStates.add(initialState);
        }

        while (!stateQueue.isEmpty()) {
            StateSet u = stateQueue.remove();
            MultiValuedMap<Event, Long> observableTransitions = u.groupAndGetObservableTransitions(controller);
            for (Event e : observableTransitions.keys()) {
                List<State> targetStates = new ArrayList<>();
                for (long targetStateID : observableTransitions.get(e)) {
                    targetStates.add(source.getState(targetStateID));
                }
                StateSet ss = nullClosure(targetStates);
                if (!addedStates.contains(ss)) {
                    this.addStateAt(ss, false);
                    addedStates.add(ss);
                }
                if (!containsTransition(u, e, ss.getID())) {
                    this.addTransition(u, e.getLabel(), ss);
                    stateQueue.add(ss);
                }
            }
        }

        /* Re-number states (by removing empty ones) */

        this.renumberStates();

    }

    /**
     * Performs null closure w.r.t. the specified controller.
     * 
     * @param state      state to perform null closure with
     * @param controller the controller to perform subset construction with
     */
    private StateSet nullClosure(State state) {
        Set<State> indistinguishableStates = new HashSet<>();
        nullClosure(indistinguishableStates, state);
        return new StateSet(indistinguishableStates, source.nStates);
    }

    /**
     * Performs null closure w.r.t. the specified controller.
     * 
     * @param states     a list of states that share the same triggering event
     * @param controller the controller to perform subset construction with
     */
    private StateSet nullClosure(List<State> states) {
        Set<State> indistinguishableStates = new HashSet<>();
        for (State s : states) {
            Set<State> tempSet = new HashSet<>();
            nullClosure(tempSet, s);
            indistinguishableStates.addAll(tempSet);
        }
        return new StateSet(indistinguishableStates, source.nStates);
    }

    /**
     * Recursively generate set of indistinguishable state.
     * 
     * @param stateSet   set of states containing indistinguishable states
     * @param curr       state to process
     * @param controller the controller to perform subset construction with
     */
    private void nullClosure(Set<State> stateSet, State curr) {
        stateSet.add(curr);
        Iterator<Transition> nullTransitions = IteratorUtils.filteredIterator(
                curr.getTransitions().iterator(),
                t -> {
                    if (t.getEvent().getVector().getLabelAtIndex(controller).equals(Event.EPSILON)) {
                        return true;
                    } else if (controller == 0) {
                        return false;
                    }
                    return !t.getEvent().isObservable(controller - 1);
                });
        while (nullTransitions.hasNext()) {
            Transition t = nullTransitions.next();
            State targetState = source.getState(t.getTargetStateID());
            if (!stateSet.contains(targetState)) {
                nullClosure(stateSet, targetState);
            }
        }
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public long addState(String label, boolean marked, boolean isInitialState) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public long addState(String label, boolean marked, List<Transition> transitions, boolean isInitialState) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean addStateAt(String label, boolean marked, List<Transition> transitions, boolean isInitialState,
            long id) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean addStateAt(String label, boolean marked, List<Transition> transitions, boolean isInitialState,
            long id, boolean enablement, boolean disablement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the state with the specified ID as a state set.
     * 
     * @param id a state ID
     * @return the state with the specified ID
     * 
     * @throws ClassCastException if the requested state is not a state set
     */
    public StateSet getStateAsStateSet(long id) {
        return (StateSet) super.getState(id);
    }

    /**
     * Gets the state with the specified label as a state set.
     * 
     * @param label a state label
     * @return the state with the specified label
     * 
     * @throws ClassCastException if the requested state is not a state set
     */
    public StateSet getStateAsStateSet(String label) {
        return (StateSet) super.getState(label);
    }

    /**
     * Builds an automaton representation of this subset construction.
     * 
     * @param controller the controller to build automaton representation with
     * @return an automaton representation of this subset construction
     * 
     * @throws IndexOutOfBoundsException if {@code controller} is out of bounds
     */
    public Automaton buildAutomatonRepresentationOf(int controller) {
        if (controller < 0 || controller > nControllers)
            throw new IndexOutOfBoundsException(controller);
        Automaton aut = new Automaton(1);
        for (State s : getStates()) {
            if (s instanceof StateSet ss) {
                aut.addStateAt(Long.toString(ss.getID()), ss.getSet().stream().anyMatch(State::isIllegalConfiguration), null, ss.getID() == getInitialStateID(), ss.getID());
            } else {
                aut.addStateAt(Long.toString(s.getID()), false, null, s.getID() == getInitialStateID(), s.getID());
            }
        }
        for (State s : getStates()) {
            for (Transition t : s.getTransitions()) {
                aut.addEventIfNonExisting(t.getEvent().getVector().getLabelAtIndex(controller), new boolean[] { false },
                        new boolean[] { false });
                aut.addTransition(s.getID(), t.getEvent().getVector().getLabelAtIndex(controller),
                        t.getTargetStateID());
            }
        }
        return aut;
    }

}
