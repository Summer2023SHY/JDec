/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.graphviz;

import java.io.*;
import java.util.Objects;

import com.github.automaton.automata.Automaton;
import com.github.automaton.automata.UStructure;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.model.MutableGraph;

/**
 * Converts {@link Automaton automata} to their respective GraphViz
 * representations.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public interface AutomatonDotConverter<T extends Automaton> {

    /**
     * Creates and returns a new {@code AutomatonDotConverter} for the specified automaton.
     * 
     * @param automaton an automaton
     * @return a new DOT converter for the specified automaton
     * 
     * @throws IllegalArgumentException if argument is of invalid type
     * @throws NullPointerException if argument is {@code null}
     */
    public static AutomatonDotConverter<? extends Automaton> createConverter(Automaton automaton) {
        Objects.requireNonNull(automaton);
        switch (automaton.getType()) {
            case AUTOMATON:
            case SUBSET_CONSTRUCTION:
                return new AutomatonDotConverterImpl(automaton);
            case U_STRUCTURE:
            case PRUNED_U_STRUCTURE:
                return new UStructureDotConverterImpl((UStructure) automaton);
            default:
                throw new IllegalArgumentException("Invalid automaton type: " + automaton.getType());
        }
    }

    /**
     * Creates and returns a new {@code AutomatonDotConverter} for the specified UStructure
     * that highlights the configurations related to the specified event.
     * 
     * @param uStructure a UStructure
     * @param eventLabel an event label
     * @return a new DOT converter for the specified automaton
     * 
     * @throws NullPointerException if either one of the arguments is {@code null}
     */
    public static AutomatonDotConverter<UStructure> createEventSpecificConverter(UStructure uStructure, String eventLabel) {
        return new EventSpecificUStructureDotConverter(uStructure, eventLabel);
    }

    /**
     * Converts the internally stored automaton to its graphical representation.
     * 
     * @param outputFileName the file name for the generated image
     * @return {@code true} if the output was successfully generated
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    boolean generateImage(String outputFileName);

    /**
     * Converts the internally stored automaton to a {@link MutableGraph}.
     * 
     * @return a graph representation of the automaton
     */
    MutableGraph generateGraph();

    /**
     * Exports the internally stored automaton as a Graphviz-exportable format.
     * 
     * @param outputFileName the name of the exported file
     * @param format         the file format to export with
     * @return the exported file
     * 
     * @throws IllegalArgumentException if {@code outputFileName} has a file
     *                                  extension that is not consistent with
     *                                  {@code format}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IOException              If an I/O error occurs
     */
    File export(String outputFileName, Format format) throws IOException;

    /**
     * Exports the internally stored automaton to a file.
     * 
     * @param file the destination file to export this automaton to
     * @return the exported file
     * 
     * @throws NullPointerException     if argument is {@code null}
     * @throws IllegalArgumentException if argument is using an unsupported
     *                                  file extension
     * @throws IOException              if an I/O error occurs
     * 
     */
    File export(File file) throws IOException;
}
