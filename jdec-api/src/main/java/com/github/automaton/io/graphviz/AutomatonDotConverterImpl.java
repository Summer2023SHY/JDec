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
 * The {@link Automaton}-specific implementation of an {@link AutomatonDotConverter}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
class AutomatonDotConverterImpl extends AbstractAutomatonDotConverter<Automaton> {

    protected AutomatonDotConverterImpl(Automaton automaton) {
        super(automaton);
    }

    @Override
    protected void addAdditionalNodeProperties(State state, MutableNode node) {
        return;
    }

    @Override
    protected void addAdditionalLinkProperties(Map<String, Attributes<? extends ForLink>> map) {
        for (TransitionData data : automaton.getBadTransitions()) {
            combineAttributesInMap(map, createKey(data), Style.DOTTED);
        }
    }

}
