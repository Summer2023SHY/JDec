package com.github.automaton.automata;

import java.util.*;

/**
 * Represents a vector of {@link State}s. A {@code StateVector} is
 * treated as a {@link State} in
 * {@link Automaton#synchronizedComposition(java.io.File, java.io.File)
 * synchronized decomposition}, and the {@code j}-th component in this vector
 * stores the state w.r.t. the controller {@code j}.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
public class StateVector extends State implements Iterable<State> {
    private transient List<State> states;

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
        setID(Automaton.combineIDs(stateIDs, maxID));
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

    /**
     * Returns an iterator over the states stored in this vector.
     * @return an Iterator
     */
    @Override
    public Iterator<State> iterator() {
        return states.iterator();
    }
}
