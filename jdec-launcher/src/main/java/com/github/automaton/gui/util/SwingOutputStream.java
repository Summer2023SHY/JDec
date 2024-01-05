/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.io.*;
import javax.swing.*;

/**
 * Implements an output stream in which the data is written to a
 * {@link JTextArea}.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
class SwingOutputStream extends OutputStream {

    /** The underlying {@link JTextArea}. */
    private JTextArea textArea;

    /**
     * Constructs a new {@code SwingOutputStream}.
     */
    SwingOutputStream() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
    }

    /** 
     * Writes the specified byte to this {@code SwingOutputStream}.
     * 
     * @param b the byte to be written
     */
    @Override
    public void write(int b) {
        textArea.append(String.valueOf((char) b));
        textArea.setCaretPosition(0);
    }

    /**
     * Writes {@code len} bytes from the specified byte array starting at offset
     * {@code off} to this {@code SwingOutputStream}.
     * 
     * @param b the byte array
     * @param off the start offset
     * @param len the number of bytes to write
     * 
     * @throws IndexOutOfBoundsException if {@code off} is negative,
     * {@code len} is negative, or {@code off > b.length - len}
     */
    @Override
    public void write(byte[] b, int off, int len) {
        textArea.append(new String(b, off, len));
        textArea.setCaretPosition(0);
    }

    /**
     * Returns the underlying {@link JTextArea}.
     * @return the underlying {@link JTextArea}
     */
    final JTextArea getTextArea() {
        return textArea;
    }

}
