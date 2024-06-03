/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.CommunicationData;
import com.github.automaton.automata.CommunicationRole;
import com.github.automaton.automata.UStructure;

/**
 * Generates all feasible protocols given the selected senders and receivers.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class GenerateFeasibleProtocolsPrompt extends ChooseSendersAndReceiversPrompt {

    /* CONSTRUCTOR */

    /**
     * Construct a GenerateFeasibleProtocolsPrompt object.
     * 
     * @param gui        A reference to the GUI
     * @param uStructure The U-Structure that is being worked with
     * @param title      The title of the popup box
     * @param message    The text for the label to be displayed at the top of the
     *                   screen
     **/
    public GenerateFeasibleProtocolsPrompt(JDec gui, UStructure uStructure, String title, String message) {

        super(gui, uStructure, title, message, "Generate All");

    }

    /* OVERRIDDEN METHOD */

    /** {@inheritDoc} */
    @Override
    protected boolean performAction() {

        List<Set<CommunicationData>> feasibleProtocols = generateFeasibleProtocols();

        if (feasibleProtocols.size() == 0) {

            gui.displayMessage("No Feasible Protocols",
                    "There were no feasible protocols found with the specified senders and receivers.",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;

        } else {

            // Hide this screen, since we will not need to go back to it
            SwingUtilities.invokeLater(() -> setVisible(false));

            // Display results in another window
            new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols",
                    " Here is the list of all feasible protocols: ");
            return true;

        }

    }

    /* METHOD */

    /**
     * Generate the feasible protocols based on the selected senders and receivers.
     * 
     * @return The list of feasible protocols
     **/
    protected List<Set<CommunicationData>> generateFeasibleProtocols() {

        // Generate list of communications which are still allowed based on which boxes
        // the user selected
        List<CommunicationData> chosenCommunications = new ArrayList<CommunicationData>();

        outer: for (CommunicationData data : uStructure.getPotentialAndNashCommunications()) {

            int sender = data.getIndexOfSender();

            // Check for communication that isn't allowed
            for (int i = 0; i < data.roles.length; i++)
                if (data.roles[i] == CommunicationRole.RECEIVER && !checkBoxes[sender][i].isSelected())
                    continue outer;

            // If we got this far then we can add it
            chosenCommunications.add(data);

        }

        return uStructure.generateAllFeasibleProtocols(chosenCommunications, false);

    }

}
