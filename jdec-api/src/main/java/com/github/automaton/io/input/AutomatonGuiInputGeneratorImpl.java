/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.input;

import org.apache.commons.lang3.StringUtils;

import com.github.automaton.automata.*;

/**
 * The {@link Automaton}-specific implementation of an {@link AutomatonGuiInputGenerator}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
final class AutomatonGuiInputGeneratorImpl extends AbstractAutomatonGuiInputGenerator<Automaton> {

    /**
     * Constructs a new GUI input generator.
     * 
     * @param automaton the automaton to associate this generator with
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    AutomatonGuiInputGeneratorImpl(Automaton automaton) {
        super(automaton);
    }

    @Override
    protected String getInputCodeForSpecialTransitions(TransitionData data) {
        return (getAutomaton().getBadTransitions().contains(data)) ? ",BAD" : StringUtils.EMPTY;
    }
    
}
