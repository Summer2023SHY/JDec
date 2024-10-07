/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.github.automaton.automata.incremental.*;
import com.github.automaton.automata.util.*;

import org.apache.commons.collections4.*;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
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
                    state.getEnablementEvents(),
                    state.getDisablementEvents(),
                    state.getIllegalConfigEvents());

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
        for (long s = 1; s <= source.getNumberOfStates(); s++) {

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
                    s == source.initialState, s, new HashSet<>(state.getEnablementEvents()), new HashSet<>(state.getDisablementEvents()), new HashSet<>(state.getIllegalConfigEvents()));

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
     * Generates the complement of the specified automaton.
     * 
     * @param <T>      The type of automaton
     * @param source   an automaton
     * @param supplier a function that creates a new automaton of the same type
     * 
     * @return the complement of {@code source}
     * 
     * @throws NullPointerException     if either one of the arguments is
     *                                  {@code null}
     * @throws OperationFailedException if {@code source} is already a complement of
     *                                  another automaton
     * 
     * @see Automaton#complement()
     */
    public static <T extends Automaton> T complement(final T source, final IntFunction<T> supplier) {

        Objects.requireNonNull(source);
        Objects.requireNonNull(supplier);

        T automaton = supplier.apply(source.nControllers);

        /* Add events */

        automaton.addAllEvents(source.events);

        /* Build complement of this automaton */

        final long dumpStateID = source.getNumberOfStates() + 1;
        boolean needToAddDumpState = false;

        // Add each state to the new automaton
        for (State state : source.getStates()) {
            // Indicate that a dump state already exists, and the complement shouldn't be
            // taken again
            if (state.getLabel().equals(Automaton.DUMP_STATE_LABEL))
                throw new OperationFailedException();

            long id = automaton.addState(state.getLabel(), state.isMarked(), state.getID() == source.initialState);

            // Add transitions for each event (even if they were previously undefined, these
            // transitions will lead to the dump state)
            for (Event e : source.events) {

                boolean foundMatch = false;

                // Search through each transition for the event
                for (Transition t : state.getTransitions())
                    if (t.getEvent().equals(e)) {
                        automaton.addTransition(id, e.getID(), t.getTargetStateID());
                        foundMatch = true;
                    }

                // Add new transition leading to dump state if this event if undefined at this
                // state
                if (!foundMatch) {
                    automaton.addTransition(id, e.getID(), dumpStateID);
                    needToAddDumpState = true;
                }

            }

        }

        /* Create dump state if it needs to be made */

        if (needToAddDumpState) {

            long id = automaton.addState(Automaton.DUMP_STATE_LABEL, false, false);

            if (id != dumpStateID)
                logger.error("Dump state ID did not match expected ID.");

        }

        /* Add special transitions */

        source.copyOverSpecialTransitions(automaton);

        /* Return complement automaton */

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

                        // Mark as bad transition if either of them are bad
                        if (first.isBadTransition(id1, t1.getEvent().getID(), t1.getTargetStateID())
                                || second.isBadTransition(id2, t2.getEvent().getID(), t2.getTargetStateID()))
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

    /**
     * Apply the synchronized composition algorithm to an automaton to produce the
     * U-Structure.
     * 
     * @param automaton an automaton
     * @return The U-Structure
     * 
     * @throws NoInitialStateException  if there was no starting state
     * @throws NullPointerException if argument is {@code null}
     * @throws OperationFailedException if something else went wrong
     * 
     * @since 2.1.0
     **/
    public static UStructure synchronizedComposition(Automaton automaton) {

        Objects.requireNonNull(automaton);

        // Error checking
        if (automaton.getState(automaton.initialState) == null) {
            throw new NoInitialStateException("No starting state");
        }

        /* Setup */

        boolean containsDumpState = automaton.getState(Automaton.DUMP_STATE_LABEL) != null;

        Deque<StateVector> stack = new ArrayDeque<StateVector>();
        Set<StateVector> valuesInStack = new HashSet<StateVector>();
        UStructure uStructure = new UStructure(automaton.getNumberOfControllers());

        /* Add initial state to the stack */

        { // The only reason this is inside a scope is so that variable names could be
          // re-used more cleanly
            List<State> listOfInitialStates = new ArrayList<State>();
            State startingState = automaton.getState(automaton.getInitialStateID());

            // Create list of initial IDs and build the label
            for (int i = 0; i <= automaton.getNumberOfControllers(); i++) {
                listOfInitialStates.add(startingState);
            }

            StateVector initialStateVector = new StateVector(listOfInitialStates, automaton.getNumberOfStates());
            stack.push(initialStateVector);
            valuesInStack.add(initialStateVector);

            uStructure.addStateAt(initialStateVector, true);

        }

        /* Continue until the stack is empty */

        while (stack.size() > 0) {

            StateVector stateVector = stack.pop();
            valuesInStack.remove(stateVector);

            // Get list of IDs and states
            List<State> listOfStates = stateVector.getStates();
            List<Long> listOfIDs = new ArrayList<Long>();
            for (State s : listOfStates)
                listOfIDs.add(s.getID());

            // For each transition in the system automaton
            outer: for (Transition t1 : listOfStates.get(0).getTransitions()) {

                Event e = t1.getEvent();

                List<State> targetStates = new ArrayList<State>();
                State currTargetState = automaton.getState(t1.getTargetStateID());
                targetStates.add(currTargetState);

                List<String> combinedEvent = new ArrayList<>();
                combinedEvent.add(e.getLabel());

                // Determine observable and controllable properties for this event vector
                boolean[] observable = new boolean[automaton.getNumberOfControllers()];
                boolean[] controllable = new boolean[automaton.getNumberOfControllers()];

                // For each controller
                for (int i = 0; i < automaton.getNumberOfControllers(); i++) {

                    // Observable events by this controller
                    if (e.isObservable(i)) {

                        observable[i] = true;

                        // If the event is observable, but not possible at this current time, then we
                        // can skip this altogether
                        State target = null;
                        for (Transition t2 : listOfStates.get(i + 1).getTransitions())
                            if (t2.getEvent().equals(e)) {
                                target = automaton.getState(t2.getTargetStateID());
                            }
                        if (target == null)
                            continue outer;

                        combinedEvent.add(e.getLabel());
                        targetStates.add(target);

                        if (e.isControllable(i)) {

                            controllable[i] = true;

                        }

                    // Unobservable events by this controller
                    } else {
                        combinedEvent.add(Event.EPSILON);
                        targetStates.add(listOfStates.get(i + 1));
                    }

                } // for i

                LabelVector eventLabelVector = new LabelVector(combinedEvent);

                StateVector targetStateVector = new StateVector(targetStates, automaton.getNumberOfStates());

                boolean isConditionalViolation = false, isUnconditionalViolation = false;

                /* Check control configurations */
                if (automaton.transitionExists(listOfIDs.get(0), e.getID(), targetStates.get(0).getID()) && BooleanUtils.or(e.isControllable())) {
                    if (automaton.getBadTransitions().contains(new TransitionData(listOfIDs.get(0), e.getID(), targetStates.get(0).getID())))
                        isUnconditionalViolation = true;
                    else
                        isConditionalViolation = true;
                }
                controlCheck: for (int i = 1; i < listOfIDs.size() && (isConditionalViolation || isUnconditionalViolation); i++) {
                    String eventLabel = eventLabelVector.getLabelAtIndex(i);
                    if ((listOfIDs.get(i) == targetStates.get(i).getID()) && (Objects.equals(eventLabel, Event.EPSILON)))
                        continue controlCheck;
                    else if (!automaton.transitionExists(listOfIDs.get(i), automaton.getEvent(eventLabel).getID(), targetStates.get(i).getID())) {
                        isUnconditionalViolation = isConditionalViolation = false;
                    }
                }

                // Add event
                uStructure.addEventIfNonExisting(eventLabelVector, observable, controllable);

                // Add state if it doesn't already exist
                if (!uStructure.stateExists(targetStateVector.getID())) {

                    // Add state
                    if (!uStructure.addStateAt(targetStateVector, false)) {
                        throw new OperationFailedException("Failed to add state");
                    }

                    // Only add the ID if it's not already waiting to be processed
                    if (!valuesInStack.contains(targetStateVector)) {
                        stack.push(targetStateVector);
                        valuesInStack.add(targetStateVector);
                    } else
                        logger.debug("Prevented adding of state since it was already in the stack.");
                    /*
                     * NOTE: Does this ever get printed to the console? Intuitively it should, but I
                     * have never seen it before. (from Micah Stairs)
                     */
                }

                // Add transition
                int eventID = uStructure.addTransition(stateVector, eventLabelVector, targetStateVector);

                inner: for (int i = 0; i < automaton.getNumberOfControllers(); i++) {
                    if ((isConditionalViolation || isUnconditionalViolation)
                            && !automaton.transitionExistsWithEvent(stateVector.getStateFor(i + 1).getID(),
                                automaton.getEvent(combinedEvent.get(0)).getID())) {
                        isUnconditionalViolation = false;
                        isConditionalViolation = false;
                        break inner;
                    }
                }

                if (isUnconditionalViolation && !automaton.getState(t1.getTargetStateID()).getLabel().contains(Automaton.DUMP_STATE_LABEL)) {
                    uStructure.addUnconditionalViolation(stateVector.getID(), eventID, targetStateVector.getID());
                    stateVector.setDisablementOf(combinedEvent.get(0));
                    boolean validConfig = false;
                    for (int i = 1; !validConfig && i < stateVector.getStates().size(); i++) {
                        State init = stateVector.getStateFor(i);
                        if (automaton.getBadTransitions().parallelStream().anyMatch(td -> td.initialStateID == init.getID() && td.eventID == automaton.getEvent(eventLabelVector.getLabelAtIndex(0)).getID())) {
                            validConfig = true;
                        }
                    }
                    if (!validConfig) {
                        stateVector.setIllegalConfigOf(combinedEvent.get(0));
                    }
                }
                if (isConditionalViolation && !automaton.getState(t1.getTargetStateID()).getLabel().contains(Automaton.DUMP_STATE_LABEL)) {

                    uStructure.addConditionalViolation(stateVector.getID(), eventID, targetStateVector.getID());
                    stateVector.setEnablementOf(combinedEvent.get(0));
                    boolean validConfig = false;
                    for (int i = 1; !validConfig && i < stateVector.getStates().size(); i++) {
                        State init = stateVector.getStateFor(i);
                        if (automaton.getBadTransitions().parallelStream().noneMatch(td -> td.initialStateID == init.getID() && td.eventID == automaton.getEvent(eventLabelVector.getLabelAtIndex(0)).getID())) {
                            validConfig = true;
                        }
                    }
                    if (!validConfig) {
                        stateVector.setIllegalConfigOf(combinedEvent.get(0));
                    }
                }

            } // for

            // For each unobservable transition in the each of the controllers of the
            // automaton
            outer: for (int i = 0; i < automaton.getNumberOfControllers(); i++) {

                for (Transition t : listOfStates.get(i + 1).getTransitions()) {
                    if (!t.getEvent().isObservable(i)) {

                        List<State> targetStates = new ArrayList<State>();
                        List<String> combinedEvent = new ArrayList<>();

                        for (int j = 0; j <= automaton.getNumberOfControllers(); j++) {

                            // The current controller
                            if (j == i + 1) {
                                combinedEvent.add(t.getEvent().getLabel());
                                targetStates.add(automaton.getState(t.getTargetStateID()));
                            } else {
                                combinedEvent.add(Event.EPSILON);
                                targetStates.add(automaton.getState(listOfIDs.get(j)));
                            }

                        }

                        LabelVector eventLabelVector = new LabelVector(combinedEvent);
                        StateVector targetStateVector = new StateVector(targetStates, automaton.getNumberOfStates());

                        // Add event
                        boolean[] observable = new boolean[automaton.getNumberOfControllers()];
                        boolean[] controllable = new boolean[automaton.getNumberOfControllers()];
                        controllable[i] = t.getEvent().isControllable(i);
                        uStructure.addEventIfNonExisting(eventLabelVector, observable, controllable);

                        // Add state if it doesn't already exist
                        if (!uStructure.stateExists(targetStateVector)) {

                            // Add state
                            if (!uStructure.addStateAt(targetStateVector, false)) {
                                throw new OperationFailedException("Failed to add state");
                            }

                            // Only add the ID if it's not already waiting to be processed
                            if (!valuesInStack.contains(targetStateVector)) {
                                stack.push(targetStateVector);
                                valuesInStack.add(targetStateVector);
                            } else
                                logger.debug("Prevented adding of state since it was already in the stack.");

                        }

                        // Add transition
                        int eventID = uStructure.addTransition(stateVector, eventLabelVector, targetStateVector);
                        if (eventID == 0)
                            logger.error("Failed to add transition.");
                    }
                }

            } // for

        } // while

        /* Filter dump state */
        if (containsDumpState) {
            StringBuilder labelBuilder = new StringBuilder(Automaton.DUMP_STATE_LABEL);
            for (int i = 0; i < automaton.getNumberOfControllers(); i++) {
                labelBuilder.append('_');
                labelBuilder.append(Automaton.DUMP_STATE_LABEL);
            }
            String label = labelBuilder.toString();
            long dumpID = uStructure.getStateID(label);
            uStructure.removeState(dumpID);
        }

        /* Re-number states (by removing empty ones) */
        uStructure.renumberStates();

        /* Return produced U-Structure */
        return uStructure;

    }

    /**
     * Tests whether the specified system is inference observable.
     * 
     * @param automaton a system
     * @param showInferenceLevel whether the level of inferencing required should be returned
     * @return whether the system is inference observable and the level of inferencing required
     * 
     * @throws NullPointerException if {@code automaton} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Pair<Boolean, OptionalInt> testObservability(final Automaton automaton, final boolean showInferenceLevel) {

        Objects.requireNonNull(automaton);

        StopWatch sw = StopWatch.createStarted();

        // Take the U-Structure, then relabel states as needed
        UStructure uStructure = UStructureOperations.relabelConfigurationStates(synchronizedComposition(automaton));

        Automaton[] determinizations = new Automaton[automaton.nControllers];
        List<List<State>>[] indistinguishableStatesArr = new List[automaton.nControllers];
        Map<Event, MutableInt> nValues = new HashMap<>();

        IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
            determinizations[i] = uStructure.subsetConstruction(i + 1);
            indistinguishableStatesArr[i] = new ArrayList<>();
            for (State indistinguishableStates : determinizations[i].states.values()) {
                indistinguishableStatesArr[i]
                        .add(uStructure.getStatesFromLabel(new LabelVector(indistinguishableStates.getLabel())));
            }
        });

        for (Event e : IterableUtils.filteredIterable(
                automaton.events, event -> BooleanUtils.or(event.isControllable()))) {

            ListValuedMap<State, Set<State>> neighborMap = MultiMapUtils.newListValuedHashMap();
            /* Initialize value of N for this event (e) */
            nValues.put(e, new MutableInt(-1));

            Set<State> disablementStates = Collections.unmodifiableSet(uStructure.getDisablementStates(e.getLabel()));
            Set<State> enablementStates = Collections.unmodifiableSet(uStructure.getEnablementStates(e.getLabel()));

            /*
             * Initialize set of adjacent vertices
             */
            for (int i = 0; i < automaton.nControllers; i++) {
                for (State controlState : SetUtils.union(enablementStates, disablementStates)) {
                    neighborMap.put(controlState, e.isControllable(i) ? new LinkedHashSet<>() : Collections.emptySet());
                }
            }

            /* Build edges of bipartite graph */
            IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
                List<List<State>> indistinguishableStateLists = indistinguishableStatesArr[i];
                for (List<State> indistinguishableStateList : indistinguishableStateLists) {
                    for (State disablementState : disablementStates) {
                        for (State enablementState : enablementStates) {
                            if (indistinguishableStateList.contains(disablementState)
                                    && indistinguishableStateList.contains(enablementState)
                                    && e.isControllable(i)) {
                                neighborMap.get(disablementState).get(i).add(enablementState);
                                neighborMap.get(enablementState).get(i).add(disablementState);
                            }
                        }
                    }
                }
            });

            Set<State> vDist = new LinkedHashSet<>();

            int infLevel = 0;

            for (State v : neighborMap.keySet()) {
                for (int i = 0; i < automaton.nControllers; i++) {
                    if (e.isControllable(i) && neighborMap.get(v).get(i).isEmpty()) {
                        vDist.add(v);
                        nValues.get(e).setValue(infLevel);
                    }
                }
            }

            Set<State> prevDist = new LinkedHashSet<>(vDist);

            while (!prevDist.isEmpty()) {
                Set<State> currDist = new LinkedHashSet<>();
                infLevel += 1;
                logger.printf(Level.DEBUG, "infLevel = %d", infLevel);
                for (State v : prevDist) {
                    logger.printf(Level.TRACE, "\tv = (%s)", v.getLabel());
                    for (int i = 0; i < automaton.nControllers; i++) {
                        logger.printf(Level.TRACE, "\t\tController %d", i);
                        logger.printf(Level.TRACE, "\t\tNeighbors = %s", neighborMap.get(v).get(i).toString());
                        if (e.isControllable(i)) {
                            if (neighborMap.get(v).get(i).isEmpty() && !vDist.contains(v)) {
                                currDist.add(v);
                                if (infLevel > nValues.get(e).intValue())
                                    nValues.get(e).setValue(infLevel);
                            } else {
                                for (State vPrime : neighborMap.get(v).get(i)) {
                                    neighborMap.get(vPrime).get(i).remove(v);
                                    if (neighborMap.get(vPrime).get(i).isEmpty() && !vDist.contains(vPrime)) {
                                        currDist.add(vPrime);
                                        if (infLevel > nValues.get(e).intValue())
                                            nValues.get(e).setValue(infLevel);
                                    }
                                }
                            }
                        }
                    }
                }
                vDist.addAll(currDist);
                prevDist = currDist;
            }
            if (vDist.size() < neighborMap.keySet().size()) {
                long timeTaken = sw.getTime(TimeUnit.MILLISECONDS);
                logger.info("Time taken: " + timeTaken + " ms");
                return Pair.of(false, OptionalInt.empty());
            }
        }
        OptionalInt n = showInferenceLevel ? nValues.values().stream().mapToInt(MutableInt::intValue).max()
                : OptionalInt.empty();

        long timeTaken = sw.getTime(TimeUnit.MILLISECONDS);
        logger.info("Time taken: " + timeTaken + " ms");

        return Pair.of(true, n);
    }

    /**
     * Calculates the ambiguity levels for each control configuration in the
     * specified system.
     * 
     * @param automaton a system
     * 
     * @return the ambiguity levels for each control configuration, if the system is
     *         inference observable
     * 
     * @throws IllegalArgumentException if system is not inference observable
     * @throws NullPointerException     if argument is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static List<AmbiguityData> calculateAmbiguityLevels(final Automaton automaton) {

        return generateLocalControlDecisions(automaton, false);

    }

    @SuppressWarnings("unchecked")
    public static List<AmbiguityData> generateLocalControlDecisions(final Automaton automaton, boolean enablement) {

        Objects.requireNonNull(automaton);

        StopWatch sw = StopWatch.createStarted();

        Pair<Boolean, OptionalInt> obsResult = testObservability(automaton, true);

        if (!obsResult.getLeft())
            throw new IllegalArgumentException("System is not inference observable");

        // Take the U-Structure, then relabel states as needed
        UStructure uStructure = UStructureOperations.relabelConfigurationStates(synchronizedComposition(automaton));

        Automaton[] determinizations = new Automaton[automaton.nControllers];
        List<List<State>>[] indistinguishableStatesArr = new List[automaton.nControllers];

        // Build list of indistinguishable states
        IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
            determinizations[i] = uStructure.subsetConstruction(i + 1);
            indistinguishableStatesArr[i] = new ArrayList<>();
            for (State indistinguishableStates : determinizations[i].states.values()) {
                indistinguishableStatesArr[i]
                        .add(uStructure.getStatesFromLabel(new LabelVector(indistinguishableStates.getLabel())));
            }
        });

        List<AmbiguityData> retList = Collections.synchronizedList(new ArrayList<>());

        for (Event e : IterableUtils.filteredIterable(
                automaton.events, event -> BooleanUtils.or(event.isControllable()))) {

            ListValuedMap<State, Set<State>> neighborMap = MultiMapUtils.newListValuedHashMap();

            Set<State> disablementStates = Collections.unmodifiableSet(uStructure.getDisablementStates(e.getLabel()));
            Set<State> enablementStates = Collections.unmodifiableSet(uStructure.getEnablementStates(e.getLabel()));
            Set<State> controlStates = SetUtils.union(enablementStates, disablementStates);

            /*
             * Initialize set of adjacent vertices and ambiguity levels
             */
            for (int i = 0; i < automaton.nControllers; i++) {
                for (State controlState : controlStates) {
                    neighborMap.put(controlState, e.isControllable(i) ? new LinkedHashSet<>() : Collections.emptySet());
                }
            }

            /* Build edges of bipartite graph */
            IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
                List<List<State>> indistinguishableStateLists = indistinguishableStatesArr[i];
                for (List<State> indistinguishableStateList : indistinguishableStateLists) {
                    for (State disablementState : disablementStates) {
                        for (State enablementState : enablementStates) {
                            if (indistinguishableStateList.contains(disablementState)
                                    && indistinguishableStateList.contains(enablementState)) {
                                neighborMap.get(disablementState).get(i).add(enablementState);
                                neighborMap.get(enablementState).get(i).add(disablementState);
                            }
                        }
                    }
                }
            });

            // vDist is the collection of set of vertices that can be distinguished by i
            List<Set<State>> vDist = new ArrayList<>(automaton.nControllers);
            for (int i = 0; i < automaton.nControllers; i++) {
                vDist.add(e.isControllable(i) ? new LinkedHashSet<>() : Collections.emptySet());
            }

            int infLevel = 0;

            for (State v : neighborMap.keySet()) {
                for (int i = 0; i < automaton.nControllers; i++) {
                    if (e.isControllable(i) && neighborMap.get(v).get(i).isEmpty() && (enablement ^ v.isDisablementStateOf(e.getLabel()))) {
                        retList.add(new AmbiguityData(v, e, i + 1, enablement, infLevel));
                        vDist.get(i).add(v);
                    }
                }
            }

            Set<State> prevDist = new LinkedHashSet<>();
            for (Set<State> vDistI : vDist) {
                prevDist.addAll(vDistI);
            }

            while (infLevel <= obsResult.getRight().getAsInt()) {
                Set<State> currDist = new LinkedHashSet<>();
                infLevel += 1;
                for (State v : prevDist) {
                    for (int i = 0; i < automaton.nControllers; i++) {
                        if (e.isControllable(i)) {
                            var neighborStates = neighborMap.get(v).get(i);
                            if (!neighborStates.isEmpty()) {
                                for (State vPrime : neighborStates) {
                                    neighborMap.get(vPrime).get(i).remove(v);
                                    if (neighborMap.get(vPrime).get(i).isEmpty() && !vDist.get(i).contains(vPrime)) {
                                        currDist.add(vPrime);
                                        retList.add(new AmbiguityData(vPrime, e, i + 1, (infLevel % 2 == 1) ^ enablement, infLevel));
                                    }
                                }
                                neighborStates.clear();
                                vDist.get(i).add(v);
                                retList.add(new AmbiguityData(v, e, i + 1, (infLevel % 2 == 1) ^ enablement, infLevel));
                            }
                        }
                    }
                }
                prevDist = currDist;
            }
        }

        long timeTaken = sw.getTime(TimeUnit.MILLISECONDS);

        logger.info("Time taken: " + timeTaken + " ms");

        return retList;

    }

    /**
     * Tests whether the specified system is inference observable.
     * 
     * @param automaton a system
     * @param showInferenceLevel whether the level of inferencing required should be returned
     * @return whether the system is inference observable and the level of inferencing required
     * 
     * @throws NullPointerException if {@code automaton} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Map<Event, ListValuedMap<State, Set<State>>> generateBipartiteGraph(final Automaton automaton) {

        Objects.requireNonNull(automaton);

        // Take the U-Structure, then relabel states as needed
        UStructure uStructure = UStructureOperations.relabelConfigurationStates(synchronizedComposition(automaton));

        Automaton[] determinizations = new Automaton[automaton.nControllers];
        List<List<State>>[] indistinguishableStatesArr = new List[automaton.nControllers];

        IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
            determinizations[i] = uStructure.subsetConstruction(i + 1);
            indistinguishableStatesArr[i] = new ArrayList<>();
            for (State indistinguishableStates : determinizations[i].states.values()) {
                indistinguishableStatesArr[i]
                        .add(uStructure.getStatesFromLabel(new LabelVector(indistinguishableStates.getLabel())));
            }
        });

        Map<Event, ListValuedMap<State, Set<State>>> bipartiteGraphs = new HashMap<>();

        for (Event e : IterableUtils.filteredIterable(
                automaton.events, event -> BooleanUtils.or(event.isControllable()))) {

            ListValuedMap<State, Set<State>> neighborMap = MultiMapUtils.newListValuedHashMap();

            Set<State> disablementStates = Collections.unmodifiableSet(uStructure.getDisablementStates(e.getLabel()));
            Set<State> enablementStates = Collections.unmodifiableSet(uStructure.getEnablementStates(e.getLabel()));

            /*
             * Initialize set of adjacent vertices
             */
            for (int i = 0; i < automaton.nControllers; i++) {
                for (State controlState : SetUtils.union(enablementStates, disablementStates)) {
                    neighborMap.put(controlState, e.isControllable(i) ? new LinkedHashSet<>() : Collections.emptySet());
                }
            }

            /* Build edges of bipartite graph */
            IntStream.range(0, automaton.nControllers).parallel().forEach(i -> {
                List<List<State>> indistinguishableStateLists = indistinguishableStatesArr[i];
                for (List<State> indistinguishableStateList : indistinguishableStateLists) {
                    for (State disablementState : disablementStates) {
                        for (State enablementState : enablementStates) {
                            if (indistinguishableStateList.contains(disablementState)
                                    && indistinguishableStateList.contains(enablementState)
                                    && e.isControllable(i)) {
                                neighborMap.get(disablementState).get(i).add(enablementState);
                                neighborMap.get(enablementState).get(i).add(disablementState);
                            }
                        }
                    }
                }
            });
            bipartiteGraphs.put(e, neighborMap);
        }
        return bipartiteGraphs;
    }

    /**
     * Generates the language recognized by the specified automaton.
     * 
     * @param automaton an automaton
     * @return the language that the automaton recognizes
     * 
     * @throws IllegalArgumentException if argument recognizes an infinite language
     * @throws NoInitialStateException  if argument has no initial state
     * @throws NullPointerException     if argument is {@code null}
     */
    public static Set<Word> buildLanguage(Automaton automaton) {
        Objects.requireNonNull(automaton);
        if (!automaton.stateExists(automaton.getInitialStateID())) {
            throw new NoInitialStateException();
        } else if (automaton.hasSelfLoop(automaton.getAllTransitions())) {
            throw new IllegalArgumentException();
        }
        Queue<Sequence> queue = new ArrayDeque<>();
        Set<Word> language = new LinkedHashSet<>();
        queue.add(new Sequence(automaton.getInitialStateID()));
        while (!queue.isEmpty()) {
            Sequence sequence = queue.remove();
            State lastState = automaton.getState(sequence.getState(sequence.length() - 1));
            if (lastState.isMarked()) {
                int[] eventIDs = sequence.getEventArray();
                String[] events = new String[eventIDs.length];
                for (int i = 0; i < events.length; i++) {
                    events[i] = automaton.getEvent(eventIDs[i]).getLabel();
                }
                language.add(new Word(events));
            }
            for (Transition t : lastState.getTransitions()) {
                if (sequence.containsState(t.getTargetStateID()))
                    throw new IllegalArgumentException();
                else {
                    if (!automaton.getBadTransitions().contains(new TransitionData(lastState.getID(), t.getEvent().getID(), t.getTargetStateID()))) {
                        queue.add(sequence.append(t.getEvent().getID(), t.getTargetStateID()));
                    }
                }
            }
        }
        return language;
    }

    /**
     * Generates the twin plant of the specified automaton.
     * 
     * <p>
     * The technique used here is similar to how the complement works.
     * This would not work in all cases, but G_{&Sigma;*} is a special case.
     * 
     * @param automaton an automaton
     * 
     * @return the twin plant of the specified automaton
     */
    public static Automaton generateTwinPlant(final Automaton automaton) {

        Objects.requireNonNull(automaton);

        Automaton twinPlant = new Automaton(automaton.getNumberOfControllers());

        /* Add events */

        twinPlant.addAllEvents(automaton.getEvents());

        /* Build twin plant */

        long dumpStateID = automaton.getNumberOfStates() + 1;
        boolean needToAddDumpState = false;

        List<Event> activeEvents = automaton.getActiveEvents();

        // Add each state to the new automaton
        for (long s = 1; s <= automaton.getNumberOfStates(); s++) {

            State state = automaton.getState(s);

            long id = twinPlant.addState(state.getLabel(), state.isMarked(), s == automaton.initialState);

            // Try to add transitions for each event
            for (Event e : automaton.getEvents()) {

                boolean foundMatch = false;

                // Search through each transition for the event
                for (Transition t : state.getTransitions())
                    if (t.getEvent().equals(e)) {
                        twinPlant.addTransition(id, e.getID(), t.getTargetStateID());
                        foundMatch = true;
                    }

                // Add new transition leading to dump state if this event if undefined at this
                // state and is active
                if (!foundMatch && activeEvents.contains(e)) {
                    twinPlant.addTransition(id, e.getID(), dumpStateID);
                    twinPlant.markTransitionAsBad(id, e.getID(), dumpStateID);
                    needToAddDumpState = true;
                }

            }

        }

        /* Create dump state if it needs to be made */

        if (needToAddDumpState) {

            long id = twinPlant.addState(Automaton.DUMP_STATE_LABEL, false, false);

            if (id != dumpStateID)
                logger.error("Dump state ID did not match expected ID.");

        }

        /* Add special transitions */

        automaton.copyOverSpecialTransitions(twinPlant);    

        /* Return generated automaton */

        return twinPlant;

    }

    /**
     * Given a set of plants and specifications, test whether the combined system is inference observable.
     * This method uses random order for querying the system components.
     * 
     * @param plants a set of plants
     * @param specs a set of specifications
     * @return {@code true} if the combined system is inference observable
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public static boolean testIncrementalObservability(Set<Automaton> plants, Set<Automaton> specs) {
        return testIncrementalObservability(plants, specs, RandomOrderComponentIterable::new);
    }

    /**
     * Given a set of plants and specifications, test whether the combined system is inference observable.
     * This method uses the specified heuristic for querying the system components.
     * 
     * @param plants a set of plants
     * @param specs a set of specifications
     * @param componentHeuristicSupplier a component heuristic supplier
     * 
     * @return {@code true} if the combined system is inference observable
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public static boolean testIncrementalObservability(Set<Automaton> plants, Set<Automaton> specs, FilteredComponentIterableGenerator componentHeuristicSupplier) {
        return testIncrementalObservability(plants, specs, CounterexampleHeuristics.NONE, RandomOrderComponentIterable::new);
    }

    /**
     * Given a set of plants and specifications, test whether the combined system is inference observable.
     * This method uses the specified heuristic for querying the system components.
     * 
     * @param plants a set of plants
     * @param specs a set of specifications
     * @param counterexampleHeuristic a counterexample heuristic
     * @param componentHeuristicSupplier a component heuristic supplier
     * 
     * @return {@code true} if the combined system is inference observable
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public static boolean testIncrementalObservability(Set<Automaton> plants, Set<Automaton> specs, CounterexampleHeuristics counterexampleHeuristic, FilteredComponentIterableGenerator componentHeuristicSupplier) {
        Objects.requireNonNull(plants);
        Objects.requireNonNull(specs);
        Objects.requireNonNull(counterexampleHeuristic);
        Objects.requireNonNull(componentHeuristicSupplier);

        /* Create copies of the sets to avoid modifying supplied sets */
        Set<Automaton> G = new LinkedHashSet<>(plants);
        Set<Automaton> H = new LinkedHashSet<>(specs);

        logger.info("Starting incremental observability check");
        logger.info("Counterexample heuristic: " + counterexampleHeuristic.toString());
        logger.info("Component heuristic: " + componentHeuristicSupplier.toString());
        StopWatch sw = StopWatch.createStarted();
        int nComponentChecks = 0;

        while (!H.isEmpty()) {
            Automaton Hj = H.iterator().next();
            Set<Automaton> Hprime = new LinkedHashSet<>();
            Set<Automaton> Gprime = new LinkedHashSet<>();
            Hprime.add(Hj);
            Automaton combinedSys = generateTwinPlant(Hj);
            while (!testObservability(combinedSys, false).getLeft()) {
                UStructure uStructure = UStructureOperations.relabelConfigurationStates(synchronizedComposition(combinedSys));
                List<List<Word>> counterExamplesRaw = new ArrayList<>();
                List<List<Word>> counterExamples = SetUniqueList.setUniqueList(counterExamplesRaw);
                for (Event controllableEvent : combinedSys.getControllableEvents()) {
                    var enablementStates = uStructure.getEnablementStates(controllableEvent.getLabel());
                    var illegalConfigs = uStructure.getIllegalConfigStates(controllableEvent.getLabel());
                    for (var illegalConfig : illegalConfigs) {
                        if (!enablementStates.contains(illegalConfig)) {
                            illegalConfig.setMarked(true);
                            var trim = uStructure.trim();
                            SubsetConstruction subsetConstruction = trim.subsetConstruction(0);
                            var counterExample = buildCounterexample(subsetConstruction);
                            counterExamples.add(counterExample);
                            illegalConfig.setMarked(false);
                        }
                    }
                }
                counterExamplesRaw.sort(counterexampleHeuristic);

                boolean found = false;
                List<Word> counterExample = counterExamplesRaw.get(0);
                logger.info("Current counterexample: " + counterExample);
                var componentIterator = componentHeuristicSupplier.generate(G, H, Gprime, Hprime).iterator();
                componentSearch: while (!found && componentIterator.hasNext()) {
                    var M = componentIterator.next();
                    logger.info("Current component: " + M);
                    nComponentChecks++;
                    if (M.recognizesWords(counterExample)) {
                        found = true;
                        if (G.contains(M))
                            Gprime.add(M);
                        else
                            Hprime.add(M);
                        break componentSearch;
                    }
                }

                if (!found) {
                    logger.info("Time taken: " + sw.getTime(TimeUnit.MILLISECONDS) + " ms");
                    logger.info("Number of component checks: " + nComponentChecks);
                    return false;
                }
                combinedSys = buildCombinedSystem(Gprime, Hprime);
                H.removeAll(Hprime);
                G.addAll(Hprime);
            }
        }
        logger.info("Time taken: " + sw.getTime(TimeUnit.MILLISECONDS) + " ms");
        logger.info("Number of component checks: " + nComponentChecks);
        return true;
    }

    private static Automaton buildCombinedSystem(Set<Automaton> plants, Set<Automaton> specs) {
        Automaton compositeSpec = buildCompositeAutomaton(specs);
        if (plants.isEmpty()) {
            return compositeSpec.generateTwinPlant();
        }
        Automaton compositePlant = buildCompositeAutomaton(plants);
        Automaton combinedSys = intersection(compositePlant.generateTwinPlant(), compositeSpec.generateTwinPlant());
        BitSet bSet = new BitSet();
        for (long stateId = 1; stateId <= combinedSys.getNumberOfStates(); stateId++) {
            if (!combinedSys.stateExists(stateId))
                continue;
            State s = combinedSys.getState(stateId);
            String[] stateLabels = s.getLabel().split("_");
            boolean plantStateIsDump = Objects.equals(stateLabels[0], Automaton.DUMP_STATE_LABEL);
            boolean specStateIsDump = Objects.equals(stateLabels[1], Automaton.DUMP_STATE_LABEL);
            if (plantStateIsDump && specStateIsDump) {
                combinedSys.removeState(stateId);
            } else if (specStateIsDump) {
                s.setMarked(true);
                bSet.set((int) stateId);
            }
        }
        var bad = combinedSys.getAllTransitions().parallelStream().filter(td -> bSet.get((int) td.targetStateID)).toList();
        for (var badTd : bad) {
            combinedSys.markTransitionAsBad(badTd.initialStateID, badTd.eventID, badTd.targetStateID);
        }
        combinedSys.renumberStates();
        relabelStates(combinedSys);
        return combinedSys;
    }

    private static Automaton buildCompositeAutomaton(Set<Automaton> automata) {
        if (automata.size() == 1) {
            return automata.iterator().next();
        }
        return relabelStates(automata.parallelStream().reduce(AutomataOperations::intersection)
                .orElseThrow(IllegalArgumentException::new));
    }

    private static <T extends Automaton> T relabelStates(T automaton) {
        for (State s : automaton.getStates()) {
            s.setLabel(Long.toString(s.getID()));
        }
        return automaton;
    }

    private static List<Word> buildCounterexample(final SubsetConstruction subsetConstruction) {

        State currState = subsetConstruction.getState(subsetConstruction.initialState);
        Sequence seq = new Sequence(currState.getID());

        while (currState.getNumberOfTransitions() > 0) {
            Transition transition = currState.getTransition(0);
            seq = seq.append(transition.getEvent().getID(), transition.getTargetStateID());
            currState = subsetConstruction.getState(transition.getTargetStateID());
        }

        List<List<String>> temp = new ArrayList<>();
        for (int i = 0; i <= subsetConstruction.nControllers; i++) {
            temp.add(new ArrayList<>());
        }
        for (int eventID : seq.getEventList()) {
            Event e = subsetConstruction.getEvent(eventID);
            LabelVector lv = e.getVector();
            for (int i = 0; i <= subsetConstruction.nControllers; i++) {
                temp.get(i).add(lv.getLabelAtIndex(i));
            }
        }
        List<Word> words = new ArrayList<>();

        for (int i = 0; i <= subsetConstruction.nControllers; i++) {
            words.add(new Word(temp.get(i)));
        }
        return words;
    }

}
