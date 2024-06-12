/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;
import java.util.function.IntFunction;

import com.github.automaton.automata.util.*;

import org.apache.logging.log4j.*;

/**
 * A collection of automata operations.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class AutomataOperations {

    private static Logger logger = LogManager.getLogger();

    /** Private constructor. */
    private AutomataOperations() {
    }

    /**
     * Generates the accessible portion of the specified automaton.
     * 
     * @param <T>      the type of automaton
     * @param source   an automaton
     * @param supplier a function that creates a new automaton of the same type
     * @return the accessible portion of {@code source}
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     * 
     * @see Automaton#accessible()
     */
    public static <T extends Automaton> T accessible(final T source, final IntFunction<T> supplier) {

        /* Setup */

        Objects.requireNonNull(source);
        Objects.requireNonNull(supplier);

        T automaton = supplier.apply(source.nControllers);

        // Add events
        automaton.addAllEvents(source.events);

        // If there is no initial state, return null, so that the GUI knows to alert the
        // user
        if (source.getInitialStateID() == 0)
            return null;

        // Add the initial state to the stack
        Deque<Long> stack = new ArrayDeque<Long>();
        stack.push(source.getInitialStateID());

        /* Build automaton from the accessible part of this automaton */

        // Add states and transition
        while (stack.size() > 0) {

            // Get next ID
            long id = stack.pop();

            // This state has already been created in the new automaton, so it does not need
            // to be created again
            if (automaton.stateExists(id))
                continue;

            // Get state and transitions
            State state = source.getState(id);
            List<Transition> transitions = state.getTransitions();

            // Add new state
            automaton.addStateAt(
                    state.getLabel(),
                    state.isMarked(),
                    new ArrayList<Transition>(),
                    id == source.getInitialStateID(),
                    id,
                    state.isEnablementState(),
                    state.isDisablementState());

            // Traverse each transition
            for (Transition t : transitions) {

                // Add the target state to the stack
                stack.push(t.getTargetStateID());

                // Add transition to the new automaton
                automaton.addTransition(id, t.getEvent().getID(), t.getTargetStateID());

            }

        }

        /* Add special transitions if they still appear in the accessible part */

        source.copyOverSpecialTransitions(automaton);

        /* Re-number states (by removing empty ones) */

        automaton.renumberStates();

        /* Return accessible automaton */

        return automaton;
    }

    /**
     * Generates the co-accessible portion of the specified automaton.
     * 
     * @param <T>      The type of automaton
     * @param source   an automaton
     * @param supplier a function that creates a new automaton of the same type
     * @return the co-accessible portion of {@code source}
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     * 
     * @see Automaton#coaccessible()
     */
    public static <T extends Automaton> T coaccessible(final T source, final IntFunction<T> supplier) {

        Objects.requireNonNull(source);
        Objects.requireNonNull(supplier);

        T invertedAutomaton = invert(source, supplier);
        T automaton = supplier.apply(source.nControllers);

        /*
         * Build co-accessible automaton by seeing which states are accessible from the
         * marked states in the inverted automaton
         */

        // Add events
        automaton.addAllEvents(source.events);

        // Add all marked states to the stack (NOTE: This may have complications if
        // there are more than Integer.MAX_VALUE marked states)
        Deque<Long> stack = new ArrayDeque<Long>();
        for (long s = 1; s <= source.nStates; s++) {

            State state = invertedAutomaton.getState(s);

            if (state.isMarked())
                stack.push(s);

        }

        // Add all reachable states to the co-accessible automaton
        while (stack.size() > 0) {

            long s = stack.pop();

            // Skip this state is it has already been taken care of
            if (automaton.stateExists(s))
                continue;

            State state = source.getState(s);
            State stateWithInvertedTransitions = invertedAutomaton.getState(s);

            // Add this state (and its transitions) to the co-accessible automaton
            automaton.addStateAt(state.getLabel(), state.isMarked(), new ArrayList<Transition>(),
                    s == source.initialState, s);

            // Add all directly reachable states from this one to the stack
            for (Transition t : stateWithInvertedTransitions.getTransitions()) {

                // Add transition if both states already exist in the co-accessible automaton
                if (automaton.stateExists(t.getTargetStateID()))
                    automaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), s);

                // Otherwise add this to the stack since it is not yet in the co-accessible
                // automaton
                else
                    stack.push(t.getTargetStateID());

            }

            // Required to catch transitions if we didn't add them the first time around
            // (since this state was not yet in the co-accessible automaton)
            for (Transition t : state.getTransitions()) {

                // Add transition if both states already exist in the co-accessible automaton
                if (automaton.stateExists(t.getTargetStateID()))
                    // We don't want to add self-loops twice
                    if (s != t.getTargetStateID())
                        automaton.addTransition(s, t.getEvent().getID(), t.getTargetStateID());

            }

        }

        /* Add special transitions if they still appear in the accessible part */

        source.copyOverSpecialTransitions(automaton);

        /* Re-number states (by removing empty ones) */

        automaton.renumberStates();

        /* Return co-accessible automaton */

        return automaton;
    }

    /**
     * Generates the inverse of the specified automaton.
     * The state IDs of the inverted automaton are the same as the original
     * automaton. Special transition information are not maintained in the
     * inverse.
     * 
     * @param <T>      the type of automaton
     * @param source   an automaton
     * @param supplier a function that creates a new automaton of the same type
     * @return the inverse of {@code source}
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     * 
     * @see Automaton#invert()
     **/
    public static <T extends Automaton> T invert(final T source, final IntFunction<T> supplier) {

        Objects.requireNonNull(source);
        Objects.requireNonNull(supplier);

        T automaton = supplier.apply(source.nControllers);

        /*
         * Create a new automaton that has each of the transitions going the opposite
         * direction
         */

        // Add events
        automaton.addAllEvents(source.events);

        // Add states
        for (State state : source.getStates()) {
            automaton.addStateAt(state.getLabel(), state.isMarked(), new ArrayList<>(),
                    state.getID() == source.getInitialStateID(),
                    state.getID());
        }

        // Add transitions
        for (State state : source.getStates())
            for (Transition t : state.getTransitions())
                automaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), state.getID());

        return automaton;

    }

    /**
     * Generate the intersection of the two specified automata.
     * 
     * @param first  The first automaton
     * @param second The second automaton
     * @return The intersection
     * @throws IncompatibleAutomataException If the number of controllers do not
     *                                       match, or the automata have
     *                                       incompatible events
     * 
     * @since 2.0
     **/
    public static Automaton intersection(Automaton first, Automaton second) throws IncompatibleAutomataException {

        /* Error checking */

        if (first.getNumberOfControllers() != second.getNumberOfControllers())
            throw new IncompatibleAutomataException();

        /* Setup */

        Automaton automaton = new Automaton(first.getNumberOfControllers());

        // These two stacks should always have the same size
        Deque<Long> stack1 = new ArrayDeque<Long>();
        Deque<Long> stack2 = new ArrayDeque<Long>();

        // Add the initial states to the stack
        stack1.push(first.getInitialStateID());
        stack2.push(second.getInitialStateID());

        /* Build product */

        // Create event set (intersection of both event sets)
        for (Event e1 : first.getEvents())
            for (Event e2 : second.getEvents())
                if (e1.equals(e2)) {

                    // Ensure that these automata are compatible (meaning that no events have the
                    // same name, but with different properties)
                    if (!Arrays.equals(e1.isObservable(), e2.isObservable())
                            || !Arrays.equals(e1.isControllable(), e2.isControllable())) {
                        throw new IncompatibleAutomataException();
                    }
                    automaton.addEvent(e1.getLabel(), e1.isObservable(), e1.isControllable());

                }

        // Add states and transition
        while (stack1.size() > 0) {

            // Get next IDs
            long id1 = stack1.pop();
            long id2 = stack2.pop();

            // Error checking
            if (id1 == 0 || id2 == 0) {
                logger.error("Bad state ID.");
                continue;
            }

            // Create combined ID
            long newStateID = IDUtil.combineTwoIDs(id1, first, id2, second);

            // This state has already been created, so it does not need to be created again
            if (automaton.stateExists(newStateID))
                continue;

            // Get states and transitions
            State state1 = first.getState(id1);
            State state2 = second.getState(id2);
            List<Transition> transitions1 = state1.getTransitions();
            List<Transition> transitions2 = state2.getTransitions();

            // Add new state
            automaton.addStateAt(state1.getLabel() + "_" + state2.getLabel(),
                    state1.isMarked() && state2.isMarked(),
                    new ArrayList<Transition>(),
                    id1 == first.getInitialStateID() && id2 == second.getInitialStateID(),
                    newStateID);

            // Find every pair of transitions that have the same events
            for (Transition t1 : transitions1)
                for (Transition t2 : transitions2)
                    if (t1.getEvent().equals(t2.getEvent())) {

                        // Add this pair to the stack
                        stack1.add(t1.getTargetStateID());
                        stack2.add(t2.getTargetStateID());

                        // Add transition to the new automaton
                        long targetID = IDUtil.combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(),
                                second);
                        int eventID = automaton.addTransition(newStateID, t1.getEvent().getLabel(), targetID);

                        // Mark as bad transition if both of them are bad
                        if (first.isBadTransition(id1, t1.getEvent().getID(), t1.getTargetStateID())
                                && second.isBadTransition(id2, t2.getEvent().getID(), t2.getTargetStateID()))
                            automaton.markTransitionAsBad(newStateID, eventID, targetID);

                    }

        }

        /* Re-number states (by removing empty ones) */

        automaton.renumberStates();

        /* Return produced automaton */

        return automaton;
    }

    /**
     * Generates the union of the two specified automata.
     * 
     * @param first  the first automaton
     * @param second the second automaton
     * 
     * @return the union of the two automata
     * 
     * @throws IncompatibleAutomataException If the number of controllers do not
     *                                       match, or the automata have
     *                                       incompatible events
     * @throws NullPointerException          if either one of the arguments is
     *                                       {@code null}
     * 
     * @since 2.1.0
     **/
    public static Automaton union(Automaton first, Automaton second) throws IncompatibleAutomataException {

        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        /* Error checking */

        if (first.getNumberOfControllers() != second.getNumberOfControllers())
            throw new IncompatibleAutomataException();

        /* Setup */

        Automaton automaton = new Automaton(first.getNumberOfControllers());

        // These two stacks should always have the same size
        Deque<Long> stack1 = new ArrayDeque<Long>();
        Deque<Long> stack2 = new ArrayDeque<Long>();

        // Add the initial states to the stack
        stack1.push(first.getInitialStateID());
        stack2.push(second.getInitialStateID());

        /* Build automata by parallel composition */

        // Create two sets containing each automata's private events
        List<Event> privateEvents1 = new ArrayList<Event>();
        privateEvents1.addAll(first.getEvents());
        for (Event e : second.getEvents())
            privateEvents1.remove(e);
        List<Event> privateEvents2 = new ArrayList<Event>();
        privateEvents2.addAll(second.getEvents());
        for (Event e : first.getEvents())
            privateEvents2.remove(e);

        // Create event set (union of both event sets)
        automaton.addAllEvents(first.getEvents());
        automaton.addEventsWithErrorChecking(second.getEvents());

        // Add states and transition
        while (stack1.size() > 0) {

            // Get next IDs
            long id1 = stack1.pop();
            long id2 = stack2.pop();

            // Error checking
            if (id1 == 0 || id2 == 0) {
                logger.error("Bad state ID.");
                continue;
            }

            // Create combined ID
            long newStateID = IDUtil.combineTwoIDs(id1, first, id2, second);

            // This state has already been created, so it does not need to be created again
            if (automaton.stateExists(newStateID))
                continue;

            // Get states and transitions
            State state1 = first.getState(id1);
            State state2 = second.getState(id2);
            List<Transition> transitions1 = state1.getTransitions();
            List<Transition> transitions2 = state2.getTransitions();

            // Add new state
            automaton.addStateAt(
                    state1.getLabel() + "_" + state2.getLabel(),
                    state1.isMarked() && state2.isMarked(),
                    new ArrayList<Transition>(),
                    id1 == first.getInitialStateID() && id2 == second.getInitialStateID(),
                    newStateID);

            // Find every pair of transitions that have the same events (this accounts for
            // public events)
            for (Transition t1 : transitions1)
                for (Transition t2 : transitions2)
                    if (t1.getEvent().equals(t2.getEvent())) {

                        // Add this pair to the stack
                        stack1.add(t1.getTargetStateID());
                        stack2.add(t2.getTargetStateID());

                        // Add transition to the new automaton
                        long targetID = IDUtil.combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(),
                                second);
                        int eventID = automaton.addTransition(newStateID, t1.getEvent().getLabel(), targetID);

                        // Mark as bad transition if either of them are bad
                        if (first.isBadTransition(id1, t1.getEvent().getID(), t1.getTargetStateID())
                                || second.isBadTransition(id2, t2.getEvent().getID(), t2.getTargetStateID()))
                            automaton.markTransitionAsBad(newStateID, eventID, targetID);

                    }

            // Take care of the first automaton's private events
            for (Transition t : transitions1)
                if (privateEvents1.contains(t.getEvent())) {

                    // Add the pair of states to the stack
                    stack1.add(t.getTargetStateID());
                    stack2.add(id2);

                    // Add transition to the new automaton
                    long targetID = IDUtil.combineTwoIDs(t.getTargetStateID(), first, id2, second);
                    int eventID = automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

                    // Mark as bad transition if it is bad
                    if (first.isBadTransition(id1, t.getEvent().getID(), t.getTargetStateID()))
                        automaton.markTransitionAsBad(newStateID, eventID, targetID);

                }

            // Take care of the second automaton's private events
            for (Transition t : transitions2)
                if (privateEvents2.contains(t.getEvent())) {

                    // Add the pair of states to the stack
                    stack1.add(id1);
                    stack2.add(t.getTargetStateID());

                    // Add transition to the new automaton
                    long targetID = IDUtil.combineTwoIDs(id1, first, t.getTargetStateID(), second);
                    int eventID = automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

                    // Mark as bad transition if it is bad
                    if (second.isBadTransition(id2, t.getEvent().getID(), t.getTargetStateID()))
                        automaton.markTransitionAsBad(newStateID, eventID, targetID);

                }

        }

        /* Re-number states (by removing empty ones) */

        automaton.renumberStates();

        /* Return generated automaton */

        return automaton;

    }

}
