/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

/**
 * A collection of U-Structure operations.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class UStructureOperations {

    /**
     * Creates a copy of the specified U-Structure that has copies of same state(s)
     * if the state appears in more than one projections.
     * 
     * @return a copy of the specified U-Structure with relabeled states
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    public static UStructure relabelConfigurationStates(UStructure uStructure) {

        Objects.requireNonNull(uStructure);

        SubsetConstruction subsetConstruction = new SubsetConstruction(uStructure, 0);

        /* Collection of counters for occurrences of original states */
        MultiSet<Long> stateIDMultiSet = new HashMultiSet<>();
        /* Mapping of state set IDs to their member state IDs */
        final Map<Long, Map<Long, Long>> relabelMapping = new LinkedHashMap<>();

        /*
         * Build a queue for breadth-first traversal of the UStructure
         * Each entry of the queue consists of the triple of:
         * 
         * - ID of the current state of interest
         * - the sequence of states and events that led to this state
         * - whether a cycle has been detected
         */
        Queue<Triple<Long, Sequence, Boolean>> combinedStateQueue = new ArrayDeque<>();
        combinedStateQueue.add(Triple.of(subsetConstruction.initialState, new Sequence(subsetConstruction.initialState), true));

        UStructure relabeled = new UStructure(uStructure.getNumberOfControllers());
        relabeled.addAllEvents(uStructure.getEvents());

        while (!combinedStateQueue.isEmpty()) {
            Triple<Long, Sequence, Boolean> currSequence = combinedStateQueue.remove();

            StateSet ss = subsetConstruction.getStateAsStateSet(currSequence.getLeft());
            Map<Long, Long> currStateSetIDMap = currSequence.getRight() ? new LinkedHashMap<>() : relabelMapping.get(currSequence.getLeft());
            if (currSequence.getRight()) {
                relabelMapping.put(ss.getID(), currStateSetIDMap);
                /* Calculate new state IDs for relabeling */
                for (State s : ss.getSet()) {
                    long origID = s.getID();
                    long modID = origID + uStructure.getNumberOfStates() * stateIDMultiSet.getCount(origID);
                    currStateSetIDMap.put(s.getID(), modID);
                    State modState = new State(
                            s.getLabel() + (stateIDMultiSet.getCount(origID) == 0 ? StringUtils.EMPTY
                                    : "-" + Integer.toString(stateIDMultiSet.getCount(origID))),
                            modID, false, s.getEnablementEvents(), s.getDisablementEvents(), s.getIllegalConfigEvents());
                    relabeled.addStateAt(modState, false);
                    stateIDMultiSet.add(origID);
                }
                /* Add transitions to states in the same state set */
                for (State origS : ss.getSet()) {
                    State modS = relabeled.getState(currStateSetIDMap.get(origS.getID()));
                    for (Transition t : origS.getTransitions()) {
                        if (currStateSetIDMap.containsKey(t.getTargetStateID())) {
                            relabeled.addTransition(modS.getID(), t.getEvent().getLabel(),
                                    currStateSetIDMap.get(t.getTargetStateID()));
                        }
                    }
                }
            }
            /* Handle transitions from preceding state set */
            if (currSequence.getMiddle().getEventArray().length > 0) {
                long prevStateSetID = currSequence.getMiddle().getState(currSequence.getMiddle().length() - 2);
                StateSet prevStateSet = subsetConstruction.getStateAsStateSet(prevStateSetID);
                Map<Long, Long> prevStateSetIDMap = relabelMapping.get(prevStateSetID);
                for (State prevS : prevStateSet.getSet()) {
                    for (Transition prevT : prevS.getTransitions()) {
                        if (currStateSetIDMap.containsKey(prevT.getTargetStateID())) {
                            relabeled.addTransition(prevStateSetIDMap.get(prevS.getID()), prevT.getEvent().getLabel(),
                                    currStateSetIDMap.get(prevT.getTargetStateID()));
                        }
                    }
                }
            }

            /* Detect next entries for breadth-first traversal */
            for (Transition t : IterableUtils.filteredIterable(
                    ss.getTransitions(), t -> t.getTargetStateID() != ss.getID())) {
                if (!currSequence.getMiddle().containsState(t.getTargetStateID()) && currSequence.getRight()) {
                    /* Next transition found */
                    combinedStateQueue.add(Triple.of(t.getTargetStateID(),
                            currSequence.getMiddle().append(t.getEvent().getID(), t.getTargetStateID()), true));
                } else if (!currSequence.getMiddle().isLastState(t.getTargetStateID()) && currSequence.getRight()) {
                    /* Cycle detected */
                    combinedStateQueue.add(Triple.of(t.getTargetStateID(),
                            currSequence.getMiddle().append(t.getEvent().getID(), t.getTargetStateID()), false));
                }
            }

        }

        /* Restore violation states in relabeled U-Structure */
        uStructure.copyOverSpecialTransitions(relabeled);
        for (TransitionData unconditionalTd : uStructure.getUnconditionalViolations()) {
            long initStateID = unconditionalTd.initialStateID;
            long targetStateID = unconditionalTd.targetStateID;
            for (int i = 1; i < stateIDMultiSet.getCount(unconditionalTd.initialStateID); i++) {
                long relabeledInitStateID = initStateID + uStructure.getNumberOfStates() * i;
                for (int j = 1; j < stateIDMultiSet.getCount(targetStateID); j++) {
                    long relabeledTargetStateID = targetStateID + uStructure.getNumberOfStates() * j;
                    if (relabeled.transitionExists(relabeledInitStateID, unconditionalTd.eventID,
                            relabeledTargetStateID)) {
                        relabeled.addUnconditionalViolation(relabeledInitStateID, unconditionalTd.eventID,
                                relabeledTargetStateID);
                    }
                }
            }
        }
        for (TransitionData conditionalTd : uStructure.getConditionalViolations()) {
            long initStateID = conditionalTd.initialStateID;
            long targetStateID = conditionalTd.targetStateID;
            for (int i = 1; i < stateIDMultiSet.getCount(conditionalTd.initialStateID); i++) {
                long relabeledInitStateID = initStateID + uStructure.getNumberOfStates() * i;
                for (int j = 1; j < stateIDMultiSet.getCount(targetStateID); j++) {
                    long relabeledTargetStateID = targetStateID + uStructure.getNumberOfStates() * j;
                    if (relabeled.transitionExists(relabeledInitStateID, conditionalTd.eventID,
                            relabeledTargetStateID)) {
                        relabeled.addConditionalViolation(relabeledInitStateID, conditionalTd.eventID,
                                relabeledTargetStateID);
                    }
                }
            }
        }

        relabeled.setInitialStateID(uStructure.getInitialStateID());

        relabeled.renumberStates();
        return relabeled;
    }

    /** Cached values of factorials */
    private static int[] factorial = new int[13];

    /**
     * Recursively find the factorial of the specified number.
     * 
     * @param n The number to take the factorial of, must be in the range [0,12]
     * @return The factorial value
     * 
     * @throws ArithmeticException if {@code n} is outside allowed range
     **/
    private static int factorial(int n) {

        // Error checking
        if (n < 0 || n > 12) {
            throw new ArithmeticException("Factorial value of " + n + " is outside allowed range.");
        }

        if (factorial[n] == 0) {
            // Base case
            if (n == 0)
                factorial[n] = 1;
            else
                factorial[n] = n * factorial(n - 1);
        }

        return factorial[n];
    }


    /**
     * Given the Shapley values for each coalition, and the index of a controller,
     * calculate its Shapley value.
     * NOTE: This calculation is specified in the paper 'Coalitions of the willing:
     * Decentralized discrete-event
     * control as a cooperative game', in section 3.
     * 
     * @param shapleyValues     The mappings between the coalitions and their
     *                          associated Shapley values
     * @param indexOfController The index of the controller (1-based)
     * @return The Shapley value of the specified controller
     **/
    public static double findShapleyValueForController(UStructure uStructure, Map<Set<Integer>, Integer> shapleyValues, int indexOfController) {

        int sum = 0;

        // Iterate through each coalition
        for (Map.Entry<Set<Integer>, Integer> entry : shapleyValues.entrySet()) {
            Set<Integer> coalition = entry.getKey();

            // Skip this coalition if it contains the controller
            if (coalition.contains(indexOfController))
                continue;

            Integer shapleyValueWithoutController = entry.getValue();

            // Find the Shapley value of this coalition if the controller were to be added
            Set<Integer> coalitionWithController = new HashSet<Integer>(coalition);
            coalitionWithController.add(indexOfController);
            Integer shapleyValueWithController = shapleyValues.get(coalitionWithController);

            // Add calculated value to summation
            sum += factorial(coalition.size())
                    * factorial(uStructure.getNumberOfControllers() - coalition.size() - 1)
                    * (shapleyValueWithController - shapleyValueWithoutController);

        }

        return (double) sum / (double) factorial(uStructure.getNumberOfControllers());

    }
}
