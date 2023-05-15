package com.github.automaton.automata;

import java.util.*;

/**
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class StateVector extends State implements Iterable<State> {
    private List<State> states;

    public StateVector(List<State> states, long maxID) {
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

    public State getStateFor(int controller) {
        return states.get(controller);
    }

    public List<State> getStates() {
        return states;
    }

    @Override
    public Iterator<State> iterator() {
        return states.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return states.hashCode();
    }
}
