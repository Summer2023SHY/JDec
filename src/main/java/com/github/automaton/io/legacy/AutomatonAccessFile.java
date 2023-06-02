package com.github.automaton.io.legacy;

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
import java.nio.file.*;
import java.util.Objects;

import org.apache.commons.io.file.FilesUncheck;
import org.apache.logging.log4j.*;

/**
 * I/O handler for automaton data files.
 * 
 * @author Sung Ho Yoon
 * @since 1,1
 */
public abstract class AutomatonAccessFile implements Closeable {

    private Logger logger;
    private String fileName;
    private File file;

    /**
     * Constructs a new {@code AutomatonAccessFile} with the given file
     * @param file an automaton data file
     * @throws NullPointerException if argument is {@code null}
     */
    AutomatonAccessFile(File file) throws NullPointerException {
        this.file = Objects.requireNonNull(file);
        this.fileName = this.file.getAbsolutePath();
        this.logger = LogManager.getLogger(this.getClass().getName() + "(" + this.file.getName() +")");
    }

    /**
     * Clears the content of the underlying data file
     * @return {@code true} if the content is cleared; {@code false} otherwise.
     */
    public abstract boolean clearFile();

    /**
     * Copies the content of the underlying data file to a new file
     * 
     * @param newFile the target destination
     * @throws IOException if I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    public void copyTo(File newFile) throws IOException {
        Files.copy(getFile().toPath(), Objects.requireNonNull(newFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Determines whether the underlying data file exists.
     * 
     * @return {@code true} if the underlying file exists
     */
    public final boolean exists() {
        return file.exists();
    }

    /**
     * Returns the underlying automaton data file.
     * @return the underlying automaton data file
     */
    public final File getFile() {
        return file;
    }

    /**
     * Returns the internally used logger. The name of this logger is
     * of the form {@code Classname (filename)}, where {@code Classname}
     * is specified by calling {@link Class#getName()} on {@link #getClass()}.
     * @return the internally used logger
     * 
     * @since 1.3
     */
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Checks whether some other object is "equal to" this {@code AutomatonAccessFile}.
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if argument is "equal to" this {@code AutomatonAccessFile}
     * 
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * @since 2.0
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj instanceof AutomatonAccessFile) {
            return FilesUncheck.isSameFile(this.file.toPath(), ((AutomatonAccessFile) obj).file.toPath());
        }
        else return false;
    }

    /**
     * Returns the string representation of this automaton data file
     * @return the string representation of this automaton data file
     */
    @Override
    public String toString() {
        return fileName;
    }

    /**
     * Closes this automaton data file and releases any system resource
     * associated with this.
     * 
     * @throws IOException if I/O error occurs
     */
    public abstract void close() throws IOException;
}
