/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.legacy;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;

import org.apache.commons.io.file.FilesUncheck;
import org.apache.logging.log4j.*;

/**
 * I/O handler for automaton data files.
 * 
 * @author Sung Ho Yoon
 * @since 1.1
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
     * Writes the specified bytes to the data file
     * @param data bytes to write to the header
     * @throws IOException if I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     * 
     * @see java.io.RandomAccessFile#write(byte[])
     * 
     * @since 2.0
     */
    public final void write(byte[] data) throws IOException {
        getRAFile().write(Objects.requireNonNull(data));
    }

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
     * Returns the underlying {@link RandomAccessFile}
     * @return the underlying {@link RandomAccessFile}
     * 
     * @since 2.0
     */
    /*
     * FOR INTERNAL USE ONLY! THIS METHOD IS
     * PACKAGE-PRIVATE FOR A REASON.
     */
    abstract RandomAccessFile getRAFile();

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
        else if (obj instanceof AutomatonAccessFile other) {
            return FilesUncheck.isSameFile(this.file.toPath(), other.file.toPath());
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
