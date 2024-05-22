/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.input;

import java.util.Objects;

import com.github.automaton.automata.*;

/**
 * Converts {@link Automaton automata} to their respective "GUI input" format.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public interface AutomatonGuiInputGenerator<T extends Automaton> {

    /**
     * Creates and returns a new {@code AutomatonGuiInputGenerator} for the specified automaton.
     * 
     * @param automaton an automaton
     * @return a new input generator for the specified automaton
     * 
     * @throws IllegalArgumentException if argument is of invalid type
     * @throws NullPointerException if argument is {@code null}
     */
    public static AutomatonGuiInputGenerator<? extends Automaton> createGuiInputGenerator(Automaton automaton) {
        Objects.requireNonNull(automaton);
        switch (automaton.getType()) {
            case AUTOMATON:
                return new AutomatonGuiInputGeneratorImpl(automaton);
            case U_STRUCTURE:
            case PRUNED_U_STRUCTURE:
                return new UStructureGuiInputGeneratorImpl((UStructure) automaton);
            default:
                throw new IllegalArgumentException("Invalid automaton type: " + automaton.getType());
        }
    }

    /**
     * Refreshes the internally cached GUI inputs.
     */
    void refresh();

    /**
     * Returns the event GUI input code.
     * 
     * @return GUI input code for events
     **/
    String getEventInput();

    /**
     * Returns the state GUI input code.
     * 
     * @return the GUI input code for states
     **/
    String getStateInput();

    /**
     * Returns the transition GUI input code.
     * 
     * @return the GUI input code for transitions
     **/
    String getTransitionInput();

    /**
     * Returns the automaton that this generator is associated with.
     * 
     * @return an automaton
     */
    T getAutomaton();
}
