/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.swing.*;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FilenameUtils;

import com.github.automaton.automata.Automaton;
import com.github.automaton.automata.incremental.*;
import com.github.automaton.gui.util.*;

/**
 * A prompt for selecting automata to be used in incremental observability test.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
class IncrementalObsAutomataSelectionPrompt extends JDialog {

    private AtomicBoolean selected = new AtomicBoolean(false);

    private List<AutomatonEntry> entries;

    private AbstractComboBoxModel<CounterexampleHeuristics> counterexampleHeuristics;
    private AbstractComboBoxModel<ComponentHeuristics> componentHeuristics;

    public IncrementalObsAutomataSelectionPrompt(Frame owner) {
        super(owner, true);
        buildComponents();
        SwingUtilities.invokeLater(() -> {
            setResizable(false);
            revalidate();
            pack();
            setLocationRelativeTo(owner);
        });
    }

    private void buildComponents() {

        setLayout(new GridBagLayout());

        this.entries = new ArrayList<AutomatonEntry>();
        Container entryBox = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(0, 8, 0, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        for (var tab : JDec.instance().getTabs()) {
            if (tab.type != Automaton.Type.AUTOMATON)
                continue;
            AutomatonEntry entry = new AutomatonEntry(tab);
            entryBox.add(entry, c);
            entries.add(entry);
        }
        JScrollPane pane = new JScrollPane(entryBox);
        setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1d;
        c.weighty = 1d;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        add(pane, c);

        JLabel counterexampleHeuristicLabel = new JLabel("Counterexample Heuristic");
        c.gridy = 1;
        c.weightx = 0.5d;
        c.weighty = 0d;
        c.gridwidth = 1;
        c.insets = new Insets(0, 8, 0, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        add(counterexampleHeuristicLabel, c);

        counterexampleHeuristics = new AbstractComboBoxModel<>(CounterexampleHeuristics.values()) {};

        var counterexampleHeuristicOptions = new JComboBox<>(counterexampleHeuristics);
        c.gridx = 1;
        add(counterexampleHeuristicOptions, c);

        JLabel componentHeuristicLabel = new JLabel("Component Heuristic");
        c.gridx = 0;
        c.gridy = 2;
        add(componentHeuristicLabel, c);

        componentHeuristics = new AbstractComboBoxModel<>(ComponentHeuristics.values()) {};

        var componentHeuristicOptions = new JComboBox<>(componentHeuristics);

        c.gridx = 1;
        add(componentHeuristicOptions, c);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(action -> {
            selected.set(true);
            IncrementalObsAutomataSelectionPrompt.this.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(action -> IncrementalObsAutomataSelectionPrompt.this.dispose());

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.5d;
        c.weighty = 0d;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(submitButton, c);

        c.gridx = 1;
        add(cancelButton, c);
    }

    public Set<Automaton> getPlants() {
        return entries.parallelStream().filter(AutomatonEntry::isSelectedAsPlant).map(entry -> entry.tab.automaton)
                .collect(Collectors.toSet());
    }

    public Set<Automaton> getSpecs() {
        return entries.parallelStream().filter(AutomatonEntry::isSelectedAsSpec).map(entry -> entry.tab.automaton)
                .collect(Collectors.toSet());
    }

    boolean userSelected() {
        return selected.get();
    }

    CounterexampleHeuristics getSelectedCounterexampleHeuristic() {
        return counterexampleHeuristics.getSelectedItem();
    }

    FilteredComponentIterableGenerator getSelectedComponentHeuristic() {
        return componentHeuristics.getSelectedItem();
    }

    static class AutomatonEntry extends JPanel {

        static enum SelectionType {
            DISABLED("Disabled"),
            PLANT("Plant"),
            SPECIFICATION("Specification");

            private final String str;

            SelectionType(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        private JDec.AutomatonTab tab;

        private final ButtonGroup group = new ButtonGroup();

        AutomatonEntry(JDec.AutomatonTab tab) {
            super(new BorderLayout());
            this.tab = tab;
            super.add(new JLabel(FilenameUtils.getBaseName(tab.ioAdapter.getFile().getAbsolutePath())),
                    BorderLayout.LINE_START);
            JPanel lineEnd = new JPanel();
            for (final SelectionType type : SelectionType.values()) {
                var radioButtton = new JRadioButton(type.toString());
                group.add(radioButtton);
                lineEnd.add(radioButtton);
                group.setSelected(radioButtton.getModel(), type == SelectionType.DISABLED);
            }
            super.add(lineEnd, BorderLayout.LINE_END);
        }

        public AbstractButton getSelectedButton() {
            for (var button : IteratorUtils.asIterable(group.getElements().asIterator())) {
                if (button.isSelected())
                    return button;
            }
            return null;
        }

        public JDec.AutomatonTab getTab() {
            return tab;
        }

        public boolean isDisabled() {
            return Objects.equals(getSelectedButton().getText(), SelectionType.DISABLED.toString());
        }

        public boolean isSelectedAsPlant() {
            return Objects.equals(getSelectedButton().getText(), SelectionType.PLANT.toString());
        }

        public boolean isSelectedAsSpec() {
            return Objects.equals(getSelectedButton().getText(), SelectionType.SPECIFICATION.toString());
        }
    }
}
