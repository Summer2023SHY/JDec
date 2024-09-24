/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import static com.github.automaton.gui.EventSpecificView.getControllableEventLabels;

import java.awt.*;

import javax.swing.*;

import com.github.automaton.automata.UStructure;
import com.github.automaton.gui.util.*;

/**
 * Displays the control configurations in the current {@link UStructure}.
 *
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class ControlConfigDisplay extends JDialog {

    /**
     * Constructs a new {@code ControlConfigDisplay}.
     *
     * @param owner the frame that owns this window
     */
    public ControlConfigDisplay(Frame owner) {
        super(owner, true);
        buildComponents();
        SwingUtilities.invokeLater(() -> {
            setResizable(false);
            revalidate();
            pack();
            setLocationRelativeTo(owner);
            setVisible(true);
        });
    }

    private void buildComponents() {
        setLayout(new BorderLayout());

        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;

        /* Controller Input */

        // Controller input label
        JLabel eventInputLabel = new JLabel("Event label:");
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        container.add(eventInputLabel, c);

        // Controller input spinner
        JComboBox<String> comboBox = new JComboBox<>(getControllableEventLabels().toArray(String[]::new));
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        container.add(comboBox, c);

        JTable table = new JTable(0, 0);

        JScrollPane scrollPane = new JScrollPane(table);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            String eventLabel = comboBox.getItemAt(comboBox.getSelectedIndex());
            JDec.AutomatonTab tab = JDec.instance().getCurrentTab();
            UStructure uStructure = (UStructure) tab.automaton;
            var controlConfigTable = new ControlConfigTable(uStructure, eventLabel);
            table.setModel(controlConfigTable);
            table.setRowSorter(new ControlConfigTableRowSorter(controlConfigTable));
        });
        c.gridx += 1;
        container.add(submitButton, c);

        add(container, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

    }
}
