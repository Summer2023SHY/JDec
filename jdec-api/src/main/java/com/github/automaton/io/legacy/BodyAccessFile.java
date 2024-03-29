/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.legacy;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.commons.io.RandomAccessFileMode;

/**
 * I/O handler for {@code .bdy} files.
 * 
 * @author Sung Ho Yoon
 * @since 1.1
 */
public final class BodyAccessFile extends AutomatonAccessFile {

    /**
     * Default file extension used by body files.
     * 
     * @since 2.0
     */
    public static final String EXTENSION = "bdy";

    /** List each state in the automaton, with the transitions */
    private RandomAccessFile bodyRAFile;

    /**
     * Constructs a new {@code BodyAccessFile} for a given file
     * @param bodyFile a body file
     * @throws NullPointerException if argument is {@code null}
     * @throws FileNotFoundException if the given file object cannot be opened
     */
    public BodyAccessFile(File bodyFile) throws FileNotFoundException {
        super(bodyFile);
        bodyRAFile = RandomAccessFileMode.READ_WRITE.create(bodyFile);
    }

    /**
     * Clears the content of the body file
     * @return {@code true} the content is cleared; {@code false} otherwise.
     */
    @Override
    public boolean clearFile() {
        try {
            bodyRAFile.close();
            getFile().delete();
            getFile().createNewFile();
            bodyRAFile = RandomAccessFileMode.READ_WRITE.create(getFile());
            return true;
        } catch (IOException e) {
            getLogger().catching(e);
            return false;
        }
    }

    /**
     * Copies the content of the underlying body file to a new file
     * 
     * @param newFile the target destination
     * @throws IOException if I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    @Override
    public void copyTo(File newFile) throws IOException {
        bodyRAFile.close();
        super.copyTo(newFile);
        bodyRAFile = RandomAccessFileMode.READ_WRITE.create(getFile());
    }

    /**
     * Copies and overwrites the underlying body file with the specified file
     * 
     * @param srcFile the source data file
     * @throws IllegalArgumentException if argument is not a normal file
     * @throws IOException if I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    public void copyFrom(File srcFile) throws IOException {
        Objects.requireNonNull(srcFile);
        if (!srcFile.isFile()) {
            throw new IllegalArgumentException(srcFile + "is not a normal file");
        }
        bodyRAFile.close();
        Files.copy(srcFile.toPath(), getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        bodyRAFile = RandomAccessFileMode.READ_WRITE.create(getFile());
    }

    /**
     * Returns the underlying {@link RandomAccessFile}
     * @return the underlying {@link RandomAccessFile}
     */
    RandomAccessFile getRAFile() {
        /*
         * FOR INTERNAL USE ONLY! THIS METHOD IS
         * PACKAGE-PRIVATE FOR A REASON.
         */
        return bodyRAFile;
    }

    /**
     * Returns the underlying file object
     * @return the underlying file object
     */
    public File getBodyFile() {
        return getFile();
    }

    /**
     * Closes this body access file and releases any system resource
     * associated with this.
     * 
     * @throws IOException if I/O error occurs
     */
    @Override
    public void close() throws IOException {
        bodyRAFile.close();
    }

    /**
     * Returns the string representation of this body access file
     * @return the string representation of this body access file
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
