/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Represents a state in an automaton, complete with a label and transitions.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class State implements Cloneable {

    /* CLASS CONSTANTS */

    // These masks allow us to store and access multiple true/false values within
    // the same byte
    /**
     * Bitmask for checking whether or not a state actually exists here
     * 
     * @deprecated This constant is for the legacy I/O component and no longer
     *             used. Use
     *             {@link com.github.automaton.io.legacy.StateIO#EXISTS_MASK}
     *             instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static final int EXISTS_MASK = 0b00000010;
    /**
     * Bitmask for checking whether or not a state is marked
     * 
     * @deprecated This constant is for the legacy I/O component and no longer
     *             used. Use
     *             {@link com.github.automaton.io.legacy.StateIO#MARKED_MASK}
     *             instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static final int MARKED_MASK = 0b00000001;
    /**
     * Bitmask for checking whether or not a state is an enablement state
     * 
     * @since 1.3
     * @deprecated This constant is for the legacy I/O component and no longer
     *             used. Use
     *             {@link com.github.automaton.io.legacy.StateIO#ENABLEMENT_MASK}
     *             instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static final int ENABLEMENT_MASK = 0b00000100;
    /**
     * Bitmask for checking whether or not a state is a disablement state
     * 
     * @since 1.3
     * @deprecated This constant is for the legacy I/O component and no longer
     *             used. Use
     *             {@link com.github.automaton.io.legacy.StateIO#DISABLEMENT_MASK}
     *             instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static final int DISABLEMENT_MASK = 0b00001000;

    /* INSTANCE VARIABLES */

    private String label;
    private long id;
    private boolean marked;
    private List<Transition> transitions;

    /**
     * The set of events that this state is an enablement configuration of.
     * 
     * @since 2.1.0
     */
    private Set<String> enablementEvents;
    /**
     * The set of events that this state is a disablement configuration of.
     * 
     * @since 2.1.0
     */
    private Set<String> disablementEvents;
    /**
     * The set of events that this state represents an illegal configuration of.
     * 
     * @since 2.1.0
     */
    private Set<String> illegalConfigEvents;

    /* CONSTRUCTORS */

    /**
     * Default constructor. All instances created using this constructor
     * must update the instance variables using the respective mutator methods.
     * 
     * @since 1.3
     */
    State() {
        transitions = new ArrayList<Transition>();
        enablementEvents = new LinkedHashSet<String>();
        disablementEvents = new LinkedHashSet<String>();
        illegalConfigEvents = new LinkedHashSet<String>();
    }

    /**
     * Construct a state (including transitions).
     * 
     * @param label       The state's label
     * @param id          The state ID
     * @param marked      Whether or not the state is marked
     * @param transitions The list of transitions leading out from this state
     * @param enablement  Whether or not the state is an enablement state
     * @param disablement Whether or not the state is an disablement state
     * 
     * @throws UnsupportedOperationException always
     * 
     * @deprecated This constructor should no longer be used.
     * @since 1.3
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public State(String label, long id, boolean marked, List<Transition> transitions, boolean enablement,
            boolean disablement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs a new state with the specified transitions and the control
     * information.
     * 
     * @param label             The state's label
     * @param id                The state ID
     * @param marked            Whether or not the state is marked
     * @param transitions       The list of transitions leading out from this state
     * @param enablementEvents  the set of events that this state is an enablement
     *                          configuration of
     * @param disablementEvents the set of events that this state is a disablement
     *                          configuration of
     * @throws IllegalArgumentException if {@code enablement && disablement} is
     *                                  {@code true}
     * 
     * @since 2.1.0
     */
    public State(String label, long id, boolean marked, List<Transition> transitions, Set<String> enablementEvents,
            Set<String> disablementEvents) {
        this(label, id, marked, transitions);
        if (!SetUtils.intersection(enablementEvents, disablementEvents).isEmpty())
            throw new IllegalArgumentException(
                    "A state cannot be an enablement and a disablement of the same event simultaneously");
        this.disablementEvents.addAll(disablementEvents);
        this.enablementEvents.addAll(enablementEvents);
    }

    /**
     * Constructs a new state with the specified transitions and the control
     * information.
     * 
     * @param label             The state's label
     * @param id                The state ID
     * @param marked            Whether or not the state is marked
     * @param transitions       The list of transitions leading out from this state
     * @param enablementEvents  the set of events that this state is an enablement
     *                          configuration of
     * @param disablementEvents the set of events that this state is a disablement
     *                          configuration of
     * @throws IllegalArgumentException if {@code enablement && disablement} is
     *                                  {@code true}
     * 
     * @since 2.1.0
     */
    public State(String label, long id, boolean marked, List<Transition> transitions, Set<String> enablementEvents,
            Set<String> disablementEvents, Set<String> illegalConfigEvents) {
        this(label, id, marked, transitions);
        if (!SetUtils.intersection(enablementEvents, disablementEvents).isEmpty())
            throw new IllegalArgumentException(
                    "A state cannot be an enablement and a disablement of the same event simultaneously");
        this.disablementEvents.addAll(disablementEvents);
        this.enablementEvents.addAll(enablementEvents);
        this.illegalConfigEvents.addAll(illegalConfigEvents);
    }

    /**
     * Constructs a new state with the specified transitions.
     * 
     * @param label       The state's label
     * @param id          The state ID
     * @param marked      Whether or not the state is marked
     * @param transitions The list of transitions leading out from this state
     **/
    public State(String label, long id, boolean marked, List<Transition> transitions) {
        this.label = label;
        this.id = id;
        this.marked = marked;
        this.transitions = Objects.requireNonNullElse(transitions, new ArrayList<>());
        this.disablementEvents = new LinkedHashSet<>();
        this.enablementEvents = new LinkedHashSet<>();
    }

    /**
     * Construct a state (with 0 transitions).
     * 
     * @param label       The state's label
     * @param id          The state ID
     * @param marked      Whether or not the state is marked
     * @param enablement  Whether or not the state is an enablement state
     * @param disablement Whether or not the state is an disablement state
     * @throws IllegalArgumentException if {@code enablement && disablement} is
     *                                  {@code true}
     * 
     * @deprecated This constructor should no longer be used.
     * @since 1.3
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public State(String label, long id, boolean marked, boolean enablement, boolean disablement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs a new state with the specified control information.
     * 
     * @param label             The state's label
     * @param id                The state ID
     * @param marked            Whether or not the state is marked
     * @param enablementEvents  the set of events that this state is an enablement
     *                          configuration of
     * @param disablementEvents the set of events that this state is a disablement
     *                          configuration of
     * @throws IllegalArgumentException if {@code enablement && disablement} is
     *                                  {@code true}
     * 
     * @since 2.1.0
     **/
    public State(String label, long id, boolean marked, Set<String> enablementEvents, Set<String> disablementEvents) {
        this(label, id, marked, new ArrayList<>(), enablementEvents, disablementEvents);
    }

    /**
     * Constructs a new state with the specified control information.
     * 
     * @param label             The state's label
     * @param id                The state ID
     * @param marked            Whether or not the state is marked
     * @param enablementEvents  the set of events that this state is an enablement
     *                          configuration of
     * @param disablementEvents the set of events that this state is a disablement
     *                          configuration of
     * @throws IllegalArgumentException if {@code enablement && disablement} is
     *                                  {@code true}
     * 
     * @since 2.1.0
     **/
    public State(String label, long id, boolean marked, Set<String> enablementEvents, Set<String> disablementEvents, Set<String> illegalConfigEvents) {
        this(label, id, marked, enablementEvents, disablementEvents);
        this.illegalConfigEvents.addAll(illegalConfigEvents);
    }

    /**
     * Constructs a new state.
     * 
     * @param label  The state's label
     * @param id     The state ID
     * @param marked Whether or not the state is marked
     **/
    public State(String label, long id, boolean marked) {
        this(label, id, marked, new ArrayList<>());
    }

    /* MUTATOR METHODS */

    /**
     * Changes the label of this state.
     * 
     * @param label new label for this state
     * @throws NullPointerException if argument is {@code null}
     * @since 1.3
     */
    void setLabel(String label) {
        this.label = Objects.requireNonNull(label);
    }

    /**
     * Change the ID of this state.
     * 
     * @param id The new ID
     **/
    public void setID(long id) {
        this.id = id;
    }

    /**
     * Change the marked status of this state.
     * 
     * @param marked Whether or not this state should be marked
     **/
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    /**
     * Change the enablement status of this state.
     * 
     * @param enablement Whether or not this state is an enablement state
     * 
     * @throws UnsupportedOperationException always
     * 
     * @since 1.3
     * 
     * @deprecated This method is no longer used.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    void setEnablement(boolean enablement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Marks this state as an enablement config of the specified event.
     * 
     * @param event an event
     * @return {@code true} if this state is modified by the call
     * 
     * @since 2.1.0
     */
    boolean setEnablementOf(String event) {
        if (disablementEvents.contains(event))
            return false;
        return enablementEvents.add(event);
    }

    /**
     * Change the disablement status of this state.
     * 
     * @param disablement Whether or not this state is an disablement state
     * 
     * @throws UnsupportedOperationException always
     * 
     * @since 1.3
     * 
     * @deprecated This method is no longer used.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    void setDisablement(boolean disablement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Marks this state as a disablement config of the specified event.
     * 
     * @param event an event
     * @return {@code true} if this state is modified by the call
     * 
     * @since 2.1.0
     */
    boolean setDisablementOf(String event) {
        if (enablementEvents.contains(event))
            return false;
        return disablementEvents.add(event);
    }

    /**
     * Changes whether this state represents an illegal configuration.
     * 
     * @param event an event
     * 
     * @since 2.1.0
     */
    boolean setIllegalConfigOf(String event) {
        if (isEnablementStateOf(event) || isDisablementStateOf(event))
            return illegalConfigEvents.add(event);
        return false;
    }

    /**
     * Changes whether this state represents an illegal configuration.
     * 
     * @param illegalConfig whether this state represents an illegal configuration
     * 
     * @since 2.1.0
     */
    boolean setIllegalConfigOf(Collection<String> events) {
        boolean modified = false;
        for (String event : events)
            modified |= setIllegalConfigOf(event);
        return modified;
    }

    /**
     * Add a transition to the list.
     * 
     * @param transition The new transition
     * @return {@code true} if the transition was added successfully
     * 
     * @revised 2.0
     **/
    public boolean addTransition(Transition transition) {
        if (transitions.contains(transition))
            return false;
        return transitions.add(transition);
    }

    /**
     * Remove a transition from the list.
     * 
     * @param transition The transition to be removed
     * @return Whether or not the removal was successful
     **/
    public boolean removeTransition(Transition transition) {
        return transitions.remove(transition);
    }

    /**
     * Removes all transitions from this {@code State}.
     * 
     * @since 2.0
     */
    public void clearTransitions() {
        transitions.clear();
    }

    /* ACCESSOR METHODS */

    /**
     * Get the marked status of this state.
     * 
     * @return Whether or not the state is marked
     **/
    public boolean isMarked() {
        return marked;
    }

    /**
     * Checks whether this state is an enablement state
     * 
     * @return Whether or not this state is an enablement state
     *
     * @since 1.3
     */
    public boolean isEnablementState() {
        return !enablementEvents.isEmpty();
    }

    /**
     * Checks whether this state is a disablement state
     * 
     * @return Whether or not this state is a disablement state
     * 
     * @since 1.3
     */
    public boolean isDisablementState() {
        return !disablementEvents.isEmpty();
    }

    /**
     * Checks whether this state is an enablement config of the specified event.
     * 
     * @param event an event
     * @return {@code true} if this state is an enablement config of the specified event
     * 
     * @since 2.1.0
     */
    public boolean isEnablementStateOf(String event) {
        return enablementEvents.contains(event);
    }

    Set<String> getEnablementEvents() {
        return enablementEvents;
    }

    Set<String> getDisablementEvents() {
        return disablementEvents;
    }

    Set<String> getIllegalConfigEvents() {
        return illegalConfigEvents;
    }

    /**
     * Checks whether this state is a disablement config of the specified event.
     * 
     * @param event an event
     * @return {@code true} if this state is a disablement config of the specified event
     * 
     * @since 2.1.0
     */
    public boolean isDisablementStateOf(String event) {
        return disablementEvents.contains(event);
    }

    /**
     * Checks whether this state represents an illegal configuration.
     * 
     * @return whether or not this state represents an illegal configuration
     * 
     * @since 2.1.0
     */
    public boolean isIllegalConfiguration() {
        return !illegalConfigEvents.isEmpty();
    }

    /**
     * Checks whether this state represents an illegal configuration of the specified event.
     * 
     * @param event an event
     * 
     * @return whether or not this state represents an illegal configuration
     * 
     * @since 2.1.0
     */
    public boolean isIllegalConfigurationOf(String event) {
        return illegalConfigEvents.contains(event);
    }

    /**
     * Get the state's label.
     * 
     * @return The state's label
     **/
    public String getLabel() {
        return label;
    }

    /**
     * Get the ID of this state.
     * 
     * @return The state's ID
     **/
    public long getID() {
        return id;
    }

    /**
     * Gets the transition leading out from this state with
     * the specified index.
     * 
     * @param index the index of the transition
     * @return the transition with the specified index
     * @throws IndexOutOfBoundsException if argument is out of bounds
     * 
     * @since 2.0
     **/
    public Transition getTransition(int index) {
        return transitions.get(Objects.checkIndex(index, getNumberOfTransitions()));
    }

    /**
     * Get the list of transitions leading out from this state.
     * The returned list is {@link Collections#unmodifiableList(List) unmodifiable}.
     * 
     * @return The list of transitions
     **/
    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    /**
     * Get the number of transitions leading out from this state.
     * 
     * @return The number of transitions
     **/
    public int getNumberOfTransitions() {
        return transitions.size();
    }

    /* OVERRIDDEN METHODS */

    /**
     * Returns a copy of this {@code State}.
     * The transitions stored in this {@code State} are cloned, but
     * the events triggering the transitions are not.
     * 
     * @return a copy of this {@code State}
     * 
     * @see Transition#clone()
     * @since 2.0
     */
    @Override
    public State clone() {
        List<Transition> clonedTransitions = new ArrayList<>();
        for (Transition orig : transitions) {
            clonedTransitions.add(ObjectUtils.clone(orig));
        }
        return new State(label, id, marked, clonedTransitions, new LinkedHashSet<>(enablementEvents),
                new LinkedHashSet<>(disablementEvents), new LinkedHashSet<>(illegalConfigEvents));
    }

    /**
     * Returns string representation of this state
     * 
     * @return string representation of this state
     */
    @Override
    public String toString() {
        return "(" /*
                    * + "\""
                    */ + Objects.toString(label) /*
                                                  * + "\",ID:"
                                                  * + id + ","
                                                  * + (marked ? "Marked" : "Unmarked") + ","
                                                  * + "# Transitions: " + transitions.size()
                                                  */
                + ")";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(label, id);
    }

    /**
     * Indicates whether an object is "equal to" this state
     * 
     * @param other the reference object with which to compare
     * @return {@code true} if this state is the same as the argument
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        else if (other instanceof State s) {
            return Objects.equals(this.label, s.label) && this.id == s.id;
        } else
            return false;
    }

}
