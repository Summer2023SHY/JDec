/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.*;

/**
 * Used to display a pop-up which contains a list of feasible protocols.
 * The user has the ability to generate automata by applying any of the
 * protocols in the list.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class FeasibleProtocolOutput extends JDialog {

    /* INSTANCE VARIABLES */

    private JDec gui;
    private UStructure uStructure;
    private java.util.List<Set<CommunicationData>> feasibleProtocols;
    private JTextPane[] detailedProtocolText;

    /* CONSTRUCTOR */

    /**
     * Construct a FeasibleProtocolOutput object.
     * 
     * @param gui               A reference to the GUI which will be receiving
     *                          requests for new tabs
     * @param uStructure        The U-Structure that is being worked with
     * @param feasibleProtocols The list of protocols that are feasible
     * @param title             The title of the popup box
     * @param message           The text for the label to be displayed at the top of
     *                          the screen
     **/
    public FeasibleProtocolOutput(JDec gui,
            UStructure uStructure,
            java.util.List<Set<CommunicationData>> feasibleProtocols,
            String title,
            String message) {

        super(gui, true);

        this.gui = gui;
        this.uStructure = uStructure;
        this.feasibleProtocols = feasibleProtocols;

        addComponents(message);
        setGUIproperties(title);

    }

    /* METHODS */

    /**
     * Add all of the components to the window.
     * 
     * @param message The text for the label to be displayed at the top of the
     *                screen
     **/
    private void addComponents(String message) {

        /* Setup */

        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

        /* Add Instructions */

        add(new JLabel(message), BorderLayout.NORTH);

        /* Display feasible protocols */

        Container outerContainer = new Container();
        outerContainer.setLayout(new BoxLayout(outerContainer, BoxLayout.PAGE_AXIS));
        detailedProtocolText = new JTextPane[feasibleProtocols.size()];

        final boolean[] alreadyPressed = new boolean[feasibleProtocols.size()];

        for (int i = 0; i < feasibleProtocols.size(); i++) {

            Container containerForTextAndButton = new Container();
            containerForTextAndButton.setLayout(new FlowLayout());

            final Set<CommunicationData> protocol = feasibleProtocols.get(i);

            // Format title text differently if there is only one protocol
            if (feasibleProtocols.size() == 1)
                containerForTextAndButton.add(new JLabel(String.format(
                        "Feasible protocol has %d communications.",
                        protocol.size())));
            else
                containerForTextAndButton.add(new JLabel(String.format(
                        "Feasible protocol #%d has %d communications.",
                        i + 1,
                        protocol.size())));

            // Add a button to generate the U-Structure with this protocol
            final JButton button = new JButton("Generate Automaton");
            final int index = i;
            button.addActionListener(e -> {

                if (alreadyPressed[index])
                    return;

                // Prevent the user from pressing this button again later
                alreadyPressed[index] = true;
                EventQueue.invokeLater(() -> {
                    button.setEnabled(false);
                });

                // Apply the protocol, and place the generated Automaton in a new tab
                // String fileName = gui.getTemporaryFileName();
                // File headerFile = new File(fileName + ".hdr");
                // File bodyFile = new File(fileName + ".bdy");
                Automaton generatedAutomaton = uStructure.applyProtocol(protocol, true);
                gui.createTab(generatedAutomaton);

            });
            containerForTextAndButton.add(button);

            outerContainer.add(containerForTextAndButton);

            // Add text to a text pane and make it so that the user cannot edit it
            StringBuilder protocolText = new StringBuilder();
            for (CommunicationData data : protocol)
                protocolText.append(data.toString(uStructure) + System.lineSeparator());
            detailedProtocolText[i] = new JTextPane();
            detailedProtocolText[i].setText(protocolText.toString());
            detailedProtocolText[i].setEditable(false);

            // Build scroll pane
            Container innerContainer = new Container();
            innerContainer.setLayout(new BoxLayout(innerContainer, BoxLayout.PAGE_AXIS));
            innerContainer.add(detailedProtocolText[i]);
            JScrollPane innerScrollPane = new JScrollPane(innerContainer);
            innerScrollPane.removeMouseWheelListener(innerScrollPane.getMouseWheelListeners()[0]);
            outerContainer.add(innerScrollPane);

        }

        JScrollPane outerScrollPane = new JScrollPane(outerContainer);
        outerScrollPane.setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
        add(outerScrollPane, BorderLayout.CENTER);

        /* Add "Dismissed" Button */

        JButton dismissedButton = new JButton("Dismiss");
        dismissedButton.addActionListener(e -> {
            FeasibleProtocolOutput.this.dispose();
        });
        add(dismissedButton, BorderLayout.SOUTH);

    }

    /**
     * Set some default GUI Properties.
     * 
     * @param title The title of the pop-up box
     **/
    private void setGUIproperties(String title) {

        /* Pack things in nicely */

        pack();

        /*
         * Sets screen location in the center of the screen (only works after calling
         * pack)
         */

        setLocationRelativeTo(super.getOwner());

        /* Update title */

        setTitle(title);

        /* Show screen */

        setVisible(true);

    }

}
