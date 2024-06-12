/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.Dimension;
import java.util.*;

import javax.swing.*;

import com.github.automaton.automata.*;
import com.github.automaton.gui.util.*;

/**
 * Displays the calculated ambiguity levels from observability tests.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 * 
 * @see Automaton#testObservability()
 */
public class AmbiguityLevelOutput extends JDialog {

    /** Reference to the {@link JDec} instance that owns this popup */
    private JDec gui;
    private List<AmbiguityData> data;
    private JTable dataTable;

    /**
     * Constructs a new {@code AmbiguityLevelOutput}.
     * 
     * @param gui a reference to the {@link JDec} instance that owns
     * this popup
     * @param title the title for this popup
     * @param data the result from {@link Automaton#testObservability(boolean)}
     * 
     * @throws NullPointerException if {@code data} is {@code null}
     */
    public AmbiguityLevelOutput(JDec gui, String title, List<AmbiguityData> data) {
        super(gui, true);
        this.gui = gui;
        this.data = Objects.requireNonNull(data);
        setResizable(false);
        buildComponents();
        setPreferredSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
        setGUIproperties(title);
        pack();
    }

    /**
     * Builds the GUI components of this {@code AmbiguityLevelOutput}.
     */
    private void buildComponents() {
        AmbiguityLevelTable ambLevelTable = new AmbiguityLevelTable(data);
        dataTable = new JTable(ambLevelTable);
        dataTable.setRowSorter(new AmbiguityLevelTableRowSorter(ambLevelTable));
        Box box = Box.createVerticalBox();
        box.add(new JScrollPane(dataTable));
        add(box);

    }

    /**
     * Set some default GUI Properties.
     * 
     * @param title The title of the pop-up box
     **/
    private void setGUIproperties(final String title) {

        /*
         * This method is mostly copied from
         * AutomataExplorer.setGUIProperties(String)
         */

        SwingUtilities.invokeLater(() -> {

            /*
             * Sets screen location in the center of the screen
             * (only works after calling pack)
             */

            setLocationRelativeTo(gui);

            /* Update title */

            setTitle(title);

            /* Show screen */

            setVisible(true);

        });

    }
}
