/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.AbstractPanInteractor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.UncheckedException;

import com.github.automaton.automata.*;
import com.github.automaton.gui.util.ImageLoader;
import com.github.automaton.gui.util.OverwriteCheckingFileChooser;
import com.github.automaton.gui.util.bipartite.BipartiteGraphExport;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

/**
 * A popup for displaying event-specific view
 * of a UStructure.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
class BipartiteGraphView extends JFrame {

    private JSVGCanvas canvas;
    private Automaton automaton;
    private JComboBox<String> eventComboBox;

    BipartiteGraphView(Automaton automaton) {
        this.automaton = automaton;
        setTitle("Show Bipartite Graphs");
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
        c.insets = new Insets(0, 8, 0, 0);
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        container.add(eventInputLabel, c);

        // Controller input spinner
        eventComboBox = new JComboBox<>(getControllableEventLabels(automaton).toArray(String[]::new));
        c.insets = new Insets(0, 0, 0, 0);
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        container.add(eventComboBox, c);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            try {
                String eventLabel = getSelectedEventLabel();
                var graph = BipartiteGraphExport.generateBipartiteGraph(getAutomaton(), eventLabel);
                File targetTempFile = File.createTempFile("bipartite_graph_",
                        FilenameUtils.EXTENSION_SEPARATOR_STR + Format.SVG.fileExtension);
                Graphviz.fromGraph(graph).render(Format.SVG).toFile(targetTempFile);
                canvas.setSVGDocument(ImageLoader.loadSVGFromFile(targetTempFile));
                setTitle(String.format("Bipartite graph (Event: %s)", eventLabel));
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

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new OverwriteCheckingFileChooser() {
                    @Override
                    protected JDialog createDialog(Component parent) {
                        JDialog dialog = super.createDialog(BipartiteGraphView.this);
                        dialog.setModal(true);
                        return dialog;
                    }
                };

                fileChooser.setDialogTitle("Output bipartite graph image");

                /* Filter files */

                fileChooser.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter dotFilter = new FileNameExtensionFilter("dot files",
                        "dot");
                FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("png files",
                        "png");
                fileChooser.addChoosableFileFilter(dotFilter);
                fileChooser.addChoosableFileFilter(pngFilter);

                /* Begin at the most recently accessed directory */

                if (JDec.instance().currentDirectory != null)
                    fileChooser.setCurrentDirectory(JDec.instance().currentDirectory);

                /* Prompt user to select a filename */

                int result = fileChooser.showSaveDialog(null);

                /* No file was selected */

                if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
                    return;

                FileNameExtensionFilter usedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

                if (!FilenameUtils.isExtension(fileChooser.getSelectedFile().getName(),
                        usedFilter.getExtensions())) {
                    fileChooser.setSelectedFile(new File(
                            fileChooser.getSelectedFile().getAbsolutePath()
                                    + FilenameUtils.EXTENSION_SEPARATOR
                                    + usedFilter.getExtensions()[0]));
                }

                File dest = fileChooser.getSelectedFile();

                var graph = BipartiteGraphExport.generateBipartiteGraph(this.automaton, getSelectedEventLabel());
                dest.delete();
                Format format = Objects.equals(usedFilter.getExtensions()[0], "dot") ? Format.DOT : Format.PNG;
                Graphviz.fromGraph(graph).render(format).toFile(dest);
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

    Automaton getAutomaton() {
        return this.automaton;
    }

    String getSelectedEventLabel() {
        return eventComboBox.getItemAt(eventComboBox.getSelectedIndex());
    }

    /**
     * Generates the set of controllable event labels from the current UStructure.
     * 
     * @return the set of controllable event labels
     */
    static Set<String> getControllableEventLabels(Automaton automaton) {
        return automaton.getEvents().parallelStream().filter(event -> BooleanUtils.or(event.isControllable()))
                .map(event -> event.getLabel()).collect(Collectors.toSet());
    }

}
