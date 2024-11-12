/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.*;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.util.PowerSetUtils;
import com.github.automaton.io.json.JsonUtils;
import com.google.gson.*;

/**
 * Represents an un-pruned U-Structure.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 * @revised 2.0
 */
public class UStructure extends Automaton {

    /* CLASS CONSTANTS */

    /**
     * Whether or not invalid communications should be added for mathematical
     * completeness, or suppressed for efficiency purposes
     */
    public static boolean SUPPRESS_INVALID_COMMUNICATIONS = true;

    private static Logger logger = LogManager.getLogger();

    /* INSTANCE VARIABLES */

    // Special transitions
    protected List<TransitionData> unconditionalViolations;
    protected List<TransitionData> conditionalViolations;
    protected List<CommunicationData> potentialCommunications;
    protected List<TransitionData> invalidCommunications;
    protected List<NashCommunicationData> nashCommunications;
    protected List<DisablementData> disablementDecisions;

    /* CONSTRUCTORS */

    /**
     * Constructs a new {@code Automaton} with the specified number of controllers.
     * 
     * @param nControllers the number of controllers that the new automaton has (1
     *                     implies centralized control, >1 implies decentralized
     *                     control)
     * @throws IllegalArgumentException if argument is not positive
     * 
     * @since 2.0
     */
    public UStructure(int nControllers) {
        super(nControllers);
    }

    /**
     * Constructs a new {@code UStructure} that is represented by a JSON object
     * 
     * @param jsonObject a JSON object that represents a U-Structure
     * 
     * @see Automaton#buildAutomaton(JsonObject)
     * @since 2.0
     **/
    UStructure(JsonObject jsonObject) {

        super(jsonObject);

    }

    @Override
    protected void initializeLists() {

        super.initializeLists();

        unconditionalViolations = new ArrayList<TransitionData>();
        conditionalViolations = new ArrayList<TransitionData>();
        potentialCommunications = new ArrayList<CommunicationData>();
        invalidCommunications = new ArrayList<TransitionData>();
        nashCommunications = new ArrayList<NashCommunicationData>();
        disablementDecisions = new ArrayList<DisablementData>();

    }

    /* AUTOMATA OPERATIONS */

    @Override
    public UStructure accessible() {
        return AutomataOperations.accessible(this, UStructure::new);
    }

    @Override
    public UStructure coaccessible() {
        return AutomataOperations.coaccessible(this, UStructure::new);
    }

    @Override
    public UStructure complement() {

        return AutomataOperations.complement(this, UStructure::new);

    }

    /**
     * @since 2.1.0
     */
    @Override
    public UStructure trim() {
        return accessible().coaccessible();
    }

    @Override
    public UStructure invert() {
        return AutomataOperations.invert(this, UStructure::new);
    }

