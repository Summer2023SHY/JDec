package com.github.automaton.automata;

/* 
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.util.Objects;

/**
 * The {@code AmbiguityData} class represents control decision of 
 * an inference observable system.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 * 
 * @see Automaton#testObservability(boolean)
 */
public final class AmbiguityData {

    /**
     * The maximum ambiguity level.
     */
    public static final int MAX_AMB_LEVEL = Integer.MAX_VALUE;

    private State state;
    private Event event;
    private int controller;
    private boolean isEnablement;
    private int ambLevel;

    /**
     * Constructs a new {@code AmbiguityData} object.
     * 
     * @param state a state
     * @param event an event
     * @param controller the controller (1-based index)
     * @param isEnablement whether this decision is an enablement decision
     * @param ambLevel     the ambiguity level associated with the data
     * 
     * @throws IllegalArgumentException if {@code ambLevel} is negative
     * @throws IndexOutOfBoundsException if {@code controller} is invalid for the specified event
     * @throws NullPointerException if either one of {@code state} or {@code event} is {@code null}
     */
    AmbiguityData(State state, Event event, int controller, boolean isEnablement, int ambLevel) {
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
    public State getState() {
        return state;
    }

    /**
     * Returns the event this data is for.
     * 
     * @return the event for this data
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Returns the controller this data is for.
     * 
     * @return the controller (1-based index)
     */
    public int getController() {
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
    public int getAmbiguityLevel() {
        return ambLevel;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(state, event, controller, isEnablement);
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
            event.getLabel(), getController(), ambLevel
        );
    }

}
