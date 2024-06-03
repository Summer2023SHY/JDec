/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * Represents an event in an automaton. It supports both centralized and
 * decentralized control, which means that it can have observability and
 * controllability properties for each controller. It also has support for
 * events that have labels formatted as vectors.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class Event {

    /* INSTANCE VARIABLES */

    private String label;
    private int id;
    private boolean[] observable, controllable;

    /**
     * Events can sometimes be vectors (for example, automata created by
     * synchronized composition use them).
     * Example of syntax: "&lt;a,b,d>" actually represents an event vector: {"a",
     * "b", "d"}. This instance
     * variable holds a reference to the vectorized event label.
     **/
    private transient LabelVector vector = null;

    /* CONSTRUCTOR */

    /**
     * Private constructor for compatibility with gson
     * 
     * @since 2.0
     */
    private Event() {
        this.label = StringUtils.EMPTY;
        this.id = -1;
        this.observable = ArrayUtils.EMPTY_BOOLEAN_ARRAY;
        this.controllable = ArrayUtils.EMPTY_BOOLEAN_ARRAY;
        this.vector = null;
    }

    /**
     * Construct a new event with the specified properties.
     * 
     * @param label        The name of the event
     * @param id           The ID of the event
     * @param observable   Whether or not the event can be observed
     * @param controllable Whether or not the event can be controlled
     * 
     * @throws IllegalArgumentException if
     *                                  {@code observable.length != controllable.length}
     * @throws NullPointerException     if any argument is {@code null}
     **/
    public Event(String label, int id, boolean[] observable, boolean[] controllable) {
        if (Objects.requireNonNull(observable).length != Objects.requireNonNull(controllable).length) {
            throw new IllegalArgumentException("Invalid input arrays");
        }
        this.label = Objects.requireNonNull(label);
        this.id = id;
        this.observable = observable;
        this.controllable = controllable;
        this.vector = new LabelVector(label);

    }

    /**
     * Construct a new event with the specified properties
     * 
     * @param labelVector  the label vector
     * @param id           The ID of the event
     * @param observable   Whether or not the event can be observed
     * @param controllable Whether or not the event can be controlled
     * 
     * @throws IllegalArgumentException if
     *                                  {@code observable.length != controllable.length}
     * @throws NullPointerException     if any argument is {@code null}
     * 
     * @since 1.3
     */
    public Event(LabelVector labelVector, int id, boolean[] observable, boolean[] controllable) {
        if (Objects.requireNonNull(observable).length != Objects.requireNonNull(controllable).length) {
            throw new IllegalArgumentException("Invalid input arrays");
        }
        this.label = Objects.requireNonNull(labelVector).toString();
        this.vector = labelVector;
        this.id = id;
        this.observable = observable;
        this.controllable = controllable;
    }

    /* MUTATOR METHOD */

    /**
     * Set the event's ID number.
     * 
     * @param id The new ID for the event
     **/
    public void setID(int id) {
        this.id = id;
    }

    /* ACCESSOR METHODS */

    /**
     * Get the label of the event.
     * 
     * @return The event's label
     **/
    public String getLabel() {
        return label;
    }

    /**
     * Get the ID number of the event.
     * 
     * @return The ID
     **/
    public int getID() {
        return id;
    }

    /**
     * Get the observability property of the event for each controller.
     * 
     * @return Whether or not the event is observable
     **/
    public boolean[] isObservable() {
        return observable;
    }

    /**
     * Checks whether a specific controller can observe this event.
     * 
     * @param controller a controller
     * @return {@code true} if the specified controller can observe this event
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     * 
     * @since 2.0
     **/
    public boolean isObservable(int controller) {
        return observable[Objects.checkIndex(controller, observable.length)];
    }

    /**
     * Get the controllability property of the event for each controller.
     * 
     * @return Whether or not the event is controllable
     **/
    public boolean[] isControllable() {
        return controllable;
    }

    /**
     * Checks whether a specific controller can control this event.
     * 
     * @param controller a controller
     * @return {@code true} if the specified controller can control this event
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     * 
     * @since 2.0
     **/
    public boolean isControllable(int controller) {
        return controllable[Objects.checkIndex(controller, controllable.length)];
    }

    /**
     * Get the event vector.
     * 
     * @return The event vector
     **/
    public LabelVector getVector() {
        if (this.vector == null) {
            this.vector = new LabelVector(label);
        }
        return vector;
    }

    /* OVERRIDDEN METHODS */

    /**
     * Indicates whether an object is "equal to" this event
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if this event is the same as the argument
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof Event) {
            Event other = (Event) obj;
            return this.label.equals(other.label);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.label.hashCode();
    }

    /**
     * Returns string representation of this event
     * 
     * @return string representation of this event
     */
    @Override
    public String toString() {
        return "("
                + "\"" + label + "\",ID:"
                + id + ","
                + "Observable=" + Arrays.toString(observable) + ","
                + "Controllable=" + Arrays.toString(controllable)
                + ")";
    }

}
