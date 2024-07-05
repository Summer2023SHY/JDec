/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.github.automaton.automata.Automaton;
import com.github.automaton.io.legacy.AutomatonBinaryFileAdapter;

/**
 * Displays a popup box which allows the user to specify the properties of a
 * random
 * automaton that they would like generated. The appropriate actions are also
 * triggered when the user presses the "Generate" button in the popup.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class RandomAutomatonPrompt extends JDialog {

    /* CLASS CONSTANTS */

    // Default values
    private static int nControllersDefault = 2;
    private static int nEventsDefault = 3;
    private static int nStatesDefault = 7;
    private static int minTransitionsDefault = 1;
    private static int maxTransitionsDefault = 3;
    private static int nBadTransitionsDefault = 2;

    /* INSTANCE VARIABLES */

    public boolean isDisposed = false;
    private JDec gui;
    private JSpinner nControllers, nEvents, nStates, minTransitions, maxTransitions, nBadTransitions;

    /* CONSTRUCTOR */

    /**
     * Construct a RandomAutomatonPrompt object.
     * 
     * @param gui A reference to the GUI which will be receiving the generated
     *            automaton request.
     **/
    public RandomAutomatonPrompt(JDec gui) {

        super(gui, true);

        this.gui = gui;

        interruptThreadOnClose();

        addComponents();
        setGUIproperties();

    }

    /* METHODS */

    /**
     * Add all of the components to the window.
     **/
    private void addComponents() {

        /* Setup */

        setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        /* Number of controllers */

        JLabel nControllersLabel = new JLabel(" # Controllers:");
        c.gridx = 0;
        c.gridy = 0;
        add(nControllersLabel, c);

        nControllers = new JSpinner(
                new SpinnerNumberModel(nControllersDefault, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
        c.gridx = 1;
        c.gridy = 0;
        add(nControllers, c);

        /* Number of events */

        JLabel nEventsLabel = new JLabel(" # Events:");
        c.gridx = 0;
        c.gridy = 1;
        add(nEventsLabel, c);

        nEvents = new JSpinner(
                new SpinnerNumberModel(nEventsDefault, 0, AutomatonBinaryFileAdapter.MAX_EVENT_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 1;
        add(nEvents, c);

        /* Number of states */

        JLabel nStatesLabel = new JLabel(" # States:");
        c.gridx = 0;
        c.gridy = 2;
        add(nStatesLabel, c);

        nStates = new JSpinner(new SpinnerNumberModel(nStatesDefault, 2, 1000, 1)); // The observability test is too
                                                                                    // expensive to deal with large
                                                                                    // automata, so it has been
                                                                                    // arbitrarily capped
        c.gridx = 1;
        c.gridy = 2;
        add(nStates, c);

        /* Number of transitions */

        JLabel minTransitionsLabel = new JLabel(" Min. # Transitions per State:");
        c.gridx = 0;
        c.gridy = 3;
        add(minTransitionsLabel, c);

        minTransitions = new JSpinner(new SpinnerNumberModel(minTransitionsDefault, 0,
                AutomatonBinaryFileAdapter.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 3;
        add(minTransitions, c);

        JLabel maxTransitionsLabel = new JLabel(" Max. # Transitions per State:");
        c.gridx = 0;
        c.gridy = 4;
        add(maxTransitionsLabel, c);

        maxTransitions = new JSpinner(new SpinnerNumberModel(maxTransitionsDefault, 1,
                AutomatonBinaryFileAdapter.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 4;
        add(maxTransitions, c);

        /* Bad transitions */

        JLabel nBadTransitionsLabel = new JLabel(" # Bad Transitions:");
        c.gridx = 0;
        c.gridy = 5;
        add(nBadTransitionsLabel, c);

        nBadTransitions = new JSpinner(new SpinnerNumberModel(nBadTransitionsDefault, 0, Integer.MAX_VALUE, 1));
        c.gridx = 1;
        c.gridy = 5;
        add(nBadTransitions, c);

        /* Progress indicator */

        final JLabel progressIndicator = new JLabel("Starting...", SwingConstants.CENTER);
        progressIndicator.setVisible(false);
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 2;
        add(progressIndicator, c);

        /* Cancel button */

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(
                e -> RandomAutomatonPrompt.this.dispose());
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 1;
        add(cancelButton, c);

        /* Generate button */

        final JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> {

            final int nEventsValue = (Integer) nEvents.getValue();
            final int nStatesValue = (Integer) nStates.getValue();
            final int minTransitionsValue = (Integer) minTransitions.getValue();
            final int maxTransitionsValue = (Integer) maxTransitions.getValue();
            final int nControllersValue = (Integer) nControllers.getValue();
            final int nBadTransitionsValue = (Integer) nBadTransitions.getValue();

            // Ensure that the min. # of transition is not larger than the max. # of
            // transitions
            if (minTransitionsValue > maxTransitionsValue) {
                gui.displayErrorMessage("Invalid Input",
                        "The minimum number of transitions per state cannot be larger than the maximum number of transitions per state.");
                return;
            }

            if (nBadTransitionsValue > nStatesValue * maxTransitionsValue) {
                gui.displayErrorMessage("Invalid Input",
                        "The number of bad transitions cannot be larger than the number of states multiplied by the maximum number of transitions per state.");
                return;
            }

            if (maxTransitionsValue > nEventsValue) {
                gui.displayErrorMessage("Invalid Input",
                        "The maximum number of transitions per state cannot be larger than the number of events since we are dealing with deterministic automata.");
                return;
            }

            progressIndicator.setVisible(true);
            cancelButton.setVisible(false);
            generateButton.setVisible(false);

            // Create a new thread since this can be a long task
            new Thread(() -> {
                gui.generateRandomAutomaton(
                        RandomAutomatonPrompt.this,
                        nEventsDefault = nEventsValue,
                        nStatesDefault = nStatesValue,
                        minTransitionsDefault = minTransitionsValue,
                        maxTransitionsDefault = maxTransitionsValue,
                        nControllersDefault = nControllersValue,
                        nBadTransitionsDefault = nBadTransitionsValue,
                        progressIndicator);
                RandomAutomatonPrompt.this.dispose();
            }).start();

        });
        c.gridx = 1;
        c.gridy = 8;
        add(generateButton, c);

    }

    /**
     * Set some default GUI Properties.
     **/
    private void setGUIproperties() {

        // Pack things in nicely
        pack();

        // Make it so that the user can not resize the popup box
        setResizable(false);

        // Sets screen location in the center of the screen (only works after calling
        // pack)
        setLocationRelativeTo(super.getOwner());

        // Update title
        setTitle("Generate Random Automaton");

        // Show screen
        setVisible(true);

    }

    /**
     * Add a window listener to this dialog box which interrupts its thread when it
     * is closed.
     **/
    private void interruptThreadOnClose() {

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // The method being executed in the thread is periodically checking to see if
                // the window has been disposed
                isDisposed = true;

            }
        });

    }

}
