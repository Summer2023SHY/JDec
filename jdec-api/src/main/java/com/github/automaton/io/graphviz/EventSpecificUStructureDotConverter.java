/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.graphviz;

import java.util.Objects;

import com.github.automaton.automata.*;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.MutableNode;


/**
 * The special type of {@link UStructureDotConverterImpl} that only highlights
 * states related to the specified event.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
class EventSpecificUStructureDotConverter extends UStructureDotConverterImpl {

    private String event;

    protected EventSpecificUStructureDotConverter(UStructure automaton, String event) {
        super(automaton);
        this.event = Objects.requireNonNull(event);
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