    /**
     * Generate a new U-Structure, with all communications added (potential
     * communications are marked).
     * 
     * @return The U-Structure with the added transitions
     * 
     * @since 2.0
     **/
    public UStructure addCommunications() {

        /* Setup */

        // Create a mapping between the event labels and their associated properties in
        // the original automaton
        // NOTE: The controller's index (1-based, in this case) is appended to the
        // event's label since each
        // controller has different properties for each event
        Map<String, BitSet> observableMapping = new HashMap<>();
        Map<String, BitSet> controllableMapping = new HashMap<>();
        for (Event e : events) {
            LabelVector vector = e.getVector();
            for (int i = 1; i < vector.getSize(); i++) {
                String label = vector.getLabelAtIndex(i);
                if (!observableMapping.containsKey(label)) {
                    observableMapping.put(label, new BitSet(vector.getSize() - 1));
                }
                if (!controllableMapping.containsKey(label)) {
                    controllableMapping.put(label, new BitSet(vector.getSize() - 1));
                }
                observableMapping.get(label).set(i - 1, e.isObservable(i - 1));
                controllableMapping.get(label).set(i - 1, e.isControllable(i - 1));
            }
        }

        // Generate all potential communication labels
        Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
        for (Event e : events)
            leastUpperBounds.add(e.getVector());
        Set<CommunicationLabelVector> potentialCommunications = findPotentialCommunicationLabels(leastUpperBounds);

        // Generate all least upper bounds (if invalid communications are not being
        // suppressed)
        if (SUPPRESS_INVALID_COMMUNICATIONS) {
            leastUpperBounds.addAll(potentialCommunications);
        } else
            generateLeastUpperBounds(leastUpperBounds);

        UStructure uStructure = this.clone();

        /* Add communications (marking the potential communications) */

        // Map<String, State> memoization = new HashMap<String, State>();
        for (long s = 1; s <= uStructure.getNumberOfStates(); s++) {

            State startingState = uStructure.getState(s);

            // Try each least upper bound
            for (LabelVector vector : leastUpperBounds) {

                boolean[] vectorElementsFound = new boolean[vector.getSize()];
                State destinationState = uStructure.findWhereCommunicationLeads(vector, vectorElementsFound,
                        startingState/* , memoization */);

                if (destinationState != null) {

                    // Add event if it doesn't already exist
                    int id;
                    Event event = uStructure.getEvent(vector);
                    if (event == null) {

                        // Determine observable and controllable properties of the event vector
                        boolean[] observable = new boolean[nControllers];
                        boolean[] controllable = new boolean[nControllers];
                        for (int i = 1; i < vector.getSize(); i++) {
                            String label = vector.getLabelAtIndex(i);
                            if (!label.equals(Event.EPSILON)) {
                                observable[i - 1] = !observableMapping.get(label).isEmpty();
                                controllable[i - 1] = controllableMapping.get(label).get(i - 1);
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
                            if (SUPPRESS_INVALID_COMMUNICATIONS)
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
     * Checking the feasibility for all possible communication protocols, generate a
     * list of the feasible protocols.
     * 
     * @param <T>                         The type of communication data
     * @param communications              The communications to be considered
     *                                    <p>
     *                                    NOTE: These should be a subset of the
     *                                    {@link #potentialCommunications} list of
     *                                    this U-Structure
     * @param mustAlsoSolveControlProblem Whether or not the generated protocols
     *                                    must also solve the control problem
     * @return The feasible protocols, sorted smallest to largest
     **/
    public <T extends CommunicationData> List<Set<T>> generateAllFeasibleProtocols(List<T> communications,
            boolean mustAlsoSolveControlProblem) {

        /* Generate powerset of communication protocols */

        List<Set<T>> protocols = PowerSetUtils.powerSet(communications);

        /* Generate list of feasible protocols */

        List<Set<T>> feasibleProtocols = new ArrayList<Set<T>>();
        for (Set<T> protocol : protocols) {

            // Ignore the protocol with no communications (doesn't make sense in our
            // context)
            if (protocol.size() == 0)
                continue;

            if (isFeasibleProtocol(new HashSet<CommunicationData>(protocol), mustAlsoSolveControlProblem))
                feasibleProtocols.add(protocol);

        }

        /*
         * Sort sets by size (so that protocols with fewer communications appear first)
         */

        Collections.sort(feasibleProtocols, (set1, set2) -> Integer.compare(set1.size(), set2.size()));

        return feasibleProtocols;

    }

    /**
     * Generate a list of the smallest possible feasible protocols (in terms of the
     * number of communications).
     * 
     * @param communications The communications to be considered (which should be a
     *                       subset of the {@link #potentialCommunications} list of
     *                       this U-Structure)
     * @return The feasible protocols
     **/
    public List<Set<CommunicationData>> generateSmallestFeasibleProtocols(List<CommunicationData> communications) {

        /* Generate powerset of communication protocols */

        List<Set<CommunicationData>> protocols = PowerSetUtils.powerSet(communications);

        /*
         * Sort sets by size (so that protocols with fewer communications appear first)
         */

        Collections.sort(protocols, (set1, set2) -> Integer.compare(set1.size(), set2.size()));

        /* Generate list of feasible protocols */

        List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
        int minFeasibleSize = Integer.MAX_VALUE;
        for (Set<CommunicationData> protocol : protocols) {

            // We only want the smallest feasible protocols
            if (protocol.size() > minFeasibleSize)
                break;

            // Ignore the protocol with no communications (doesn't make sense in our
            // context)
            if (protocol.size() == 0)
                continue;

            // Add the protocol to the list if it is feasible
            if (isFeasibleProtocol(protocol, false)) {
                feasibleProtocols.add(protocol);
                minFeasibleSize = protocol.size();
            }

        }

        return feasibleProtocols;

    }

    /**
     * Greedily generate a feasible protocol (optimality is not guaranteed).
     * 
     * @param communications The communications to be considered (which should be a
     *                       subset of the
     *                       {@link #potentialCommunications}/{@link #nashCommunications}
     *                       lists of this U-Structure)
     * @return The feasible protocol
     **/
    public Set<CommunicationData> generateFeasibleProtocol(List<CommunicationData> communications) {

        Set<CommunicationData> protocol = new HashSet<CommunicationData>();

        UStructure uStructure = this;

        // Continue until no more violations exist
        while (uStructure.unconditionalViolations.size() > 0 || uStructure.conditionalViolations.size() > 0) {

            // System.out.println(uStructure.unconditionalViolations.size() + " " +
            // uStructure.conditionalViolations.size() + " " +
            // uStructure.getNumberOfStates());

            // Choose an arbitrary violation
            TransitionData chosenViolation = (uStructure.conditionalViolations.size() > 0
                    ? uStructure.conditionalViolations.get(0)
                    : uStructure.unconditionalViolations.get(0));
            // System.out.println(chosenViolation.toString(uStructure));
            // System.out.println(uStructure.getState(chosenViolation.initialStateID));

            // Determine a communication which is necessary in order to help prevent this
            // violation
            // NOTE: It is possible that more than one communication will be necessary, but
            // this will
            // be taken care of in subsequent iterations
            CommunicationData associatedCommunication = uStructure.findCommunicationToBeAdded(chosenViolation, this,
                    protocol);

            if (protocol.contains(associatedCommunication)) {
                logger.error("ERROR : There was an infinite loop detected.");
                break;
            }
            protocol.addAll(addCommunicationsToEnsureFeasibility(associatedCommunication));

            // System.out.println("communications added. protocol size is now: " +
            // protocol.size());

            // Apply the protocol, pruning as necessary
            uStructure = applyProtocol(protocol, false);

            // System.out.println("protocol applied. number of states is now: " +
            // uStructure.getNumberOfStates());
            // System.out.println("actual number of communications left in u-structure: " +
            // uStructure.getSizeOfPotentialAndNashCommunications());

        }

        // System.out.println("finished!");

        return protocol;

    }

    /**
     * Find all feasible protocols which contain each communication in the requested
     * protocol.
     * 
     * @param requestedProtocol The protocol that is being made feasible
     * @return All feasible protocols
     **/
    public List<Set<CommunicationData>> makeProtocolFeasible(Set<CommunicationData> requestedProtocol) {

        /* Generate powerset of communication protocols */

        List<Set<CommunicationData>> protocols = PowerSetUtils.powerSetSubset(getPotentialAndNashCommunications(), requestedProtocol);

        /* Generate list of feasible protocols */

        List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
        for (Set<CommunicationData> protocol : protocols) {

            // Ignore the protocol with no communications (doesn't make sense in our
            // context)
            if (protocol.size() == 0)
                continue;

            if (isFeasibleProtocol(protocol, false))
                feasibleProtocols.add(protocol);

        }

        /*
         * Sort sets by size (so that protocols with fewer communications appear first)
         */

        Collections.sort(feasibleProtocols, (set1, set2) -> Integer.compare(set1.size(), set2.size()));

        return feasibleProtocols;

    }

    /**
     * Refine this U-Structure by applying the specified communication protocol, and
     * doing the necessary pruning.
     * 
     * @param <T>                         The type of communication data
     * @param protocol                    The chosen protocol
     * @param discardUnusedCommunications Whether or not the unused communications
     *                                    should be discarded
     * @return This pruned U-Structure that had the specified protocol applied
     * 
     * @since 2.0
     **/
    public <T extends CommunicationData> PrunedUStructure applyProtocol(Set<T> protocol,
            boolean discardUnusedCommunications) {

        PrunedUStructure prunedUStructure = duplicateAsPrunedUStructure();

        /* Remove all communications that are not part of the protocol */

        if (discardUnusedCommunications) {

            for (TransitionData data : invalidCommunications)
                prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

            for (CommunicationData data : getPotentialAndNashCommunications())
                if (!protocol.contains(data))
                    prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

        }

        /* Prune (which removes more transitions) */

        for (CommunicationData data : protocol)
            prunedUStructure.prune(protocol, getEvent(data.eventID).getVector(), data.initialStateID,
                    data.getIndexOfSender() + 1);

        /* Get the accessible part of the U-Structure */

        prunedUStructure = prunedUStructure.accessible();

        /* Remove all inactive events */

        prunedUStructure.removeInactiveEvents();

        return prunedUStructure;

    }

    /**
     * Duplicate this U-Structure as a pruned U-Structure.
     * 
     * @return The duplicated U-Structure (as a pruned U-Structure)
     * 
     * @since 2.0
     **/
    public PrunedUStructure duplicateAsPrunedUStructure() {

        JsonObject jsonObj = toJsonObject();
        jsonObj.remove("type");
        jsonObj.addProperty("type", Type.PRUNED_U_STRUCTURE.getNumericValue());
        return new PrunedUStructure(jsonObj);
    }

    /**
     * Runs subset construction on this U-Structure.
     * 
     * @return subset construction of this U-structure w.r.t. controller 0
     * @since 2.0
     */
    public SubsetConstruction subsetConstruction() {
        return subsetConstruction(0);
    }

    /**
     * Runs subset construction on this U-Structure.
     * 
     * @param controller The controller to perform subset construction with
     * 
     * @return subset construction of this U-structure w.r.t. the specified
     *         controller
     * @throws IndexOutOfBoundsException if {@code controller} is negative or
     *                                   {@code controller}
     *                                   is greater than {@link #nControllers}
     * 
     * @since 2.0
     */
    public SubsetConstruction subsetConstruction(int controller) {
        return new SubsetConstruction(this, controller);
    }

    /**
     * Creates a copy of this U-Structure that has copies of same state(s)
     * if the state appears in more than one projections.
     * 
     * @return a copy of this U-Structure with relabeled states
     * 
     * @since 2.0
     */
    public UStructure relabelConfigurationStates() {

        SubsetConstruction subsetConstruction = this.subsetConstruction();

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

        UStructure relabeled = new UStructure(nControllers);
        relabeled.addAllEvents(this.events);

        while (!combinedStateQueue.isEmpty()) {
            Triple<Long, Sequence, Boolean> currSequence = combinedStateQueue.remove();

            StateSet ss = subsetConstruction.getStateAsStateSet(currSequence.getLeft());
            Map<Long, Long> currStateSetIDMap = currSequence.getRight() ? new LinkedHashMap<>() : relabelMapping.get(currSequence.getLeft());
            if (currSequence.getRight()) {
                relabelMapping.put(ss.getID(), currStateSetIDMap);
                /* Calculate new state IDs for relabeling */
                for (State s : ss.getSet()) {
                    long origID = s.getID();
                    long modID = origID + nStates * stateIDMultiSet.getCount(origID);
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
        copyOverSpecialTransitions(relabeled);
        for (TransitionData unconditionalTd : unconditionalViolations) {
            long initStateID = unconditionalTd.initialStateID;
            long targetStateID = unconditionalTd.targetStateID;
            for (int i = 1; i < stateIDMultiSet.getCount(unconditionalTd.initialStateID); i++) {
                long relabeledInitStateID = initStateID + nStates * i;
                for (int j = 1; j < stateIDMultiSet.getCount(targetStateID); j++) {
                    long relabeledTargetStateID = targetStateID + nStates * j;
                    if (relabeled.transitionExists(relabeledInitStateID, unconditionalTd.eventID,
                            relabeledTargetStateID)) {
                        relabeled.addUnconditionalViolation(relabeledInitStateID, unconditionalTd.eventID,
                                relabeledTargetStateID);
                    }
                }
            }
        }
        for (TransitionData conditionalTd : conditionalViolations) {
            long initStateID = conditionalTd.initialStateID;
            long targetStateID = conditionalTd.targetStateID;
            for (int i = 1; i < stateIDMultiSet.getCount(conditionalTd.initialStateID); i++) {
                long relabeledInitStateID = initStateID + nStates * i;
                for (int j = 1; j < stateIDMultiSet.getCount(targetStateID); j++) {
                    long relabeledTargetStateID = targetStateID + nStates * j;
                    if (relabeled.transitionExists(relabeledInitStateID, conditionalTd.eventID,
                            relabeledTargetStateID)) {
                        relabeled.addConditionalViolation(relabeledInitStateID, conditionalTd.eventID,
                                relabeledTargetStateID);
                    }
                }
            }
        }

        relabeled.setInitialStateID(this.initialState);

        relabeled.renumberStates();
        return relabeled;
    }

    /**
     * Given a vector of state labels, returns a list of states that
     * correspond to the vector elements.
     * 
     * @param lv a label vector
     * @return a list of states
     * 
     * @throws NullPointerException if argument is {@code null}
     * 
     * @since 2.0
     */
    List<State> getStatesFromLabel(LabelVector lv) {
        List<State> states = new ArrayList<>();
        for (String label : Objects.requireNonNull(lv)) {
            states.add(getState(label));
        }
        return states;
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
    public double findShapleyValueForController(Map<Set<Integer>, Integer> shapleyValues, int indexOfController) {

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
                    * factorial(getNumberOfControllers() - coalition.size() - 1)
                    * (shapleyValueWithController - shapleyValueWithoutController);

        }

        return (double) sum / (double) factorial(getNumberOfControllers());

    }

    /* AUTOMATA OPERATION HELPER METHODS */

    /**
     * Determine a communication to be added in order to help prevent the specified
     * violation.
     * 
     * @param violation           The violation that we are trying to avoid
     * @param originalUStructure  The original U-Structure
     * @param preExistingProtocol The protocol that we have currently found so far
     *                            for the original U-Structure
     * @return The communication which should be added ({@code null} if nothing was
     *         found, which should not happen)
     **/
    private CommunicationData findCommunicationToBeAdded(TransitionData violation, UStructure originalUStructure,
            Set<CommunicationData> preExistingProtocol) {

        /* Setup */

        UStructure inverted = invert();
        Set<Long> visitedStates = new HashSet<Long>();
        Queue<Long> stateQueue = new LinkedList<Long>();
        Queue<Integer> eventQueue = new LinkedList<Integer>();
        stateQueue.offer(violation.initialStateID);
        eventQueue.offer(violation.eventID);

        /*
         * Do a breadth-first search until we find a communication which can be added
         * that will prevent the violation
         */

        while (stateQueue.size() > 0) {

            long stateID = stateQueue.poll();
            int eventID = eventQueue.poll();

            // Check to see if we've found a communication which can prevent the specified
            // violation
            for (CommunicationData communication : getPotentialAndNashCommunications())
                if (communication.initialStateID == stateID)
                    if (LabelVector.isStrictSubVector(getEvent(eventID).getVector(), getEvent(communication.eventID).getVector())) {

                        // Find the associated communication in the original U-Structure (since the IDs
                        // may no longer match after a protocol is applied)
                        String initialStateLabel = getState(communication.initialStateID).getLabel();
                        String eventLabel = getEvent(communication.eventID).getLabel();
                        String targetStateLabel = getState(communication.targetStateID).getLabel();
                        CommunicationData associatedCommunication = new CommunicationData(
                                originalUStructure.getStateID(initialStateLabel),
                                originalUStructure.getEvent(eventLabel).getID(),
                                originalUStructure.getStateID(targetStateLabel),
                                communication.roles);

                        // Only return it, if it is not already in the protocol
                        if (!preExistingProtocol.contains(associatedCommunication))
                            return associatedCommunication;
                    }

            // State has already been visited
            if (visitedStates.contains(stateID))
                continue;

            visitedStates.add(stateID);

            // Traverse each transition backwards
            State currentState = inverted.getState(stateID);
            for (Transition transition : currentState.getTransitions()) {
                stateQueue.offer(transition.getTargetStateID());
                eventQueue.offer(transition.getEvent().getID());
            }

        }

        logger.error("Could not locate communication to be removed.");

        return null;

    }

    /**
     * Find the communications needed in order to ensure that adding the specified
     * communication is feasible.
     * 
     * @param initialCommunication The communication
     * @return The feasible protocol
     **/
    private Set<CommunicationData> addCommunicationsToEnsureFeasibility(CommunicationData initialCommunication) {

        Set<CommunicationData> feasibleProtocol = new HashSet<CommunicationData>();
        UStructure inverted = invert();

        // Group the communications so that they are accessible by state ID
        // NOTE: We do this to reduce the time complexity of this method
        Map<Long, List<CommunicationData>> map = new HashMap<Long, List<CommunicationData>>();
        for (CommunicationData communication : getPotentialAndNashCommunications()) {
            List<CommunicationData> list = map.get(communication.initialStateID);
            if (list == null) {
                list = new ArrayList<CommunicationData>();
                list.add(communication);
                map.put(communication.initialStateID, list);
            } else {
                list.add(communication);
            }

        }

        // Find indistinguishable states
        Set<Long> reachableStates = new HashSet<Long>();
        findReachableStates(this, inverted, reachableStates, initialCommunication.initialStateID,
                initialCommunication.getIndexOfSender() + 1);

        // Add indistinguishable communications
        for (Long stateID : reachableStates) {
            List<CommunicationData> communications = map.get(stateID);
            if (communications != null)
                for (Transition transition : getState(stateID).getTransitions())
                    for (CommunicationData data : communications)
                        if (data.eventID == transition.getEvent().getID()
                                && Arrays.deepEquals(initialCommunication.roles, data.roles))
                            feasibleProtocol.add(data);
        }

        return feasibleProtocol;

    }

    /**
     * Recursively find the factorial of the specified number.
     * 
     * @param n The number to take the factorial of, must be in the range [0,12]
     * @return The factorial value
     **/
    private static int factorial(int n) {

        // Error checking
        if (n < 0 || n > 12) {
            logger.error("Factorial value of " + n + " is outside allowed range.");
            return -1;
        }

        // Base case
        if (n == 0)
            return 1;

        // Recursive case
        return n * factorial(n - 1);

    }

    @Override
    protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

        UStructure uStructure = (UStructure) automaton;

        for (TransitionData data : unconditionalViolations)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

        for (TransitionData data : conditionalViolations)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

        for (CommunicationData data : potentialCommunications)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID,
                        ArrayUtils.clone(data.roles));

        for (TransitionData data : invalidCommunications)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addInvalidCommunication(data.initialStateID, data.eventID, data.targetStateID);

        for (NashCommunicationData data : nashCommunications)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addNashCommunication(data.initialStateID, data.eventID, data.targetStateID,
                        ArrayUtils.clone(data.roles), data.cost, data.probability);

        for (DisablementData data : disablementDecisions)
            if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
                uStructure.addDisablementDecision(data.initialStateID, data.eventID, data.targetStateID,
                        ArrayUtils.clone(data.controllers));

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
    private State findWhereCommunicationLeads(LabelVector communication,
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
            State destinationState = findWhereCommunicationLeads(communication, copy,
                    getState(t.getTargetStateID())/* , memoization */);

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
     * Encode the current method's state in order to use it as a key in a hash map.
     * <p>
     * NOTE: This makes the assumption that commas don't appear in event labels
     * (which {@link com.github.automaton.gui.JDec JDec} prevents)
     * 
     * @param communication       The event vector representing the communication
     * @param vectorElementsFound Indicates which elements of the vector have been
     *                            found
     * @param currentState        The state that we are currently on
     * @return The resulting string
     **/
    private String encodeString(LabelVector communication,
            boolean[] vectorElementsFound,
            State currentState) {

        StringBuilder missingVectorElements = new StringBuilder();
        for (int i = 0; i < communication.getSize(); i++) {
            String element = communication.getLabelAtIndex(i);
            if (!vectorElementsFound[i]) {
                missingVectorElements.append("," + element);
            } else {
                missingVectorElements.append(",*");
            }
        }

        return currentState.getID() + missingVectorElements.toString();

    }

    /**
     * Given the complete set of least upper bounds (LUBs), return the subset of
     * LUBs which are the event vectors for potential communications.
     * 
     * @param leastUpperBounds The set of LUBs
     * @return The set of potential communications, including communication roles
     **/
    private Set<CommunicationLabelVector> findPotentialCommunicationLabels(Set<LabelVector> leastUpperBounds) {

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
                List<String> potentialCommunicationLabels = new ArrayList<>();
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
                        potentialCommunicationLabels.add(label1);
                        newEventLabel = label1;
                        if (i > 0)
                            roles[i - 1] = CommunicationRole.SENDER;
                    } else if (!label2.equals(Event.EPSILON)) {
                        potentialCommunicationLabels.add(label2);
                        newEventLabel = label2;
                        if (i > 0)
                            roles[i - 1] = CommunicationRole.RECEIVER;
                    } else {
                        potentialCommunicationLabels.add(Event.EPSILON);
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
                                    potentialCommunicationLabels, copy));

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
                    List<String> leastUpperBoundList = new ArrayList<>();
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
                            leastUpperBoundList.add(label2);
                        else
                            leastUpperBoundList.add(label1);

                    }

                    /* Add to the temporary list */

                    if (valid)
                        temporaryList.add(new LabelVector(leastUpperBoundList));

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
     * Check to see if the specified protocol is feasible.
     * <p>
     * NOTE: This method works under the assumption that the protocol has at least
     * one communication.
     * 
     * @param protocol                    The protocol that is being checked for
     *                                    feasibility
     * @param mustAlsoSolveControlProblem Whether or not the protocol must solve the
     *                                    control problem (meaning
     *                                    there are no violations after pruning)
     * @return Whether or not the protocol is feasible
     **/
    private boolean isFeasibleProtocol(Set<CommunicationData> protocol, boolean mustAlsoSolveControlProblem) {

        UStructure copy = this.clone();
        copy = copy.applyProtocol(protocol, true);

        // If it must also solve the control problem, but there are still violations,
        // then return false
        if (mustAlsoSolveControlProblem)
            if (copy.conditionalViolations.size() > 0 || copy.unconditionalViolations.size() > 0)
                return false;

        // If there was a change in the number of communications after pruning, then it
        // is clearly not feasible
        if (copy.getSizeOfPotentialAndNashCommunications() != protocol.size())
            return false;

        UStructure invertedUStructure = copy.invert();

        for (CommunicationData data : copy.getPotentialAndNashCommunications()) {

            // Find indistinguishable states
            Set<Long> reachableStates = new HashSet<Long>();
            findReachableStates(copy, invertedUStructure, reachableStates, data.initialStateID,
                    data.getIndexOfSender() + 1);

            // Any strict subset of this communication's event vector which is found at an
            // indistinguishable states
            // indicates that there used to be a communication here (before the protocol was
            // applied), but that
            // it should have been part of the protocol, meaning this protocol is not
            // feasible
            LabelVector eventVector = copy.getEvent(data.eventID).getVector();
            for (Long s : reachableStates)
                for (Transition t : copy.getState(s).getTransitions())
                    if (LabelVector.isStrictSubVector(t.getEvent().getVector(), eventVector))
                        return false;

        }

        return true;

    }

    /**
     * Using recursion, determine which states are reachable through transitions
     * which are unobservable to the sender.
     * 
     * @param uStructure          The relevant U-Structure
     * @param invertedUStructure  A U-Structure identical to the previous (except
     *                            all transitions are going the opposite direction)
     *                            <p>
     *                            NOTE: There is no need for extra information (such
     *                            as special transitions) to be in the inverted
     *                            automaton
     * @param reachableStates     The set of reachable states that are being built
     *                            during this recursive process
     * @param currentStateID      The current state
     * @param vectorIndexOfSender The index in the event vector which corresponds to
     *                            the sending controller
     **/
    private static void findReachableStates(UStructure uStructure,
            UStructure invertedUStructure,
            Set<Long> reachableStates,
            long currentStateID,
            int vectorIndexOfSender) {

        // Base case
        if (reachableStates.contains(currentStateID))
            return;

        reachableStates.add(currentStateID);

        for (Transition t : uStructure.getState(currentStateID).getTransitions())
            if (t.getEvent().getVector().isUnobservableToController(vectorIndexOfSender)
                    && !reachableStates.contains(t.getTargetStateID()))
                findReachableStates(uStructure, invertedUStructure, reachableStates, t.getTargetStateID(),
                        vectorIndexOfSender);

        for (Transition t : invertedUStructure.getState(currentStateID).getTransitions())
            if (t.getEvent().getVector().isUnobservableToController(vectorIndexOfSender)
                    && !reachableStates.contains(t.getTargetStateID()))
                findReachableStates(uStructure, invertedUStructure, reachableStates, t.getTargetStateID(),
                        vectorIndexOfSender);

    }

    /**
     * Starting at the specified state, find all indistinguishable states with
     * respect to a particular controller.
     * 
     * @param uStructure         The relevant U-Structure
     * @param invertedUStructure The relevant inverted U-Structure
     * @param set                The set of connected states, which will be
     *                           populated by this method
     * @param currentStateID     The current state ID
     * @param indexOfController  The index of the controller
     **/
    protected static void findConnectingStates(UStructure uStructure, UStructure invertedUStructure, Set<Long> set,
            long currentStateID, int indexOfController) {

        /* Base Case */

        if (set.contains(currentStateID))
            return;

        /* Recursive Case */

        set.add(currentStateID);

        State currentState = uStructure.getState(currentStateID);

        // Find all unobservable events leading from this state, and add the target
        // states to the set
        for (Transition t : currentState.getTransitions())
            if (t.getEvent().getVector().isUnobservableToController(indexOfController))
                findConnectingStates(uStructure, invertedUStructure, set, t.getTargetStateID(), indexOfController);

        State currentInvertedState = invertedUStructure.getState(currentStateID);

        // Find all unobservable events leading to this state, and add those states to
        // the set
        for (Transition t : currentInvertedState.getTransitions())
            if (t.getEvent().getVector().isUnobservableToController(indexOfController))
                findConnectingStates(uStructure, invertedUStructure, set, t.getTargetStateID(), indexOfController);

    }

    @Override
    protected void renumberStatesInAllTransitionData(Map<Long, Long> mappingHashMap) {

        renumberStatesInTransitionData(mappingHashMap, unconditionalViolations);
        renumberStatesInTransitionData(mappingHashMap, conditionalViolations);
        renumberStatesInTransitionData(mappingHashMap, potentialCommunications);
        renumberStatesInTransitionData(mappingHashMap, invalidCommunications);
        renumberStatesInTransitionData(mappingHashMap, nashCommunications);
        renumberStatesInTransitionData(mappingHashMap, disablementDecisions);

    }

    /**
     * Find a counter-example, if one exists. The counter-example is returned in the
     * form of a list
     * of sequences of event labels. There will be one sequence for the system, plus
     * one more for each
     * controller that can control the final event.
     * 
     * @param findShortest If {@code true}, the first path to a unconditional
     *                     violation found will be selected as a
     *                     counter-example. If {@code false}, then the shortest
     *                     paths to all unconditional violations
     *                     will be found, and then the longest one will be returned
     * @return The list of sequences of event labels (or {@code null} if there are
     *         no counter-examples)
     * 
     * @deprecated This method is no longer used.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public List<List<String>> findCounterExample(boolean findShortest) {

        if (!hasViolations())
            return null;

        if (nStates + 1 > Integer.MAX_VALUE)
            logger.error("Integer overflow due to too many states.");

        /* Find counter-examples using a breadth-first search */

        // Setup
        Set<List<State>> paths = new HashSet<List<State>>();
        List<State> initialPath = new ArrayList<State>();
        initialPath.add(getState(initialState));
        paths.add(initialPath);
        boolean[] visited = new boolean[(int) (nStates + 1)];
        List<State> longestPath = null;
        TransitionData longestViolation = null;

        // Continue until we have checked all of the paths
        outer: while (paths.size() > 0) {

            Set<List<State>> newPaths = new HashSet<List<State>>();

            for (List<State> path : paths) {

                // Check to see if this path leads to a counter-example
                State lastState = path.get(path.size() - 1);
                TransitionData violation = findUnconditionalViolation(lastState);

                // A counter-example has been found, so let's store it
                if (violation != null) {

                    // Stop early if we only wanted the shortest one
                    if (findShortest)
                        return generateSequences(path, violation);

                    // Store the path if it is the longest path to a violation so far
                    if (longestPath == null || longestPath.size() < path.size()) {
                        longestPath = path;
                        longestViolation = violation;
                    }

                }

                // Add the new paths
                for (Transition t : lastState.getTransitions()) {
                    int targetStateID = (int) t.getTargetStateID();
                    if (!visited[targetStateID]) {
                        List<State> copy = new ArrayList<State>(path);
                        copy.add(getState(targetStateID));
                        visited[targetStateID] = true;
                        newPaths.add(copy);
                    }
                }

            }

            paths = newPaths;

        }

        // Generate the list of sequences for the longest path to a counter-example and
        // return it
        return generateSequences(longestPath, longestViolation);

    }

    /**
     * Generate the list of event label sequences for the counter-example
     * represented by the path of states.
     * 
     * @param path      The path of states leading to the counter-example
     * @param violation The relevant violation
     * @return The list of event label sequences
     * 
     * @deprecated This method is no longer used.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    private List<List<String>> generateSequences(List<State> path, TransitionData violation) {

        /* Build sequence of events and event labels for this counter-example */

        // Setup
        List<Event> eventSequence = new ArrayList<Event>();
        List<String> labelSequence = new ArrayList<String>();
        Iterator<State> iterator = path.iterator();
        State currentState = iterator.next();

        // Build sequence
        while (iterator.hasNext()) {
            State nextState = iterator.next();

            // Find the transition that was followed, and add the event to the sequence
            for (Transition t : currentState.getTransitions()) {
                if (t.getTargetStateID() == nextState.getID()) {
                    eventSequence.add(t.getEvent());
                    String label = t.getEvent().getVector().getLabelAtIndex(0);
                    if (!label.equals(Event.EPSILON))
                        labelSequence.add(label);
                    break;
                }
            }

            currentState = nextState;
        }

        // Add final event
        Event finalEvent = getEvent(violation.eventID);
        String finalEventLabel = finalEvent.getVector().getLabelAtIndex(0);
        if (!finalEventLabel.equals(Event.EPSILON))
            labelSequence.add(finalEventLabel);
        eventSequence.add(finalEvent);

        /* Create event label sequences for each controller as well */

        List<List<String>> sequences = new ArrayList<List<String>>();
        sequences.add(labelSequence);

        for (int i = 0; i < getNumberOfControllers(); i++) {

            // In counter-example notation, we put a dash if the controller cannot control
            // the final event
            if (!finalEvent.isControllable(i))
                continue;

            // Build sequence
            List<String> sequence = new ArrayList<String>();
            for (Event e : eventSequence) {
                String label = e.getVector().getLabelAtIndex(i + 1);
                if (!label.equals(Event.EPSILON))
                    sequence.add(label);
            }

            sequences.add(sequence);

        }

        return sequences;

    }

    @Override
    public UStructure clone() {
        return new UStructure(toJsonObject());
    }

    @Override
    protected void addSpecialTransitionsToJsonObject(JsonObject jsonObj) {

        /* Write special transitions to the JSON object */

        addTransitionDataToJsonObject(jsonObj, "unconditionalViolations", unconditionalViolations);
        addTransitionDataToJsonObject(jsonObj, "conditionalViolations", conditionalViolations);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "potentialCommunications", potentialCommunications,
                CommunicationData.class);
        addTransitionDataToJsonObject(jsonObj, "invalidCommunications", invalidCommunications);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "nashCommunications", nashCommunications,
                NashCommunicationData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "disablementDecisions", disablementDecisions,
                DisablementData.class);

    }

    @Override
    protected void readSpecialTransitionsFromJsonObject(JsonObject jsonObj) {
        unconditionalViolations = readTransitionDataFromJsonObject(jsonObj, "unconditionalViolations");
        conditionalViolations = readTransitionDataFromJsonObject(jsonObj, "conditionalViolations");
        potentialCommunications = JsonUtils.readListPropertyFromJsonObject(jsonObj, "potentialCommunications",
                CommunicationData.class);
        invalidCommunications = readTransitionDataFromJsonObject(jsonObj, "invalidCommunications");
        nashCommunications = JsonUtils.readListPropertyFromJsonObject(jsonObj, "nashCommunications",
                NashCommunicationData.class);
        disablementDecisions = JsonUtils.readListPropertyFromJsonObject(jsonObj, "disablementDecisions",
                DisablementData.class);
    }

    /* MUTATOR METHODS */

    /**
     * Remove a special transition, given its transition data.
     * 
     * @param data The transition data associated with the special transitions to be
     *             removed
     **/
    @Override
    protected void removeTransitionData(TransitionData data) {

        unconditionalViolations.remove(data);

        conditionalViolations.remove(data);

        // Multiple potential communications could exist for the same transition (this
        // happens when there are more than one potential sender)
        while (potentialCommunications.remove(data))
            ;

        // Multiple Nash communications could exist for the same transition (this
        // happens when there are more than one potential sender)
        while (nashCommunications.remove(data))
            ;

        invalidCommunications.remove(data);

        disablementDecisions.remove(data);

    }

    /**
     * Add an unconditional violation.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     **/
    public void addUnconditionalViolation(long initialStateID, int eventID, long targetStateID) {
        unconditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));
    }

    /**
     * Add a conditional violation.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     **/
    public void addConditionalViolation(long initialStateID, int eventID, long targetStateID) {
        conditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));
    }

    /**
     * Add a potential communication.
     * 
     * @param initialStateID     The initial state
     * @param eventID            The event triggering the transition
     * @param targetStateID      The target state
     * @param communicationRoles The roles associated with each controller
     **/
    public void addPotentialCommunication(long initialStateID,
            int eventID,
            long targetStateID,
            CommunicationRole[] communicationRoles) {

        potentialCommunications.add(new CommunicationData(initialStateID, eventID, targetStateID, communicationRoles));
    }

    /**
     * Clears the list of potential communications.
     **/
    public void removeAllPotentialCommunications() {
        potentialCommunications.clear();
    }

    /**
     * Add an invalid communication (which are the communications that were added
     * for mathematical completeness but are not actually potential communications).
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     **/
    public void addInvalidCommunication(long initialStateID, int eventID, long targetStateID) {
        invalidCommunications.add(new TransitionData(initialStateID, eventID, targetStateID));
    }

    /**
     * Add a nash communication.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     * @param roles          The communication roles associated with each controller
     * @param cost           The cost of this communication
     * @param probability    The probability of choosing this communication (a value
     *                       between 0 and 1, inclusive)
     **/
    public void addNashCommunication(long initialStateID,
            int eventID,
            long targetStateID,
            CommunicationRole[] roles,
            double cost,
            double probability) {

        nashCommunications
                .add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));

    }

    /**
     * Clears the list of nash communications.
     **/
    public void removeAllNashCommunications() {
        nashCommunications.clear();
    }

    /**
     * Add a disablement decision.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     * @param controllers    An array indicating which controllers can disable this
     *                       transition
     **/
    public void addDisablementDecision(long initialStateID, int eventID, long targetStateID, boolean[] controllers) {
        disablementDecisions.add(new DisablementData(initialStateID, eventID, targetStateID, controllers));
    }

    /* ACCESSOR METHODS */

    /**
     * Check to see if this U-Structure contains violations.
     * <p>
     * NOTE: Conditional violations are not included for our purposes.
     * 
     * @return Whether or not there are one or more violations
     **/
    public boolean hasViolations() {
        return unconditionalViolations.size() > 0;
    }

    /**
     * Find an arbitrary unconditional violation leading from this state, if one
     * exists.
     * 
     * @param startingState The state in which the unconditional violation should
     *                      come from
     * @return The violation data (or {@code null}, if none existed)
     **/
    public TransitionData findUnconditionalViolation(State startingState) {

        // Check each transition
        for (Transition transition : startingState.getTransitions()) {

            // Return the first violation that matches the state's ID (if one is found)
            for (TransitionData data : unconditionalViolations)
                if (data.initialStateID == startingState.getID())
                    return data;

        }

        return null;

    }

    /**
     * Returns the list of unconditional violations. The returned list is
     * {@link Collections#unmodifiableList(List) unmodifiable}.
     * 
     * @return the list of unconditional violations
     * 
     * @since 2.1.0
     */
    public List<TransitionData> getUnconditionalViolations() {
        return Collections.unmodifiableList(unconditionalViolations);
    }

    /**
     * Returns the list of conditional violations. The returned list is
     * {@link Collections#unmodifiableList(List) unmodifiable}.
     * 
     * @return the list of conditional violations
     * 
     * @since 2.1.0
     */
    public List<TransitionData> getConditionalViolations() {
        return Collections.unmodifiableList(conditionalViolations);
    }

    /**
     * Get the list of potential communications.
     * 
     * @return The potential communications
     **/
    public List<CommunicationData> getPotentialCommunications() {
        return potentialCommunications;
    }

    /**
     * Returns the list of invalid communications. The returned list is
     * {@link Collections#unmodifiableList(List) unmodifiable}.
     * 
     * @return the list of invalid communications
     * 
     * @since 2.1.0
     */
    public List<TransitionData> getInvalidCommunications() {
        return Collections.unmodifiableList(invalidCommunications);
    }

    /**
     * Get the list of Nash communications.
     * 
     * @return The Nash communications
     **/
    public List<NashCommunicationData> getNashCommunications() {
        return nashCommunications;
    }

    /**
     * Get the size of the union of the list of potential communications and Nash
     * communications.
     * <p>
     * NOTE: This method gets the size without actually creating a union of the two
     * sets.
     * 
     * @return The combined size of the potential communications and Nash
     *         communications
     **/
    public int getSizeOfPotentialAndNashCommunications() {
        return potentialCommunications.size() + nashCommunications.size();
    }

    /**
     * Get the union of the list of potential communications and Nash
     * communications.
     * <p>
     * NOTE: This method generates a new list each time it is called.
     * 
     * @return The potential communications and Nash communications
     **/
    public List<CommunicationData> getPotentialAndNashCommunications() {

        List<CommunicationData> communications = new ArrayList<CommunicationData>();
        communications.addAll(potentialCommunications);
        communications.addAll(nashCommunications);

        return communications;

    }

    /**
     * Get the list of disablement decisions.
     * 
     * @return The disablement decisions
     **/
    public List<DisablementData> getDisablementDecisions() {
        return disablementDecisions;
    }

    /**
     * Returns the set of enablement states for a specific event.
     * 
     * @param eventLabel the label of an event
     * 
     * @return the set of enablement states
     * 
     * @since 2.0
     */
    public Set<State> getEnablementStates(String eventLabel) {
        Set<State> enablementStates = getStates().parallelStream().filter(s -> s.isEnablementStateOf(eventLabel)).collect(Collectors.toSet());
        return enablementStates;
    }

    /**
     * Returns the set of disablement states for a specific event.
     * 
     * @param eventLabel the label of an event
     * 
     * @return the set of disablement states
     * 
     * @since 2.0
     */
    public Set<State> getDisablementStates(String eventLabel) {
        Set<State> disablementStates = getStates().parallelStream().filter(s -> s.isDisablementStateOf(eventLabel)).collect(Collectors.toSet());
        return disablementStates;
    }

    /**
     * Returns the set of illegal configurations for a specific event.
     * 
     * @param eventLabel the label of an event
     * 
     * @return the set of illegal configurations
     * 
     * @since 2.1.0
     */
    public Set<State> getIllegalConfigStates(String eventLabel) {
        Set<State> disablementStates = getStates().parallelStream().filter(s -> s.isIllegalConfigurationOf(eventLabel)).collect(Collectors.toSet());
        return disablementStates;
    }

}
