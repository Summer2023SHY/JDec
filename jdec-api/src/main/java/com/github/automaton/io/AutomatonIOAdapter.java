/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io;

import java.io.*;

import com.github.automaton.automata.*;

/**
 * An I/O wrapper for various file formats that represent an automaton.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public interface AutomatonIOAdapter {

    /**
     * Returns the file that this wrapper wraps.
     * @return a file
     */
    public File getFile();

    /**
     * Sets the automaton that this adapter wraps to the specified automaton.
     * @param automaton an automaton
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    public void setAutomaton(Automaton automaton);

    /**
     * Returns the automaton represented by the file this adapter wraps.
     * @return an automaton
     */
    public Automaton getAutomaton();

    /**
     * Saves this automaton to the underlying file.
     * @throws IOException if an I/O error occurs
     */
    public void save() throws IOException;

    
}
