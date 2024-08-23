/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.AbstractPanInteractor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.UncheckedException;

import com.github.automaton.automata.*;
import com.github.automaton.gui.util.ImageLoader;
import com.github.automaton.io.graphviz.*;

import guru.nidi.graphviz.engine.Format;

/**
 * A popup for displaying event-specific view
 * of a UStructure.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
class EventSpecificView extends JFrame {

    private JSVGCanvas canvas;
    private UStructure uStructure;

    EventSpecificView(UStructure uStructure) {
        this.uStructure = uStructure;
        setTitle("Event-specific View of UStructure");
        setMinimumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
        buildComponents();
    }

    @SuppressWarnings("unchecked")
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

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            try {
                String eventLabel = comboBox.getItemAt(comboBox.getSelectedIndex());
                AutomatonDotConverter<UStructure> converter = AutomatonDotConverter.createEventSpecificConverter(this.uStructure, eventLabel);
                File targetTempFile = File.createTempFile("eventview_", FilenameUtils.EXTENSION_SEPARATOR_STR + Format.SVG.fileExtension);
                converter.export(targetTempFile);
                canvas.setSVGDocument(ImageLoader.loadSVGFromFile(targetTempFile));
                setTitle(String.format("Event-specific View of UStructure (Event: %s)", eventLabel));
            } catch (IOException ioe) {
                throw new UncheckedException(ioe);
            }
        });
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 2;
        c.gridy = 0;
        container.add(submitButton, c);

        canvas = new JSVGCanvas();

        // Use mouse for translation
        canvas.getInteractors().add(new AbstractPanInteractor() {
            @Override
            public boolean startInteraction(InputEvent ie) {
                int mods = ie.getModifiersEx();
                return ie.getID() == MouseEvent.MOUSE_PRESSED &&
                        (mods & InputEvent.BUTTON1_DOWN_MASK) != 0;
            }
        });

        canvas.addMouseWheelListener(e -> {
            Action action = null;
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0)
                    action = canvas.getActionMap().get(JSVGCanvas.ZOOM_IN_ACTION);
                else if (e.getWheelRotation() > 0)
                    action = canvas.getActionMap().get(JSVGCanvas.ZOOM_OUT_ACTION);
            } else {
                if (e.getWheelRotation() < 0) {
                    action = canvas.getActionMap().get(JSVGCanvas.FAST_SCROLL_UP_ACTION);
                } else if (e.getWheelRotation() > 0) {
                    action = canvas.getActionMap().get(JSVGCanvas.FAST_SCROLL_DOWN_ACTION);
                }
            }
            if (action != null)
                action.actionPerformed(null);
        });

        add(container, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);

        // Pack things in nicely
        pack();

        // Sets screen location in the center of the screen (only works after calling
        // pack)
        setLocationRelativeTo(JDec.instance());

        // Show screen
        setVisible(true);
    }

    private Set<String> getControllableEventLabels() {
        JDec.AutomatonTab tab = JDec.instance().getCurrentTab();
        UStructure uStructure = (UStructure) tab.automaton;
        List<Event> controllableEvents = uStructure.getControllableEvents();
        Set<String> eventLabels = new HashSet<>();
        for (Event e : controllableEvents) {
            for (int i = 0; i < uStructure.getNumberOfControllers(); i++) {
                if (e.isControllable(i))
                    eventLabels.add(e.getVector().getLabelAtIndex(i + 1));
            }
        }
        return eventLabels;
    }

}
