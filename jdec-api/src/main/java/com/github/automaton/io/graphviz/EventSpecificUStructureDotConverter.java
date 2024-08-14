package com.github.automaton.io.graphviz;

import java.util.Objects;

import com.github.automaton.automata.*;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.MutableNode;

class EventSpecificUStructureDotConverter extends UStructureDotConverterImpl {

    private String event;

    protected EventSpecificUStructureDotConverter(UStructure automaton, String event) {
        super(automaton);
        Objects.requireNonNull(event);
        if (automaton.getEvent(event) == null) {
            throw new IllegalArgumentException("Invalid event: \"" + event + "\"");
        }
        this.event = event;
    }

    @Override
    protected void addAdditionalNodeProperties(State state, MutableNode node) {
        if (state.isEnablementStateOf(event)) {
            node.add(Color.GREEN3);
        } else if (state.isDisablementStateOf(event)) {
            node.add(Color.RED);
        }
        if (state.isIllegalConfigurationOf(event)) {
            node.add(Shape.DOUBLE_CIRCLE);
        }
    }
}
