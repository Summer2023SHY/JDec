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
