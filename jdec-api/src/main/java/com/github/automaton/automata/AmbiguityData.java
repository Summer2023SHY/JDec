/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.Objects;

/**
 * Represents control decision of an inference observable system.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 * 
 * @see Automaton#testObservability(boolean)
 */
public record AmbiguityData(State state, Event event, int controller, boolean isEnablement, int ambLevel) {

    /**
     * The maximum ambiguity level.
     */
    public static final int MAX_AMB_LEVEL = Integer.MAX_VALUE;

    /**
     * Constructs a new {@code AmbiguityData}.
     * 
     * @param state        a state
     * @param event        an event
     * @param controller   the controller (1-based index)
     * @param isEnablement whether this decision is an enablement decision
     * @param ambLevel     the ambiguity level associated with the data
     * 
     * @throws IllegalArgumentException  if {@code ambLevel} is negative
     * @throws IndexOutOfBoundsException if {@code controller} is invalid for the
     *                                   specified event
     * @throws NullPointerException      if either one of {@code state} or
     *                                   {@code event} is {@code null}
     */
    public AmbiguityData(State state, Event event, int controller, boolean isEnablement, int ambLevel) {
        this.state = Objects.requireNonNull(state);
        this.event = Objects.requireNonNull(event);
        if (controller <= 0 || controller > event.isObservable().length) {
            throw new IndexOutOfBoundsException(controller);
        }
        this.controller = controller;
        this.isEnablement = isEnablement;
        if (ambLevel < 0) {
            throw new IllegalArgumentException("Invalid ambiguity level: " + ambLevel);
        }
        this.ambLevel = ambLevel;
    }

    /**
     * Returns the state this data is for.
     * 
     * @return the state for this data
     */
    public State state() {
        return state;
    }

    /**
     * Returns the event this data is for.
     * 
     * @return the event for this data
     */
    public Event event() {
        return event;
    }

    /**
     * Returns the controller this data is for.
     * 
     * @return the controller (1-based index)
     */
    public int controller() {
        return controller;
    }

    /**
     * Returns whether this decision is an enablement decision.
     * 
     * @return {@code true} if this data is for an enablement decision
     */
    public boolean isEnablement() {
        return isEnablement;
    }

    /**
     * Returns the ambiguity level for this decision.
     * 
     * @return the ambiguity level
     */
    public int ambLevel() {
        return ambLevel;
    }

    /**
     * Determines whether an object is "equal to" this ambiguity data.
     * 
     * @param obj the object to be checked for equality
     * @return {@code true} if this data is equal to the argument
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof AmbiguityData ad) {
            return Objects.equals(this.state, ad.state) && Objects.equals(this.event, ad.event) &&
                    (this.controller == ad.controller) && (this.isEnablement == ad.isEnablement) &&
                    (this.ambLevel == ad.ambLevel);
        } else
            return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(state, event, controller, isEnablement, ambLevel);
    }

    /**
     * Returns the string representation of this data.
     * 
     * @return the string representation of this data
     */
    @Override
    public String toString() {
        return String.format(
                "(%s: %s) - (%s) - (%d) : %d",
                state.getLabel(), isEnablement ? "enablement" : "disablement",
                event.getLabel(), controller(), ambLevel);
    }

}
