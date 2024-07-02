/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.github.automaton.automata.util.*;

import org.apache.commons.collections4.*;
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
                    state.getDisablementEvents());

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

        final long dumpStateID = source.nStates + 1;
        boolean needToAddDumpState = false;

        // Add each state to the new automaton
        for (long s = 1; s <= source.nStates; s++) {

            State state = source.getState(s);

            // Indicate that a dump state already exists, and the complement shouldn't be
            // taken again
            if (state.getLabel().equals(Automaton.DUMP_STATE_LABEL))
                throw new OperationFailedException();

            long id = automaton.addState(state.getLabel(), !state.isMarked(), s == source.initialState);

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
        UStructure uStructure = automaton.synchronizedComposition().relabelConfigurationStates();

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

        Objects.requireNonNull(automaton);

        StopWatch sw = StopWatch.createStarted();

        Pair<Boolean, OptionalInt> obsResult = testObservability(automaton, true);

        if (!obsResult.getLeft())
            throw new IllegalArgumentException("System is not inference observable");

        // Take the U-Structure, then relabel states as needed
        UStructure uStructure = automaton.synchronizedComposition().relabelConfigurationStates();

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

        // Setup global ambiguity level storage
        Map<Event, ListValuedMap<State, Integer>> ambLevels = new LinkedHashMap<>();

        List<AmbiguityData> retList = Collections.synchronizedList(new ArrayList<>());

        for (Event e : IterableUtils.filteredIterable(
                automaton.events, event -> BooleanUtils.or(event.isControllable()))) {

            ListValuedMap<State, Set<State>> neighborMap = MultiMapUtils.newListValuedHashMap();

            Set<State> disablementStates = Collections.unmodifiableSet(uStructure.getDisablementStates(e.getLabel()));
            Set<State> enablementStates = Collections.unmodifiableSet(uStructure.getEnablementStates(e.getLabel()));
            Set<State> controlStates = SetUtils.union(enablementStates, disablementStates);

            // Setup ambiguity level store for current event
            ambLevels.put(e, MultiMapUtils.newListValuedHashMap());

            /*
             * Initialize set of adjacent vertices and ambiguity levels
             */
            for (int i = 0; i < automaton.nControllers; i++) {
                for (State controlState : controlStates) {
                    neighborMap.put(controlState, e.isControllable(i) ? new LinkedHashSet<>() : Collections.emptySet());
                    ambLevels.get(e).put(controlState, e.isControllable(i) ? Integer.MAX_VALUE : -1);
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
                    if (e.isControllable(i) && neighborMap.get(v).get(i).isEmpty()) {
                        vDist.get(i).add(v);
                        ambLevels.get(e).get(v).set(i, infLevel);
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
                logger.printf(Level.DEBUG, "infLevel = %d", infLevel);
                logger.printf(Level.DEBUG, "prevDist = %s", prevDist.toString());
                if (logger.isDebugEnabled()) {
                    logger.debug("neighborMap");
                    for (State s : neighborMap.keySet()) {
                        logger.debug("\tState " + s);
                        List<Set<State>> neighborsList = neighborMap.get(s);
                        for (int i = 0; i < neighborsList.size(); i++) {
                            logger.printf(Level.DEBUG, "\t\t%d: %s", i + 1, neighborsList.get(i));
                        }
                    }
                }
                for (State v : prevDist) {
                    logger.printf(Level.TRACE, "\tv = (%s)", v.getLabel());
                    for (int i = 0; i < automaton.nControllers; i++) {
                        logger.printf(Level.TRACE, "\t\tController %d", i);
                        logger.printf(Level.TRACE, "\t\tNeighbors = %s", neighborMap.get(v).get(i).toString());
                        if (e.isControllable(i)) {
                            if (neighborMap.get(v).get(i).isEmpty()) {
                                if (!vDist.get(i).contains(v)) {
                                    vDist.get(i).add(v);
                                    currDist.add(v);
                                    ambLevels.get(e).get(v).set(i, infLevel);
                                }
                            } else {
                                for (State vPrime : neighborMap.get(v).get(i)) {
                                    neighborMap.get(vPrime).get(i).remove(v);
                                    if (neighborMap.get(vPrime).get(i).isEmpty() && !vDist.get(i).contains(vPrime)) {
                                        vDist.get(i).add(vPrime);
                                        currDist.add(vPrime);
                                        ambLevels.get(e).get(vPrime).set(i, infLevel);
                                    }
                                }
                                neighborMap.get(v).get(i).clear();
                                vDist.get(i).add(v);
                                ambLevels.get(e).get(v).set(i,
                                        /* Math.min(ambLevels.get(e).get(v).get(i), */ infLevel/* ) */);
                            }
                        }
                    }
                }
                prevDist = currDist;
            }

            ambLevels.get(e).keySet().stream().parallel().forEach(state -> {
                List<Integer> ambLevelList = ambLevels.get(e).get(state);
                for (int i = 0; i < automaton.nControllers; i++) {
                    if (e.isControllable(i))
                        retList.add(new AmbiguityData(state, e, i + 1, enablementStates.contains(state),
                                ambLevelList.get(i)));
                }
            });
        }

        long timeTaken = sw.getTime(TimeUnit.MILLISECONDS);

        logger.info("Time taken: " + timeTaken + " ms");

        return retList;

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
                else
                    queue.add(sequence.append(t.getEvent().getID(), t.getTargetStateID()));
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

            long id = twinPlant.addState(state.getLabel(), !state.isMarked(), s == automaton.initialState);

            // Try to add transitions for each event
            for (Event e : automaton.getEvents()) {

                boolean foundMatch = false;

                // Search through each transition for the event
                for (Transition t : state.getTransitions())
                    if (t.getEvent().equals(e)) {
                        twinPlant.addTransition(id, e.getID(), t.getTargetStateID());
                        foundMatch = true;
                    }

                // Check to see if this event is controllable by at least one controller
                boolean controllable = BooleanUtils.or(e.isControllable());

                // Add new transition leading to dump state if this event if undefined at this
                // state and is controllable and active
                if (!foundMatch && controllable && activeEvents.contains(e)) {
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

}
