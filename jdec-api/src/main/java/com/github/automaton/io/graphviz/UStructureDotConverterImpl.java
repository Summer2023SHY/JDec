/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.graphviz;

import java.util.Map;

import com.github.automaton.automata.*;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.MutableNode;

/**
 * The {@link UStructure}-specific implementation of an {@link AutomatonDotConverter}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
class UStructureDotConverterImpl extends AbstractAutomatonDotConverter<UStructure> {

    protected UStructureDotConverterImpl(UStructure automaton) {
        super(automaton);
    }

    @Override
    protected void addAdditionalNodeProperties(State state, MutableNode node) {
        if (state.isEnablementState()) {
            node.add(Color.GREEN3);
        } else if (state.isDisablementState()) {
            node.add(Color.RED);
        }
        if (state.isIllegalConfiguration()) {
            node.add(Shape.DOUBLE_CIRCLE);
        }
    }

    @Override
    protected void addAdditionalLinkProperties(Map<String, Attributes<? extends ForLink>> map) {
        for (TransitionData data : automaton.getUnconditionalViolations()) {
            combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.RED, Color.RED.font()));
        }

        for (TransitionData data : automaton.getConditionalViolations()) {
            combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.GREEN3, Color.GREEN3.font()));
        }

        for (TransitionData data : automaton.getPotentialCommunications()) {
            combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.BLUE, Color.BLUE.font()));
        }

        for (TransitionData data : automaton.getInvalidCommunications()) {
            combineAttributesInMap(map, createKey(data), Attributes.attrs(Style.DASHED, Color.PURPLE, Color.PURPLE.font()));
        }

        for (TransitionData data : automaton.getNashCommunications()) {
            combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.BLUE, Color.BLUE.font()));
        }

        for (TransitionData data : automaton.getNashCommunications()) {
            combineAttributesInMap(map, createKey(data), Style.DOTTED);
        }
    }

}
