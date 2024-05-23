/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.input;

import org.apache.commons.lang3.BooleanUtils;

import com.github.automaton.automata.*;

/**
 * The {@link UStructure}-specific implementation of an {@link AutomatonGuiInputGenerator}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
final class UStructureGuiInputGeneratorImpl extends AbstractAutomatonGuiInputGenerator<UStructure> {

    /**
     * Constructs a new GUI input generator.
     * 
     * @param automaton the U-structure to associate this generator with
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    UStructureGuiInputGeneratorImpl(UStructure uStructure) {
        super(uStructure);
    }

    @Override
    protected String getInputCodeForSpecialTransitions(TransitionData data) {

        StringBuilder strBuilder = new StringBuilder();

        if (getAutomaton().getUnconditionalViolations().contains(data))
            strBuilder.append(",UNCONDITIONAL_VIOLATION");

        if (getAutomaton().getConditionalViolations().contains(data))
            strBuilder.append(",CONDITIONAL_VIOLATION");

        // Search entire list since there may be more than one potential communication
        String identifier = (getAutomaton().getType() == Automaton.Type.U_STRUCTURE ? ",POTENTIAL_COMMUNICATION-"
                : ",COMMUNICATION-");
        for (CommunicationData communicationData : getAutomaton().getPotentialCommunications())
            if (data.equals(communicationData)) {
                strBuilder.append(identifier);
                for (CommunicationRole role : communicationData.roles)
                    strBuilder.append(role.getCharacter());
            }

        if (getAutomaton().getInvalidCommunications().contains(data))
            strBuilder.append(",INVALID_COMMUNICATION");

        // Search entire list since there may be more than one Nash communication
        for (NashCommunicationData communicationData : getAutomaton().getNashCommunications())
            if (data.equals(communicationData)) {
                strBuilder.append(",NASH_COMMUNICATION-");
                for (CommunicationRole role : communicationData.roles)
                    strBuilder.append(role.getCharacter());
                strBuilder.append("-" + communicationData.cost);
                strBuilder.append("-" + communicationData.probability);
            }

        // There is only supposed to be one piece of disablement data per transition
        for (DisablementData disablementData : getAutomaton().getDisablementDecisions())
            if (data.equals(disablementData)) {
                strBuilder.append(",DISABLEMENT_DECISION-");
                for (boolean b : disablementData.controllers)
                    strBuilder.append(BooleanUtils.toString(b, "T", "F"));
                break;
            }

        return strBuilder.toString();

    }
}
