/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.input;

import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;

/**
 * Abstract implementation of an {@link AutomatonGuiInputGenerator}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
abstract class AbstractAutomatonGuiInputGenerator<T extends Automaton> implements AutomatonGuiInputGenerator<T> {

    private static Logger logger = LogManager.getLogger();

    private transient final T automaton;
    private transient StringBuilder eventInputBuilder;
    private transient StringBuilder stateInputBuilder;
    private transient StringBuilder transitionInputBuilder;

    /**
     * Constructs a new GUI input generator.
     * 
     * @param automaton the automaton to associate this generator with
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    protected AbstractAutomatonGuiInputGenerator(T automaton) {
        this.automaton = Objects.requireNonNull(automaton);
    }

    /* GUI INPUT CODE GENERATION */

    /**
     * Refreshes the internally cached GUI inputs.
     */
    public void refresh() {

        generateEventInputForGUI();
        generateStateAndTransitionInputForGUI();

    }

    /**
     * Generates the GUI input code for the events.
     **/
    private void generateEventInputForGUI() {

        eventInputBuilder = new StringBuilder();

        int counter = 0;

        for (Event e : automaton.getEvents()) {

            // Label
            eventInputBuilder.append(e.getLabel());

            // Observability properties
            eventInputBuilder.append(",");
            for (int i = 0; i < automaton.getNumberOfControllers(); i++)
                eventInputBuilder.append(BooleanUtils.toString(e.isObservable(i), "T", "F"));

            // Controllability properties
            eventInputBuilder.append(",");
            for (int i = 0; i < automaton.getNumberOfControllers(); i++)
                eventInputBuilder.append(BooleanUtils.toString(e.isControllable(i), "T", "F"));

            // End of line character
            if (++counter < automaton.getEvents().size())
                eventInputBuilder.append(StringUtils.LF);

        }

    }

    /**
     * Generates the GUI input code for the events.
     **/
    private void generateStateAndTransitionInputForGUI() {

        stateInputBuilder = new StringBuilder();
        transitionInputBuilder = new StringBuilder();

        boolean firstTransitionInStringBuilder = true;

        for (long s = 1; s <= automaton.getNumberOfStates(); s++) {

            State state = automaton.getState(s);

            if (state == null) {
                logger.error("State could not be loaded.");
                continue;
            }

            // Place '@' before label if this is the initial state
            if (s == automaton.getInitialStateID())
                stateInputBuilder.append("@");

            // Append label and properties
            stateInputBuilder.append(state.getLabel());
            if (automaton.getType() == Automaton.Type.AUTOMATON)
                stateInputBuilder.append(BooleanUtils.toString(state.isMarked(), ",T", ",F"));

            // Add line separator after unless this is the last state
            if (s < automaton.getNumberOfStates())
                stateInputBuilder.append(StringUtils.LF);

            // Append all transitions
            for (Transition t : state.getTransitions()) {

                // Add line separator before unless this is the very first transition
                if (firstTransitionInStringBuilder)
                    firstTransitionInStringBuilder = false;
                else
                    transitionInputBuilder.append(StringUtils.LF);

                // Append transition
                transitionInputBuilder.append(
                        state.getLabel()
                                + "," + t.getEvent().getLabel()
                                + "," + automaton.getState(t.getTargetStateID()).getLabel());

                /* Append special transition information */

                TransitionData transitionData = new TransitionData(s, t.getEvent().getID(), t.getTargetStateID());
                String specialTransitionInfo = getInputCodeForSpecialTransitions(transitionData);

                if (!specialTransitionInfo.isEmpty())
                    transitionInputBuilder.append(":" + specialTransitionInfo.substring(1));

            }

        }

    }

    /**
     * Generates the GUI input code correlating with the special transition data for
     * the specified transition.
     * 
     * @param data transition data
     * 
     * @return input code for the special transition
     */
    protected abstract String getInputCodeForSpecialTransitions(TransitionData data);

    @Override
    public final String getEventInput() {

        if (eventInputBuilder == null)
            refresh();

        return eventInputBuilder.toString();

    }

    @Override
    public final String getStateInput() {

        if (stateInputBuilder == null)
            refresh();

        return stateInputBuilder.toString();

    }

    @Override
    public final String getTransitionInput() {

        if (transitionInputBuilder == null)
            refresh();

        return transitionInputBuilder.toString();

    }

    @Override
    public final T getAutomaton() {
        return automaton;
    }

}
