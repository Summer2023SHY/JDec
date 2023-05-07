package com.github.automaton.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * I/O handler for {@code .bdy} files.
 * 
 * @author Sung Ho Yoon
 * @since 1.1
 */
public final class BodyAccessFile extends AutomatonAccessFile {

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
        bodyRAFile = new RandomAccessFile(bodyFile, "rw");
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
            bodyRAFile = new RandomAccessFile(getFile(), "rw");
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
        bodyRAFile = new RandomAccessFile(getFile(), "rw");
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
        bodyRAFile = new RandomAccessFile(getFile(), "rw");
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
