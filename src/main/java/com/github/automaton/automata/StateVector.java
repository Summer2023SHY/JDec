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

import java.util.*;

/**
 * Represents a vector of {@link State}s. A {@code StateVector} is
 * treated as a {@link State} in
 * {@link Automaton#synchronizedComposition()
 * synchronized decomposition}, and the {@code j}-th component in this vector
 * stores the state w.r.t. the controller {@code j}.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
public class StateVector extends State implements Iterable<State> {
    private transient List<State> states;

    /**
     * Private constructor. This is used for {@link #clone() cloning}.
     * 
     * @param states list of internally stored states.
     * 
     * @since 2.0
     */
    private StateVector(List<State> states) {
        this.states = states;
    }

    /**
     * Constructs a new {@code StateVector}.
     * 
     * @param states list of states that forms this vector
     * @param maxID the maximum value of the IDs in the specified list
     * 
     * @throws NullPointerException if {@code states == null}
     * @throws IllegalArgumentException if any element of {@code states} is {@code null}
     */
    public StateVector(List<State> states, long maxID) {
        if (Objects.requireNonNull(states).contains(null)) {
            throw new IllegalArgumentException("Argument contains null element");
        }
        this.states = Collections.unmodifiableList(states);
        List<Long> stateIDs = new ArrayList<>();
        StringBuilder combinedLabelBuilder = new StringBuilder();
        for (State s : states) {
            stateIDs.add(s.getID());
            combinedLabelBuilder.append('_');
            combinedLabelBuilder.append(s.getLabel());
        }
        combinedLabelBuilder.deleteCharAt(0);
        setLabel(combinedLabelBuilder.toString());
        setID(Automaton.combineBigIDs(stateIDs, maxID).longValue());
    }

    /**
     * Gets the state for the specified controller.
     * 
     * @param controller the controller
     * @return the state w.r.t. the specified controller
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public State getStateFor(int controller) {
        if (controller < 0 || controller >= states.size()) {
            throw new IndexOutOfBoundsException(controller);
        }
        return states.get(controller);
    }

    /**
     * Returns the states stored in this vector as a list. The list returned
     * by this method is unmodifiable.
     * 
     * @return the states stored in this vector as a list
     */
    public List<State> getStates() {
        return states;
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        StateVector sv = new StateVector(states);
        sv.setLabel(this.getLabel());
        sv.setID(this.getID());
        return sv;
    }

    /**
     * Returns an iterator over the states stored in this vector.
     * @return an Iterator
     */
    @Override
    public Iterator<State> iterator() {
        return states.iterator();
    }
}
