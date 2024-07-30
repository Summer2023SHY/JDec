/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Holds all 3 pieces of information needed to identify a transition.
 *
 * <p>
 * NOTE: This class is different from the {@link Transition} class, since
 * this class does not need to be attached to a specific state in order to
 * fully represent a transition (the {@link Transition} class does not have a
 * reference to the initial state ID, and it contains a reference to the actual
 * {@link Event} object instead of only holding onto its ID).
 * </p>
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class TransitionData implements Cloneable {

    /* PUBLIC INSTANCE VARIABLES */

    /** The ID of the state that the transition starts at. */
    public long initialStateID;

    /** The ID of the event which causes the transition. */
    public int eventID;

    /** The ID of the state that the transition ends at. */
    public long targetStateID;

    /* CONSTRUCTOR */

    /**
     * Private constructor for compatibility with gson
     * 
     * @since 2.0
     */
    private TransitionData() {
        this(0, -1, 0);
    }

    /**
     * Construct a TransitionData object using the IDs of the associated event and
     * states.
     * 
     * @param initialStateID The initial state's ID
     * @param eventID        The event's ID
     * @param targetStateID  The target state's ID
     **/
    public TransitionData(long initialStateID, int eventID, long targetStateID) {
        this.initialStateID = initialStateID;
        this.eventID = eventID;
        this.targetStateID = targetStateID;
    }

    /**
     * Construct a {@code TransitionData} object using the specified event and
     * states.
     * 
     * @param initialState The initial state
     * @param event        The event
     * @param targetState  The target state
     * 
     * @since 1.3
     **/
    public TransitionData(State initialState, Event event, State targetState) {
        this.initialStateID = initialState.getID();
        this.eventID = event.getID();
        this.targetStateID = targetState.getID();
    }

    /* METHOD */

    /**
     * Given the source automaton, provide even more information when represented as
     * a string.
     * 
     * @param automaton The automaton where this transition data came from
     * @return The string representation
     **/
    public String toString(Automaton automaton) {
        return String.format(
                "%s,%s,%s",
                automaton.getState(initialStateID).getLabel(),
                automaton.getEvent(eventID).getLabel(),
                automaton.getState(targetStateID).getLabel());
    }

    /* OVERRIDDEN METHODS */

    /**
     * Creates and returns a copy of this {@code TransitionData}.
     * 
     * @return a copy of this {@code TransitionData}
     * 
     * @since 2.0
     */
    @Override
    public TransitionData clone() {
        return new TransitionData(initialStateID, eventID, targetStateID);
    }

    /**
     * Indicates whether an object is "equal to" this transition data
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if this transition data is the same as the argument
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof TransitionData other) {
            if (!Objects.equals(this.getClass(), other.getClass()))
                return false;
            return initialStateID == other.initialStateID
                    && eventID == other.eventID
                    && targetStateID == other.targetStateID;
        } else
            return false;
    }

    /**
     * Returns a hash code for this {@code TransitionData}.
     * 
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(initialStateID, eventID, targetStateID);
    }

    /**
     * Returns string representation of this transition data
     * 
     * @return string representation of this transition data
     */
    @Override
    public String toString() {
        return String.format("(%d,%d,%d)", initialStateID, eventID, targetStateID);
    }

    /**
     * Checks whether the specified list contains one or more self-loops.
     * 
     * @param list a list of transitions
     * @return {@code true} if the supplied list contains a self loop
     * 
     * @throws IllegalArgumentException if argument contains {@code null}
     * @throws NullPointerException     if argument is {@code null}
     * 
     * @since 2.1.0
     */
    public static boolean containsSelfLoop(List<? extends TransitionData> list) {
        Validate.noNullElements(list);
        return list.stream().anyMatch(data -> data.initialStateID == data.targetStateID);
    }

}
