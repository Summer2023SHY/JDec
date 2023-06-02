package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.collections4.*;


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

    public StateVector toStateVector() {
        return new StateVector(Arrays.asList(set.toArray(new State[0])), maxID);
    }

    private class StateSetTransitionIterator implements Iterator<Transition> {
        private Iterator<State> stateIterator;
        private Iterator<Transition> transitionIterator;
        private Set<Transition> returnedTransitions;
        private int controller;

        StateSetTransitionIterator(int controller) {
            this.controller = controller;
            stateIterator = StateSet.this.set.iterator();
            returnedTransitions = new HashSet<>();
            this.transitionIterator = IteratorUtils.<Transition>emptyIterator();
        }

        @Override
        public boolean hasNext() {
            return stateIterator.hasNext() || transitionIterator.hasNext();
        }

        @Override
        public Transition next() {
            do {
                while (!transitionIterator.hasNext()) {
                    transitionIterator = IteratorUtils.filteredIterator(
                        stateIterator.next().getTransitions().iterator(),
                        t -> {
                            if (t.getEvent().getVector().getLabelAtIndex(controller).equals("*")) {
                                return false;
                            }
                            return !returnedTransitions.contains(t);
                        }
                    );
                }
                Transition t =  transitionIterator.next();
                returnedTransitions.add(t);
                return t;
            } while (hasNext());
        }
    }

    @Override
    public int hashCode() {
        return toStateVector().hashCode();
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
