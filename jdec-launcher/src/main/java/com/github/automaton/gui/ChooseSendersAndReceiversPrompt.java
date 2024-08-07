/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.*;

import com.github.automaton.automata.UStructure;

/**
 * Used to display a pop-up which prompts the user to decide which controllers
 * are allowed to be senders and receivers, and then go on to generate all
 * applicable feasible protocols, displaying them in another window.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public abstract class ChooseSendersAndReceiversPrompt extends JDialog {

    /* INSTANCE VARIABLES */

    protected JDec gui;
    protected UStructure uStructure;
    protected JCheckBox[][] checkBoxes;

    private boolean buttonPressed = false;

    /* CONSTRUCTOR */

    /**
     * Construct a ChooseSendersAndReceiversPrompt object.
     * 
     * @param gui        A reference to the GUI which is being worked with
     * @param uStructure The UStructure that is being worked with
     * @param title      The title of the popup box
     * @param message    The text for the label to be displayed at the top of the
     *                   screen
     * @param buttonText The text to be place on the button
     **/
    public ChooseSendersAndReceiversPrompt(JDec gui, UStructure uStructure, String title, String message,
            String buttonText) {

        super(gui, true);

        this.gui = gui;
        this.uStructure = uStructure;

        addComponents(message, buttonText);

        setGUIproperties(title);

    }

    /** METHODS **/

    /**
     * Add all of the components to the window.
     * 
     * @param message    The message to be displayed at the top of the screen
     * @param buttonText The text to be place on the button
     **/
    private void addComponents(String message, String buttonText) {

        /* Setup */

        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

        /* Add Instructions */

        add(new JLabel(message));

        /* Add Checkboxes */

        add(createCheckBoxGrid());

        /* Add Button */

        final JButton button = new JButton(buttonText);
        button.addActionListener(e -> {

            if (buttonPressed)
                return;

            buttonPressed = true;

            // Needs to be called on the event dispatch thread, or it will not update in
            // time
            SwingUtilities.invokeLater(() -> {
                button.setEnabled(false);
            });

            // Perform the overridden action
            if (performAction()) {

                // Dispose of the dialog box if the action was completed
                dispose();

            } else {

                // Allow the user to press the button again if the action was not completed
                // successfully
                buttonPressed = false;
                SwingUtilities.invokeLater(() -> {
                    button.setEnabled(true);
                });

            }

        });
        add(button);

    }

    /* METHODS */

    /**
     * This action is performed once the user has selected senders and receivers
     * and moves on to the next step.
     * 
     * @return {@code true} if the action was completed (which will dispose of
     *         this dialog box), or {@code false} if the action should be
     *         allowed to happen again.
     **/
    protected abstract boolean performAction();

    /**
     * Create a container with a 2-D grid of checkboxes, with each column and row
     * corresponding to a specific
     * controller.
     * <p>
     * NOTE: This method stores a reference to each JCheckBox object in a 2-D array
     * instance variable.
     * 
     * @return The container of checkboxes
     **/
    private Container createCheckBoxGrid() {

        Container container = new Container();

        int nControllers = uStructure.getNumberOfControllers();
        checkBoxes = new JCheckBox[nControllers][nControllers];

        container.setLayout(new GridLayout(nControllers + 1, nControllers + 1));

        // Add titles at the top of each column
        container.add(new JLabel("S\\R", SwingConstants.CENTER));
        for (int x = 1; x <= nControllers; x++)
            container.add(new JLabel(Integer.toString(x), SwingConstants.CENTER));

        // For each row
        for (int y = 0; y < nControllers; y++) {

            // Add title for this row
            container.add(new JLabel(Integer.toString(y + 1), SwingConstants.CENTER));

            // For each column
            for (int x = 0; x < nControllers; x++) {
                checkBoxes[y][x] = new JCheckBox();
                checkBoxes[y][x].setEnabled(x != y);
                checkBoxes[y][x].setSelected(x != y);
                checkBoxes[y][x].setHorizontalAlignment(SwingConstants.CENTER);
                container.add(checkBoxes[y][x]);
            }

        }

        return container;

    }

    /**
     * Set some default GUI Properties.
     * 
     * @param title The title to be displayed on the dialog box
     **/
    private void setGUIproperties(String title) {

        /* Pack things in nicely */

        pack();

        /* Don't allow the screen to resize */

        setResizable(false);

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
