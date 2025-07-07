/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A sequence of transitions that contains state and event IDs.
 * Every instance of {@code Sequence} is immutable.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public final class Sequence implements Cloneable, Iterable<Pair<Integer, Long>> {

    /** The sequence of state IDs */
    private long[] stateIDs;
    /** The sequence of event IDs */
    private int[] eventIDs;

    /** Private constructor */
    private Sequence() {
        stateIDs = ArrayUtils.EMPTY_LONG_ARRAY;
        eventIDs = ArrayUtils.EMPTY_INT_ARRAY;
    }

    /**
     * Constructs a new {@code Sequence}.
     * 
     * @param initialStateID the initial state ID
     */
    public Sequence(long initialStateID) {
        stateIDs = new long[] { initialStateID };
        eventIDs = ArrayUtils.EMPTY_INT_ARRAY;
    }

    /**
     * Constructs a new {@code Sequence} of arbitrary length.
     * 
     * @param stateIDs the sequence of state IDs
     * @param eventIDs the sequence of event IDs that triggered transitions between states
     * 
     * @throws IllegalArgumentException if {@code stateIDs} is empty or the lengths of the arguments
     *                                  are invalid
     * @throws NullPointerException if either argument is {@code null}
     */
    public Sequence(long[] stateIDs, int[] eventIDs) {
        Objects.requireNonNull(stateIDs);
        Objects.requireNonNull(eventIDs);
        if (stateIDs.length == 0) {
            throw new IllegalArgumentException("stateIDs cannot be empty");
        } else if (stateIDs.length != eventIDs.length + 1) {
            throw new IllegalArgumentException("Invalid number of events");
        }
        this.stateIDs = stateIDs.clone();
        this.eventIDs = eventIDs.clone();
    }

    /**
     * Appends a new transition to this {@code Sequence}.
     * 
     * @param eventID the event triggering the transition
     * @param stateID the target state of this transition
     * @return the appended {@code Sequence}
     */
    public Sequence append(int eventID, long stateID) {
        long[] appendedStateIDs = ArrayUtils.add(stateIDs, stateID);
        int[] appendedEventIDs = ArrayUtils.add(eventIDs, eventID);
        Sequence appendedSequence = new Sequence();
        appendedSequence.stateIDs = appendedStateIDs;
        appendedSequence.eventIDs = appendedEventIDs;
        return appendedSequence;
    }

    /**
     * Returns the length of this {@code Sequence}, that is, the number of
     * states involved in this {@code Sequence}.
     * 
     * @return the length of this {@code Sequence}
     */
    public int length() {
        return stateIDs.length;
    }

    /**
     * Returns the state at the specified index.
     * 
     * @param index an index
     * @return the state at the specified index
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public long getState(int index) {
        return stateIDs[Objects.checkIndex(index, stateIDs.length)];
    }

    /**
     * Returns the event at the specified index.
     * 
     * @param index an index
     * @return the event at the specified index
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public int getEvent(int index) {
        return eventIDs[Objects.checkIndex(index, eventIDs.length)];
    }

    /**
     * Checks whether a state is involved in this {@code Sequence}.
     * 
     * @param stateID a state
     * @return {@code true} if the argument is involved in this {@code Sequence}
     */
    public boolean containsState(long stateID) {
        return ArrayUtils.contains(stateIDs, stateID);
    }

    /**
     * Checks whether an event is involved in this {@code Sequence}.
     * 
     * @param eventID an event
     * @return {@code true} if the argument is involved in this {@code Sequence}
     */
    public boolean containsEvent(int eventID) {
        return ArrayUtils.contains(eventIDs, eventID);
    }

    /**
     * Checks whether the specified state ID is the last state in this sequence.
     * 
     * @param stateID a state
     * @return {@code true} if the argument is the last state in this {@code Sequence}
     * 
     * @since 2.1.0
     */
    public boolean isLastState(long stateID) {
        if (stateIDs.length == 0)
            return false;
        else
            return stateIDs[stateIDs.length - 1] == stateID;
    }

    /**
     * Returns an iterator over the transitions in this {@code Sequence}.
     * The values returned by this iterator are {@code (event, target state)}
     * pairs, with the exception of the first value (whose event ID is 
     * {@code 0}, indicating that there is no corresponding event for the
     * initial state). The pairs returned by this iterator are immutable.
     * 
     * @return an iterator
     */
    @Override
    public ResettableIterator<Pair<Integer, Long>> iterator() {
        return new ResettableIterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < stateIDs.length;
            }

            @Override
            public Pair<Integer, Long> next() {
                if (index == 0) {
                    return Pair.of(0, stateIDs[index++]);
                }
                Pair<Integer, Long> nextValue = Pair.of(eventIDs[index - 1], stateIDs[index]);
                index++;
                return nextValue;
            }

            @Override
            public void reset() {
                index = 0;
            }
        };
    }

    /**
     * Finds the index of the given event.
     * 
     * @param eventID an event
     * @return  the index of the event, or {@link ArrayUtils#INDEX_NOT_FOUND}
     *          ({@code -1}) if not found
     */
    public int indexOfEvent(int eventID) {
        return ArrayUtils.indexOf(eventIDs, eventID);
    }

    /**
     * Finds the index of the given state.
     * 
     * @param eventID a state
     * @return  the index of the state, or {@link ArrayUtils#INDEX_NOT_FOUND}
     *          ({@code -1}) if not found
     */
    public int lastIndexOfEvent(int eventID) {
        return ArrayUtils.lastIndexOf(eventIDs, eventID);
    }

    /**
     * Indicates whether an object is "equal to" this {@code Sequence}.
     * 
     * @param o the object to compare with
     * @return {@code true} if argument is equal to this {@code Sequence}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        else if (o instanceof Sequence s) {
            return Arrays.equals(this.stateIDs, s.stateIDs) && Arrays.equals(this.eventIDs, s.eventIDs);
        }
        else return false;
    }

    /**
     * Returns a hash code for this {@code Sequence}.
     * 
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(stateIDs), Arrays.hashCode(eventIDs));
    }

    /**
     * Converts this sequence to an array of state IDs.
     * 
     * @return  a new allocated array that corresponds to the
     *          states in this {@code Sequence}
     */
    public long[] getStateArray() {
        if (stateIDs.length == 0)
            return ArrayUtils.EMPTY_LONG_ARRAY;
        return stateIDs.clone();
    }

    /**
     * Converts this sequence to an array of event IDs.
     * 
     * @return  a new allocated array that corresponds to the
     *          events in this {@code Sequence}
     */
    public int[] getEventArray() {
        if (eventIDs.length == 0)
            return ArrayUtils.EMPTY_INT_ARRAY;
        return eventIDs.clone();
    }

    /**
     * Converts this sequence to a list of state IDs. The list returned by
     * this method is {@link List#of(Object...) immutable}.
     * 
     * @return  a list representation of the states in this {@code Sequence}
     */
    public List<Long> getStateList() {
        return List.of(ArrayUtils.toObject(stateIDs));
    }

    /**
     * Converts this sequence to a list of event IDs. The list returned by
     * this method is {@link List#of(Object...) immutable}.
     * 
     * @return  a list representation of the events in this {@code Sequence}
     */
    public List<Integer> getEventList() {
        return List.of(ArrayUtils.toObject(eventIDs));
    }

    /**
     * Creates and returns a copy of this sequence.
     * 
     * @return a copy of this sequence
     * 
     * @since 2.2.0
     */
    @Override
    public Sequence clone() {
        Sequence clone = new Sequence();
        clone.stateIDs = this.stateIDs.clone();
        clone.eventIDs = this.eventIDs.clone();
        return clone;
    }

    /**
     * Returns a reversed copy of this sequence.
     * 
     * @return a reversed copy of this sequence
     * 
     * @since 2.2.0
     */
    public Sequence reversed() {
        Sequence reversed = this.clone();
        ArrayUtils.reverse(reversed.stateIDs);
        ArrayUtils.reverse(reversed.eventIDs);
        return reversed;
    }

    /**
     * Returns a string representation of this {@code Sequence}.
     * @return a string representation of this {@code Sequence}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(stateIDs[0]);
        for (int i = 1; i < stateIDs.length; i++) {
            sb.append(" -");
            sb.append(eventIDs[i - 1]);
            sb.append("-> ");
            sb.append(stateIDs[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
