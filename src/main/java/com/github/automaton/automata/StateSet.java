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


public class StateSet extends State {
    private SortedSet<State> set;
    private long maxID;

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
        this.set = Collections.unmodifiableSortedSet(this.set);
        StateVector sv = toStateVector();
        setID(sv.getID());
        buildLabel();
    }

    @Override
    final void setLabel(String label) {
        throw new UnsupportedOperationException();
    }

    private void buildLabel() {
        List<String> labels = new ArrayList<>();
        for (State s : set) {
            labels.add(s.getLabel());
        }
        super.setLabel(new LabelVector(labels).toString());
    }

    public List<Transition> getObservableTransitions(int controller) {
        return IteratorUtils.toList(new StateSetTransitionIterator(controller));
    }

    public MultiValuedMap<Event, Long> groupAndGetObservableTransitions(int controller) {
        MultiValuedMap<Event, Long> groupedTransitions = new HashSetValuedHashMap<>();
        for (Transition s : getObservableTransitions(controller)) {
            groupedTransitions.put(s.getEvent(), s.getTargetStateID());
        }
        return groupedTransitions;
    }

    public StateVector toStateVector() {
        return new StateVector(Arrays.asList(set.toArray(new State[0])), maxID);
    }

    private class StateSetTransitionIterator implements Iterator<Transition> {
        private Iterator<State> stateIterator;
        private Iterator<Transition> transitionIterator;
        private Set<Transition> uniqueTransitions;
        private int controller;

        StateSetTransitionIterator(int controller) {
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof StateSet)) {
            return false;
        }
        return CollectionUtils.isEqualCollection(this.set, ((StateSet) other).set);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
