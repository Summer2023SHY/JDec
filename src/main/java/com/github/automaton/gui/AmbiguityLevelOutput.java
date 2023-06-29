package com.github.automaton.gui;

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
import java.util.*;

import javax.swing.*;

import org.apache.commons.lang3.tuple.Pair;

import com.github.automaton.automata.*;
import com.github.automaton.gui.util.AmbiguityLevelTable;

/**
 * Displays the calculated ambiguity levels from observability tests.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.0
 * 
 * @see Automaton#testObservability()
 */
public class AmbiguityLevelOutput extends JDialog {

    /** Reference to the {@link JDec} instance that owns this popup */
    private JDec gui;
    private boolean result;
    private List<AmbiguityData> data;
    private JTable dataTable;

    /**
     * Constructs a new {@code AmbiguityLevelOutput}.
     * 
     * @param gui a reference to the {@link JDec} instance that owns
     * this popup
     * @param title the title for this popup
     * @param data the result from {@link Automaton#testObservability(boolean)}
     */
    public AmbiguityLevelOutput(JDec gui, String title, Pair<Boolean, List<AmbiguityData>> data) {
        super(gui, true);
        this.gui = gui;
        this.result = data.getLeft();
        this.data = data.getRight();
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
        
        dataTable = new JTable(new AmbiguityLevelTable(data));
        JOptionPane resultPane = new JOptionPane("The system is " + (this.result ? "" : " not ") + "observable.",
                JOptionPane.INFORMATION_MESSAGE);
        resultPane.addPropertyChangeListener(e -> {
            if (AmbiguityLevelOutput.this.isVisible() && e.getSource() == resultPane
                    && Objects.equals(e.getPropertyName(), JOptionPane.VALUE_PROPERTY)) {
                AmbiguityLevelOutput.this.setVisible(false);
            }
        });
        Box box = Box.createVerticalBox();
        box.add(resultPane);
        box.add(Box.createVerticalStrut(10));
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                /*
                 * Sets screen location in the center of the screen
                 * (only works after calling pack)
                 */

                setLocationRelativeTo(gui);

                /* Update title */

                setTitle(title);

                /* Show screen */

                setVisible(true);

            }
        });

    }
}
