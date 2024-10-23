/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.logging.log4j.*;

import com.google.gson.JsonObject;

/**
 * Represents a pruned U-Structure.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class PrunedUStructure extends UStructure {

    private static Logger logger = LogManager.getLogger();

    /* CONSTRUCTORS */

    /**
     * Constructs a new {@code PrunedUStructure} with the specified number of
     * controllers.
     * 
     * @param nControllers the number of controllers that the new pruned U-Structure
     *                     has (1 implies centralized control, >1 implies
     *                     decentralized control)
     * @throws IllegalArgumentException if argument is not positive
     * 
     * @since 2.0
     **/
    public PrunedUStructure(int nControllers) {
        super(nControllers);
    }

    /**
     * Constructs a new {@code PrunedUStructure} that is represented by a JSON
     * object
     * 
     * @param jsonObject a JSON object that represents an automaton
     * 
     * @see Automaton#buildAutomaton(JsonObject)
     * @since 2.0
     **/
    PrunedUStructure(JsonObject jsonObject) {
        super(jsonObject);
    }

    /* AUTOMATA OPERATIONS */

    @Override
    public PrunedUStructure accessible() {
        return AutomataOperations.accessible(this, PrunedUStructure::new);
    }

    /**
     * Using recursion, starting at a given state, prune away all necessary
     * transitions.
     * 
     * @param <T>            The type of communication data
     * @param protocol       The chosen protocol (which must be feasible)
     * @param communication  The event vector representing the chosen communication
     * @param initialStateID The ID of the state where the pruning begins at
     * @param indexOfSender  The index of the sender
     **/
    public <T extends CommunicationData> void prune(Set<T> protocol,
            LabelVector communication,
            long initialStateID,
            int indexOfSender) {

        Set<Long> indistinguishableStates = new HashSet<Long>();
        findConnectingStates(this, this.invert(), indistinguishableStates, initialStateID, indexOfSender);
        // System.out.println("communication: " + communication + ", index of sender: "
        // + indexOfSender);
        for (long s : indistinguishableStates)
            pruneHelper(protocol, communication, new boolean[communication.getSize()], getState(s), 0);

    }

    /**
     * Helper method used to prune the U-Structure.
     * 
     * @param <T>                 The type of communication data
     * @param protocol            The chosen protocol (which must be feasible)
     * @param communication       The event vector representing the chosen
     *                            communication
     * @param vectorElementsFound Indicates which elements of the vector have
     *                            already been found
     * @param currentState        The state that we are currently on
     * @param depth               The current depth of the recursion (first
     *                            iteration has a depth of 0)
     **/
    private <T extends CommunicationData> void pruneHelper(Set<T> protocol,
            LabelVector communication,
            boolean[] vectorElementsFound,
            State currentState,
            int depth) {

        /* Base case */

        if (depth == nControllers)
            return;

        /* Recursive case */

        // Try all transitions leading from this state
        outer: for (int i = 0; i < currentState.getTransitions().size(); i++) {

            Transition t = currentState.getTransition(i);

            // We do not want to prune any of the chosen communications
            if (depth == 0) {
                for (CommunicationData data : protocol)
                    if (currentState.getID() == data.initialStateID && t.getEvent().getID() == data.eventID
                            && t.getTargetStateID() == data.targetStateID) {
                        // System.out.println("\t\tSkipped: " + t);
                        continue outer;
                    }
            }

            // System.out.println("\t\tProcessed: " + t);

            boolean[] copy = vectorElementsFound.clone();

            // Check to see if the event vector of this transition is compatible with what
            // we've found so far
            for (int j = 0; j < t.getEvent().getVector().getSize(); j++) {

                String element = t.getEvent().getVector().getLabelAtIndex(j);

                if (!element.equals(Event.EPSILON)) {

                    // Conflict since we have already found an element for this index (so they
                    // aren't compatible)
                    if (copy[j])
                        continue outer;

                    // Is compatible
                    else if (element.equals(communication.getLabelAtIndex(j)))
                        copy[j] = true;

                    // Conflict since the elements do not match (meaning they aren't compatible)
                    else
                        continue outer;
                }

            }

            // System.out.println("\t\tRemoved: " + t);

            // Prune this transition
            removeTransition(currentState.getID(), t.getEvent().getID(), t.getTargetStateID());
            i--;

            // Recursive call to the state where this transition leads
            pruneHelper(protocol, communication, copy, getState(t.getTargetStateID()), depth + 1);

        }

    }

    @Override
    public PrunedUStructure clone() {
        return new PrunedUStructure(toJsonObject());
    }

    /* MUTATOR METHODS */

    /**
     * Remove all events which are inactive (meaning that they do not appear in a
     * transition).
     **/
    public void removeInactiveEvents() {

        /* Determine which events are active */

        boolean[] active = new boolean[getNumberOfEvents() + 1];
        for (long s = 1; s <= getNumberOfStates(); s++)
            for (Transition t : getState(s).getTransitions())
                active[t.getEvent().getID()] = true;

        /* Remove the inactive events */

        Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
        int newID = 1;
        int maxID = getNumberOfEvents();
        for (int id = 1; id <= maxID; id++) {
            if (!active[id]) {
                if (!removeEvent(id))
                    logger.error("Failed to remove inactive event.");
            } else
                mapping.put(id, newID++);
        }

        /* Re-number event IDs */

        // Update event IDs
        for (long s = 1; s <= getNumberOfStates(); s++) {

            State state = getState(s);

            // Update the event ID in the transitions
            for (Transition t : state.getTransitions()) {
                Event e = t.getEvent();
                t.setEvent(new Event(e.getLabel(), mapping.get(e.getID()), e.isObservable(), e.isControllable()));
            }

        }

        // Update event IDs
        for (Event e : getEvents())
            e.setID(mapping.get(e.getID()));
        renumberEventsInTransitionData(mapping, unconditionalViolations);
        renumberEventsInTransitionData(mapping, conditionalViolations);
        renumberEventsInTransitionData(mapping, potentialCommunications);
        renumberEventsInTransitionData(mapping, invalidCommunications);
        renumberEventsInTransitionData(mapping, nashCommunications);
        renumberEventsInTransitionData(mapping, disablementDecisions);

    }

    /**
     * Remove the event with the specified ID.
     * NOTE: After calling this method, the events must be re-numbered, otherwise
     * there will be complications.
     * 
     * @param id The event's ID
     * @return Whether or not the event was successfully removed
     **/
    private boolean removeEvent(int id) {

        Iterator<Event> iterator = getEvents().iterator();

        while (iterator.hasNext()) {

            Event e = iterator.next();

            // Remove the event if the ID matches
            if (e.getID() == id) {

                eventsMap.remove(e.getLabel());

                iterator.remove();

                return true;

            }

        }

        return false;

    }

    /**
     * Helper method to re-number event IDs in the specified list of special
     * transitions.
     * 
     * @param mapping The state ID mappings
     * @param list    The list of special transition data
     **/
    private void renumberEventsInTransitionData(Map<Integer, Integer> mapping,
            List<? extends TransitionData> list) {

        for (TransitionData data : list)
            data.eventID = mapping.get(data.eventID);

    }

}
