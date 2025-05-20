/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.*;

import com.google.gson.JsonObject;

/**
 * A collection of U-Structure operations.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class UStructureOperations {

    private static Logger logger = LogManager.getLogger();

    /**
     * Generate a new U-Structure, with all communications added (potential
     * communications are marked).
     * 
     * @return The U-Structure with the added transitions
     * 
     * @since 2.0
     **/
    public static UStructure addCommunications(UStructure orig) {

        /* Setup */

        // Create a mapping between the event labels and their associated properties in
        // the original automaton
        // NOTE: The controller's index (1-based, in this case) is appended to the
        // event's label since each
        // controller has different properties for each event
        Map<String, Boolean> observableMapping = new HashMap<String, Boolean>();
        Map<String, Boolean> controllableMapping = new HashMap<String, Boolean>();
        for (Event e : orig.getEvents()) {
            LabelVector vector = e.getVector();
            for (int i = 1; i < vector.getSize(); i++) {
                String label = vector.getLabelAtIndex(i);
                observableMapping.put(label + i, e.isObservable(i - 1));
                controllableMapping.put(label + i, e.isControllable(i - 1));
            }
        }

        // Generate all potential communication labels
        Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
        for (Event e : orig.getEvents())
            leastUpperBounds.add(e.getVector());
        Set<CommunicationLabelVector> potentialCommunications = findPotentialCommunicationLabels(leastUpperBounds);

        // Generate all least upper bounds (if invalid communications are not being
        // suppressed)
        if (UStructure.SUPPRESS_INVALID_COMMUNICATIONS) {
            leastUpperBounds.addAll(potentialCommunications);
        } else
            generateLeastUpperBounds(leastUpperBounds);

        UStructure uStructure = orig.clone();

        /* Add communications (marking the potential communications) */

        // Map<String, State> memoization = new HashMap<String, State>();
        for (State startingState : uStructure.getStates()) {

            // Try each least upper bound
            for (LabelVector vector : leastUpperBounds) {

                boolean[] vectorElementsFound = new boolean[vector.getSize()];
                State destinationState = findWhereCommunicationLeads(uStructure, vector, vectorElementsFound,
                        startingState/* , memoization */);

                if (destinationState != null) {

                    // Add event if it doesn't already exist
                    int id;
                    Event event = uStructure.getEvent(vector.toString());
                    if (event == null) {

                        // Determine observable and controllable properties of the event vector
                        boolean[] observable = new boolean[orig.nControllers];
                        boolean[] controllable = new boolean[orig.nControllers];
                        for (int i = 1; i < vector.getSize(); i++) {
                            String label = vector.getLabelAtIndex(i);
                            if (!label.equals(Event.EPSILON)) {
                                observable[i - 1] = observableMapping.get(label + i);
                                controllable[i - 1] = controllableMapping.get(label + i);
                            }
                        }

                        id = uStructure.addEvent(vector.toString(), observable, controllable);
                    } else
                        id = event.getID();

                    // Add the transition (if it doesn't already exist)
                    if (!uStructure.transitionExists(startingState.getID(), id, destinationState.getID())) {

                        // Add transition
                        uStructure.addTransition(startingState.getID(), id, destinationState.getID());

                        // There could be more than one potential communication, so we need to mark them
                        // all
                        boolean found = false;
                        for (CommunicationLabelVector data : potentialCommunications)
                            if (vector.equals((LabelVector) data)) {
                                uStructure.addPotentialCommunication(startingState.getID(), id,
                                        destinationState.getID(), data.roles);
                                found = true;
                            }

                        // If there were no potential communications, then it must be a invalid
                        // communication
                        if (!found) {
                            if (UStructure.SUPPRESS_INVALID_COMMUNICATIONS)
                                logger.error("Invalid communication was not suppressed: " + vector);
                            uStructure.addInvalidCommunication(startingState.getID(), id, destinationState.getID());
                        }

                    }

                }

            }

        }

        return uStructure;

    }

    /**
     * Using recursion, starting at a given state, determine which state the
     * specified communication leads to (if it exists).
     * 
     * @param communication       The event vector representing the communication
     * @param vectorElementsFound Indicates which elements of the vector have been
     *                            found
     * @param currentState        The state that we are currently on
     * @return The destination state (or {@code null} if the communication does not
     *         lead to a state)
     **/
    private static State findWhereCommunicationLeads(UStructure uStructure, LabelVector communication,
            boolean[] vectorElementsFound,
            State currentState/*
                               * ,
                               * Map<String, State> memoization
                               */) {

        /* Base case */

        // We have found the destination if all vector elements have been found
        boolean finished = true;
        for (int i = 0; i < communication.getSize(); i++)
            if (!communication.getLabelAtIndex(i).equals(Event.EPSILON) && !vectorElementsFound[i]) {
                finished = false;
                break;
            }

        if (finished)
            return currentState;

        /*
         * Memoization
         * 
         * // NOTE: Memoziation is commented out since it is extremely memory intensive,
         * and should not be used in the general case
         * 
         * String key = encodeString(communication, vectorElementsFound, currentState);
         * if (memoization.containsKey(key))
         * return memoization.get(key);
         * 
         */

        /* Recursive case */

        // Try all transitions leading from this state
        outer: for (Transition t : currentState.getTransitions()) {

            boolean[] copy = ArrayUtils.clone(vectorElementsFound);

            // Check to see if the event vector of this transition is compatible with what
            // we've found so far
            for (int i = 0; i < t.getEvent().getVector().getSize(); i++) {

                String element = t.getEvent().getVector().getLabelAtIndex(i);

                if (!element.equals(Event.EPSILON)) {

                    // Conflict since we have already found an element for this index (so they
                    // aren't compatible)
                    if (copy[i])
                        continue outer;

                    // Is compatible
                    else if (element.equals(communication.getLabelAtIndex(i)))
                        copy[i] = true;

                    // Conflict since the elements do not match (meaning they aren't compatible)
                    else
                        continue outer;
                }

            }

            // Recursive call to the state where this transition leads
            State destinationState = findWhereCommunicationLeads(uStructure, communication, copy,
                    uStructure.getState(t.getTargetStateID())/* , memoization */);

            // Return destination if it is found (there will only ever be one destination
            // for a given communication from a given state, so we can stop as soon as we
            // find it the first time)
            if (destinationState != null) {
                // memoization.put(key, destinationState); // Save the answer (NOTE: Saving the
                // dead-ends is more important than saving the answers)
                return destinationState;
            }

        }

        // memoization.put(key, null); // Indicate that this is a dead-end
        return null;

    }

    /**
     * Given the complete set of least upper bounds (LUBs), return the subset of
     * LUBs which are the event vectors for potential communications.
     * 
     * @param leastUpperBounds The set of LUBs
     * @return The set of potential communications, including communication roles
     **/
    private static Set<CommunicationLabelVector> findPotentialCommunicationLabels(Set<LabelVector> leastUpperBounds) {

        /* Separate observable and unobservable labels */

        Set<LabelVector> observableLabels = new HashSet<LabelVector>();
        Set<LabelVector> unobservableLabels = new HashSet<LabelVector>();

        for (LabelVector v : leastUpperBounds) {
            if (v.getLabelAtIndex(0).equals(Event.EPSILON))
                unobservableLabels.add(v);
            else
                observableLabels.add(v);
        }

        // Find all LUB's of the unobservable labels (which will add communications
        // where there is more than one receiver)
        generateLeastUpperBounds(unobservableLabels);

        /* Find potential communications */

        Set<CommunicationLabelVector> potentialCommunications = new HashSet<CommunicationLabelVector>();

        for (LabelVector v1 : observableLabels) {
            for (LabelVector v2 : unobservableLabels) {

                /* Error checking */

                if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
                    logger.error("Bad event vectors. Least upper bounds generation aborted.");
                    return null;
                }

                /* Setup */

                CommunicationRole[] roles = new CommunicationRole[v1.getSize() - 1];

                /* Build least upper bound */

                boolean valid = true;
                StringBuilder potentialCommunicationBuilder = new StringBuilder();
                String eventLabel = null;

                for (int i = 0; i < v1.getSize(); i++) {

                    String label1 = v1.getLabelAtIndex(i);
                    String label2 = v2.getLabelAtIndex(i);

                    // Check to see if they are incompatible or if this potential communication has
                    // already been taken care of
                    if (!label1.equals(Event.EPSILON) && !label2.equals(Event.EPSILON)) {
                        valid = false;
                        break;
                    }

                    // Append vector element
                    String newEventLabel = null;
                    if (!label1.equals(Event.EPSILON)) {
                        potentialCommunicationBuilder.append("," + label1);
                        newEventLabel = label1;
                        if (i > 0)
                            roles[i - 1] = CommunicationRole.SENDER;
                    } else if (!label2.equals(Event.EPSILON)) {
                        potentialCommunicationBuilder.append("," + label2);
                        newEventLabel = label2;
                        if (i > 0)
                            roles[i - 1] = CommunicationRole.RECEIVER;
                    } else {
                        potentialCommunicationBuilder.append(",*");
                        if (i > 0)
                            roles[i - 1] = CommunicationRole.NONE;
                    }

                    // Make sure that the senders and receivers all are working with the same event
                    if (eventLabel != null && newEventLabel != null && !newEventLabel.equals(eventLabel)) {
                        valid = false;
                        break;
                    }

                    if (eventLabel == null)
                        eventLabel = newEventLabel;

                }

                /* Add it to the set */

                if (valid) {

                    // Add all potential communications (1 for each sender)
                    for (int i = 0; i < roles.length; i++) {

                        if (roles[i] == CommunicationRole.SENDER) {

                            CommunicationRole[] copy = ArrayUtils.clone(roles);

                            // Remove all other senders
                            for (int j = 0; j < copy.length; j++)
                                if (j != i && copy[j] == CommunicationRole.SENDER)
                                    copy[j] = CommunicationRole.NONE;

                            // Add potential communication
                            potentialCommunications.add(new CommunicationLabelVector(
                                    "<" + potentialCommunicationBuilder.substring(1) + ">", copy));

                        }

                    }

                }

            } // for
        } // for

        return potentialCommunications;

    }

    /**
     * Expand the specified set of event vectors to include all possible least upper
     * bounds (LUBs).
     * 
     * @param leastUpperBounds The set of all LUBs in the form of event vectors
     **/
    private static void generateLeastUpperBounds(Set<LabelVector> leastUpperBounds) {

        /*
         * Continue to find LUBs using pairs of event vectors until there are no new
         * ones left to find
         */

        boolean foundNew = true;
        while (foundNew) {

            List<LabelVector> temporaryList = new ArrayList<LabelVector>();

            // Try all pairs
            for (LabelVector v1 : leastUpperBounds) {
                for (LabelVector v2 : leastUpperBounds) {

                    /* Error checking */

                    if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
                        logger.error("Bad event vectors. Pair of label vectors skipped.");
                        continue;
                    }

                    /* Build least upper bound */

                    boolean valid = true;
                    StringBuilder leastUpperBoundBuilder = new StringBuilder();
                    for (int i = 0; i < v1.getSize(); i++) {

                        String label1 = v1.getLabelAtIndex(i);
                        String label2 = v2.getLabelAtIndex(i);

                        // Check for incompatibility
                        if (!label1.equals(Event.EPSILON) && !label2.equals(Event.EPSILON) && !label1.equals(label2)) {
                            valid = false;
                            break;
                        }

                        // Append vector element
                        if (label1.equals(Event.EPSILON))
                            leastUpperBoundBuilder.append("," + label2);
                        else
                            leastUpperBoundBuilder.append("," + label1);

                    }

                    /* Add to the temporary list */

                    if (valid)
                        temporaryList.add(new LabelVector("<" + leastUpperBoundBuilder.substring(1) + ">"));

                } // for
            } // for

            // Add all of the vectors from the temporary list
            foundNew = false;
            for (LabelVector v : temporaryList)
                if (leastUpperBounds.add(v))
                    foundNew = true;

        }

    }

    /**
     * Duplicates the specified U-Structure as a pruned U-Structure.
     * 
     * @return The duplicated U-Structure (as a pruned U-Structure)
     **/
    public static PrunedUStructure duplicateAsPrunedUStructure(UStructure orig) {

        JsonObject jsonObj = orig.toJsonObject();
        jsonObj.remove("type");
        jsonObj.addProperty("type", Automaton.Type.PRUNED_U_STRUCTURE.getNumericValue());
        return new PrunedUStructure(jsonObj);
    }

    /**
     * Refine the U-Structure by applying the specified communication protocol, and
     * doing the necessary pruning.
     * 
     * @param <T>                         The type of communication data
     * @param protocol                    The chosen protocol
     * @param discardUnusedCommunications Whether or not the unused communications
     *                                    should be discarded
     * @return the pruned U-Structure that had the specified protocol applied
     **/
    public static <T extends CommunicationData> PrunedUStructure applyProtocol(UStructure uStructure, Set<T> protocol,
            boolean discardUnusedCommunications) {

        PrunedUStructure prunedUStructure = duplicateAsPrunedUStructure(uStructure);

        /* Remove all communications that are not part of the protocol */

        if (discardUnusedCommunications) {

            for (TransitionData data : uStructure.getInvalidCommunications())
                prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

            for (CommunicationData data : uStructure.getPotentialAndNashCommunications())
                if (!protocol.contains(data))
                    prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

        }

        /* Prune (which removes more transitions) */

        for (CommunicationData data : protocol)
            prunedUStructure.prune(protocol, uStructure.getEvent(data.eventID).getVector(), data.initialStateID,
                    data.getIndexOfSender() + 1);

        /* Get the accessible part of the U-Structure */

        prunedUStructure = prunedUStructure.accessible();

        /* Remove all inactive events */

        prunedUStructure.removeInactiveEvents();

        return prunedUStructure;

    }

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
        combinedStateQueue
                .add(Triple.of(subsetConstruction.initialState, new Sequence(subsetConstruction.initialState), true));

        UStructure relabeled = new UStructure(uStructure.getNumberOfControllers());
        relabeled.addAllEvents(uStructure.getEvents());

        while (!combinedStateQueue.isEmpty()) {
            Triple<Long, Sequence, Boolean> currSequence = combinedStateQueue.remove();

            StateSet ss = subsetConstruction.getStateAsStateSet(currSequence.getLeft());
            Map<Long, Long> currStateSetIDMap = currSequence.getRight() ? new LinkedHashMap<>()
                    : relabelMapping.get(currSequence.getLeft());
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
                            modID, false, s.getEnablementEvents(), s.getDisablementEvents(),
                            s.getIllegalConfigEvents());
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
    public static double findShapleyValueForController(UStructure uStructure, Map<Set<Integer>, Integer> shapleyValues,
            int indexOfController) {

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
