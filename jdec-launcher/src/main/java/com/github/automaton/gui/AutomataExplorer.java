/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.*;
import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

import com.github.automaton.automata.Automaton;
import com.github.automaton.automata.NoInitialStateException;
import com.github.automaton.automata.State;
import com.github.automaton.automata.Transition;

/**
 * Used to display a pop-up that allows the user to interactively explore the
 * specified automaton.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class AutomataExplorer extends JDialog {

    /* INSTANCE VARIABLES */

    private JDec gui;
    private Automaton automaton;
    private Automaton invertedAutomaton;
    private long currentStateID;
    private State currentState;

    private JScrollPane scrollPane;

    /* CONSTRUCTOR */

    /**
     * Construct a FeasibleProtocolOutput object.
     * 
     * @param gui       A reference to the GUI
     * @param automaton The automaton that is being worked with
     * @param title     The title of the popup box
     * @throws NullPointerException    if the argument is {@code null}
     * @throws NoInitialStateException if the argument has no initial state
     **/
    public AutomataExplorer(JDec gui,
            Automaton automaton,
            String title) {

        super(gui, true);
        if (automaton == null) {
            this.dispose();
            throw new NullPointerException("Automaton to explore cannot be null");
        } else if (automaton.getInitialStateID() == 0L) {
            this.dispose();
            throw new NoInitialStateException("Automaton to explore has no initial state");
        }
        this.gui = gui;
        this.automaton = automaton;
        invertedAutomaton = automaton.invert();

        setGUIproperties(title);

        changeState(automaton.getInitialStateID());

    }

    /* METHODS */

    private JPanel getTransitionPanel() {

        JPanel transitionPanel = new JPanel();

        // Outgoing transitions
        transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        transitionPanel.add(new JLabel("Outgoing transitions: "));
        for (Transition t : currentState.getTransitions()) {
            final JButton button = new JButton(String.format(
                    "(%s, %s)", t.getEvent().toString(),
                    this.automaton.getState(t.getTargetStateID()).getLabel()));
            final long targetStateID = t.getTargetStateID();
            button.addActionListener(e -> {
                changeState(targetStateID);
                SwingUtilities.invokeLater(() -> button.setEnabled(false));
            });
            transitionPanel.add(button);
        }

        // Incoming transitions
        State currentStateInverted = invertedAutomaton.getState(currentStateID);
        transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        transitionPanel.add(new JLabel("Incoming transitions: "));
        for (Transition t : currentStateInverted.getTransitions()) {
            final JButton button = new JButton(String.format(
                    "(%s, %s)", t.getEvent().toString(),
                    this.automaton.getState(t.getTargetStateID()).getLabel()));
            final long targetStateID = t.getTargetStateID();
            button.addActionListener(e -> {
                changeState(targetStateID);
                SwingUtilities.invokeLater(() -> button.setEnabled(false));
            });
            transitionPanel.add(button);
        }

        transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));

        return transitionPanel;

    }

    /**
     * Add all of the components to the window.
     **/
    private void changeState(long newStateID) {

        if (scrollPane != null)
            remove(scrollPane);

        currentStateID = newStateID;
        currentState = automaton.getState(currentStateID);

        /* Setup */

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

        /* Add label to hold current state information */

        JLabel currentStateLabel = new JLabel("<html><b>" + currentState.toString() + "</b><br></html>");
        panel.add(currentStateLabel, BorderLayout.NORTH);

        /* Add section for transitions */

        JPanel transitionPanel = getTransitionPanel();
        transitionPanel.setLayout(new BoxLayout(transitionPanel, BoxLayout.Y_AXIS));
        panel.add(transitionPanel, BorderLayout.CENTER);

        /* Set up Button panel */

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        /* Add Jump To State button */

        JButton jumpToStateButton = new JButton("Jump To State");
        jumpToStateButton.addActionListener(e -> {
            String stateLabel = JOptionPane.showInputDialog("Enter the state's label:", StringUtils.EMPTY);
            if (stateLabel == null)
                return;
            try {
                changeState(AutomataExplorer.this.automaton.getStateID(stateLabel));
            } catch (NullPointerException npe) {
                JOptionPane.showMessageDialog(gui, "The state '" + stateLabel + "' was unable to be found.");
            }
        });
        buttonPanel.add(jumpToStateButton);

        /* Add dismiss button */

        JButton dismissedButton = new JButton("Dismiss");
        dismissedButton.addActionListener(e -> {
            AutomataExplorer.this.dispose();
        });
        buttonPanel.add(dismissedButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        scrollPane = new JScrollPane(panel);
        add(scrollPane);

        revalidate();
        pack();

    }

    /**
     * Set some default GUI Properties.
     * 
     * @param title The title of the pop-up box
     **/
    private void setGUIproperties(final String title) {

        SwingUtilities.invokeLater(() -> {

            /*
             * Sets screen location in the center of the screen (only works after calling
             * pack)
             */

            setLocationRelativeTo(gui);

            /* Update title */

            setTitle(title);

            /* Show screen */

            setVisible(true);

        });

    }

}
