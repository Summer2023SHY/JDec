/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

/**
 * A word in a language of events. All {@link Event#EPSILON &epsilon;}s in the
 * word are filtered out. All words are immutable.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public final class Word implements Comparable<Word>, Iterable<String> {

    /**
     * A word representation of {@link Event#EPSILON}.
     */
    public static final Word EPSILON = new Word(0);

    /**
     * The sequence of events that form this word.
     */
    private String[] events;

    /**
     * Constructs a new word with the specified length.
     * 
     * @param length a length
     * 
     * @throws IllegalArgumentException if {@code length} is negative
     */
    private Word(int length) {
        if (length < 0)
            throw new IllegalArgumentException("Invalid size: " + length);
        else if (length == 0)
            this.events = ArrayUtils.EMPTY_STRING_ARRAY;
        else
            this.events = new String[length];
    }

    /**
     * Constructs a new word, of length 1, with the specified event.
     * 
     * @param event an event
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    public Word(String event) {
        Objects.requireNonNull(event);
        if (event.equals(Event.EPSILON))
            this.events = ArrayUtils.EMPTY_STRING_ARRAY;
        else
            this.events = new String[] { event };
    }

    /**
     * Constructs a new word with the specified sequence of events.
     * 
     * @param events a sequence of events, represented as an array
     * 
     * @throws IllegalArgumentException if argument contains {@code null}
     * @throws NullPointerException     if argument is {@code null}
     */
    public Word(String[] events) {
        Validate.noNullElements(events);
        this.events = filterEpsilonEvents(Stream.of(events));
    }

    /**
     * Constructs a new word with the specified sequence of events.
     * 
     * @param events a sequence of events, represented as a list
     * 
     * @throws IllegalArgumentException if argument contains {@code null}
     * @throws NullPointerException     if argument is {@code null}
     */
    public Word(List<String> events) {
        Validate.noNullElements(events);
        this.events = filterEpsilonEvents(events.stream());
    }

    /**
     * Constructs a new word that is a concatenation of two words.
     * 
     * @param w1 the prefix of the new word
     * @param w2 the suffix of the new word
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public Word(Word w1, Word w2) {
        this(Objects.requireNonNull(w1).length() + Objects.requireNonNull(w2).length());
        System.arraycopy(w1.events, 0, this.events, 0, w1.length());
        System.arraycopy(w2.events, 0, this.events, w1.length(), w2.length());
    }

    /**
     * Filters {@link Event#EPSILON &epsilon;}s in the specified stream.
     * 
     * @param events a stream of events
     * @return the filtered sequence of events
     */
    private static String[] filterEpsilonEvents(Stream<String> events) {
        return events.filter(event -> !event.equals(Event.EPSILON)).toArray(String[]::new);
    }

    /**
     * Returns the event at the specified index.
     * 
     * @param index an index
     * @return the event at the specified index
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public String getEventAt(int index) {
        Objects.checkIndex(index, length());
        return events[index];
    }

    /**
     * Returns the word formed by appending the specified event to this word.
     * 
     * @param event an event
     * @return the word formed by appending the specified event to this word
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    public Word append(String event) {
        Objects.requireNonNull(event);
        if (Event.EPSILON.equals(event))
            return this;
        Word appended = new Word(length() + 1);
        System.arraycopy(this.events, 0, appended.events, 0, this.length());
        appended.events[appended.length() - 1] = event;
        return appended;
    }

    /**
     * Returns the word formed by appending the specified word to this word.
     * 
     * @param word a word
     * @return the word formed by appending the specified word to this word
     * 
     * @throws NullPointerException if argument is {@code null}
     * 
     * @see #Word(Word, Word)
     */
    public Word append(Word word) {
        return new Word(this, word);
    }

    /**
     * Returns the length of this word.
     * 
     * @return the length of this word
     */
    public int length() {
        return this.events.length;
    }

    /**
     * Determines whether an object is "equal to" this word.
     * 
     * @param obj an object
     * @return {@code true} if {@code obj} is equal to this word
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof Word word)
            return Arrays.equals(this.events, word.events);
        else
            return false;
    }

    /**
     * Returns a hash code for this word.
     * 
     * @return a hash code for this word
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(events);
    }

    /**
     * Returns an iterator over the events of this word.
     * 
     * @return an iterator
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < events.length;
            }

            @Override
            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return events[index++];
            }
        };
    }

    /**
     * Returns the string representation of this word.
     * 
     * @return the string representation of this word
     */
    @Override
    public String toString() {
        return Arrays.toString(events);
    }

    /**
     * Lexicographically compares this word to the specified word.
     * 
     * @param other the other word
     * @return a negative integer, zero, or a positive integer as this word
     *         is lexicographically less than, equal to, or greater than
     *         the specified word
     * 
     * @throws NullPointerException if specified word is {@code null}
     */
    @Override
    public int compareTo(Word other) {
        Objects.requireNonNull(other);
        for (int i = 0; i < Math.min(this.length(), other.length()); i++) {
            if (!Objects.equals(this.getEventAt(i), other.getEventAt(i))) {
                return this.getEventAt(i).compareTo(other.getEventAt(i));
            }
        }
        return Integer.compare(this.length(), other.length());
    }
}
