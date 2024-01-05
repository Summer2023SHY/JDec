/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.awt.Dimension;
import java.io.OutputStream;

import javax.swing.*;

import com.github.automaton.gui.JDec;

/**
 * A text popup whose content is determined by the underlying
 * {@link OutputStream}.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class TextPopup extends JDialog {

    /** Reference to the {@link JDec} instance that owns this popup */
    private JDec gui;
    /** The underlying {@link OutputStream} */
    private SwingOutputStream output;

    /**
     * Constructs a new {@code TextPopup}.
     * 
     * @param gui a reference to the {@link JDec} instance that owns
     * this popup
     * @param title the title for this popup
     */
    public TextPopup(JDec gui, String title) {
        super(gui, true);
        this.gui = gui;
        this.output = new SwingOutputStream();
        setGUIproperties(title);
        add(new JScrollPane(output.getTextArea()));
        setPreferredSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
        setResizable(false);
        pack();
    }

    /**
     * Returns the underlying {@link OutputStream}.
     * @return the underlying {@link OutputStream}
     */
    public final OutputStream getOutputStream() {
        return output;
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
