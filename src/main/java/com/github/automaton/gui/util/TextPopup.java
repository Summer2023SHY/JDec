package com.github.automaton.gui.util;

/* 
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
