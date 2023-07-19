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

import org.apache.commons.collections4.*;
import org.apache.commons.collections4.multimap.*;
import org.apache.commons.lang3.ObjectUtils;

/**
 * A set of states.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class StateSet extends State {
    /** Set of states */
    private transient SortedSet<State> set;
    /** Maximum value of the IDs */
    private transient long maxID;

    /** Private constructor */
    private StateSet() {}

    /**
     * Constructs a new {@code StateSet}.
     * 
     * @param set set of states that forms this {@code StateSet}
     * @param maxID the maximum value of the IDs in the specified set
     */
    public StateSet(Set<State> set, long maxID) {
        this.maxID = maxID;
        this.set = new TreeSet<State>(new Comparator<State>() {
            @Override
            public int compare(State o1, State o2) {
                if (Objects.equals(o1, o2)) {
                    return 0;
                } else {
                    return Long.compare(o1.getID(), o2.getID());
                }
            }
        });
        this.set.addAll(set);
        StateVector sv = toStateVector();
        setID(sv.getID());
        buildLabel();
    }

    /**
     * Label of {@code StateSet} is automatically generated and
     * cannot be modified.
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    final void setLabel(String label) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a state to this state set.
     * 
     * @param s the state to add to this set
     * 
     * @return {@code true} if the specified state was successfully added
     * to this {@code StateSet}; {@code false} otherwise
     */
    boolean add(State s) {
        if (this.set.add(s)) {
            buildLabel();
            return true;
        } else return false;
    }

    /**
     * Removes a state from this state set.
     * 
     * @param s the state to remove from this set
     * @return {@code true} if the specified state was successfully removed
     * from this {@code StateSet}; {@code false} otherwise
     */
    boolean remove(State s) {
        if (this.set.remove(s)) {
            buildLabel();
            return true;
        } else return false;
    }

    /**
     * Removes a state with the matching label from this state set.
     * 
     * @param label the label of the state to remove from this set
     * @return {@code true} if there was a state with the matching label
     * to remove from this {@code StateSet}; {@code false} otherwise
     */
    boolean removeByLabel(String label) {
        if (Objects.requireNonNull(label).isEmpty()) {
            throw new IllegalArgumentException();
        }
        Iterator<State> stateIterator = set.iterator();
        while (stateIterator.hasNext()) {
            State s = stateIterator.next();
            String origLabel = s.getLabel().split("-")[0];
            if (Objects.equals(label, origLabel)) {
                stateIterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Builds label for this {@code StateSet}.
     */
    private void buildLabel() {
        List<String> labels = new ArrayList<>();
        for (State s : set) {
            labels.add(s.getLabel());
        }
        super.setLabel(new LabelVector(labels).toString());
    }

    /**
     * Gets observable outgoing transitions from this {@code StateSet} 
     * w.r.t. specified controller.
     * 
     * @param controller the controller
     * @return list of outgoing transitions observable by the specified
     * controller
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public List<Transition> getObservableTransitions(int controller) {
        return IteratorUtils.toList(new StateSetTransitionIterator(controller));
    }

    /**
     * Gets observable outgoing transitions from this {@code StateSet}
     * after grouping by same events.
     * 
     * @param controller the controller
     * @return map of events triggering transitions to collection of target states
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    public MultiValuedMap<Event, Long> groupAndGetObservableTransitions(int controller) {
        MultiValuedMap<Event, Long> groupedTransitions = new HashSetValuedHashMap<>();
        for (Transition s : getObservableTransitions(controller)) {
            groupedTransitions.put(s.getEvent(), s.getTargetStateID());
        }
        return groupedTransitions;
    }

    /**
     * Converts this {@code StateSet} to a {@link StateVector} and returns it.
     * @return a {@link StateVector} representation of this {@code StateSet}
     */
    public StateVector toStateVector() {
        return new StateVector(Arrays.asList(set.toArray(new State[0])), maxID);
    }

    /**
     * Iterator for observable transitions w.r.t. the specified controller.
     * 
     * @since 2.0
     */
    private class StateSetTransitionIterator implements Iterator<Transition> {
        private Iterator<State> stateIterator;
        private Iterator<Transition> transitionIterator;
        private Set<Transition> uniqueTransitions;
        private int controller;

        StateSetTransitionIterator(int controller) {
            if (controller < 0) throw new IndexOutOfBoundsException(controller);
            this.controller = controller;
            stateIterator = StateSet.this.set.iterator();
            uniqueTransitions = new LinkedHashSet<>();
            this.transitionIterator = IteratorUtils.<Transition>emptyIterator();
            while (stateIterator.hasNext()) {
                transitionIterator = IteratorUtils.filteredIterator(
                    stateIterator.next().getTransitions().iterator(),
                    t -> {
                        if (t.getEvent().getVector().getLabelAtIndex(this.controller).equals("*")) {
                            return false;
                        } else if (controller > 0 && !t.getEvent().isObservable(controller - 1)) {
                            return false;
                        }
                        return !uniqueTransitions.contains(t);
                    }
                );
                while (transitionIterator.hasNext()) {
                    uniqueTransitions.add(transitionIterator.next());
                }
            }
            this.transitionIterator = uniqueTransitions.iterator();
        }

        @Override
        public boolean hasNext() {
            return transitionIterator.hasNext();
        }

        @Override
        public Transition next() {
            return transitionIterator.next();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toStateVector().hashCode();
    }

    /**
     * Checks whether a {@code StateSet} contains all states that this
     * {@code StateSet} contains.
     * 
     * @param other a {@code StateSet} to check with
     * @return {@code true} if the specified {@code StateSet} contains
     * all states that this {@code StateSet} contains
     * 
     * @throws NullPointerException if argument is {@code null}
     * 
     * @see Set#containsAll(Collection)
     */
    public boolean containsAll(StateSet other) {
        return set.containsAll(other.set);
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        StateSet ss = new StateSet();
        ss.set = ObjectUtils.clone(this.set);
        ss.maxID = this.maxID;
        StateVector sv = ss.toStateVector();
        ss.setID(sv.getID());
        ss.buildLabel();
        for (Transition orig : this.getTransitions()) {
            ss.getTransitions().add(ObjectUtils.clone(orig));
        }
        return ss;
    }

    /**
     * Indicates whether an object is "equal to" this state set
     * 
     * @param other the reference object with which to compare
     * @return {@code true} if this state set is the same as the argument
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof StateSet)) {
            return super.equals(other);
        } else {
            StateSet ss = (StateSet) other;
            return this.set.containsAll(ss.set) && ss.set.containsAll(this.set);
        }
    }

}
