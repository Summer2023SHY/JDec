package com.github.automaton.io;

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
