/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.*;

import org.apache.commons.lang3.Validate;

import com.github.automaton.automata.*;

/**
 * A Java implementation of a counterexample.
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
public final class Counterexample {

    private Event event;
    private List<Word> words;

    /**
     * Constructs a new {@code Counterexample}.
     * 
     * @param event the event that generates this counterexample
     * @param list the list of sequences that correspond to this counterexample
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     * @throws IllegalArgumentException if {@code list} contains {@code null}
     */
    public Counterexample(Event event, List<Word> list) {
        this.event = Objects.requireNonNull(event);
        this.words = Validate.noNullElements(list);
    }

    /**
     * Returns the event that this counterexample corresponds to.
     * 
     * @return the event that this counterexample corresponds to
     */
    public Event getEvent() {
        return this.event;
    }

    /**
     * Returns the list of words in this counterexample.
     * 
     * @return the list of words in this counterexample
     */
    public List<Word> getWords() {
        return words;
    }

    /**
     * Returns the string representation of this counterexample.
     * 
     * @return the string representation of this counterexample
     */
    @Override
     public String toString() {
        return String.format("(%s: %s)", event.getLabel(), words.toString());
    }

    /**
     * Checks whether an object is equal to this counterexample.
     * 
     * @param other an object
     * @return {@code true} if argument is equal to this counterexample
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof Counterexample ce) {
            return Objects.equals(this.event, ce.event) && Objects.equals(this.words, ce.words);
        } else
            return false;
    }

    /**
     * Returns a hash code for this counterexample.
     * 
     * @return a hash code
     */
    @Override
     public int hashCode() {
        return Objects.hash(event, words);
    }
}
