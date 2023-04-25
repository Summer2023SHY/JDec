package com.github.automaton.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * I/O handler for {@code .bdy} files.
 * 
 * @author Sung Ho Yoon
 */
public final class BodyAccessFile implements Closeable {
    private String bodyFileName;
    private File bodyFile;
    /** List each state in the automaton, with the transitions */
    private RandomAccessFile bodyRAFile;

    /**
     * Constructs a new {@code BodyAccessFile} for a given file
     * @param bodyFile a body file
     * @throws NullPointerException if argument is {@code null}
     * @throws FileNotFoundException if the given file object cannot be opened
     */
    public BodyAccessFile(File bodyFile) throws FileNotFoundException {
        this.bodyFile = Objects.requireNonNull(bodyFile);
        this.bodyFileName   = this.bodyFile.getAbsolutePath();
        bodyRAFile = new RandomAccessFile(bodyFile, "rw");
    }

    public boolean exists() {
        return bodyFile.exists();
    }

    /**
     * Clears the content of the body file
     * @return {@code true} the content is cleared; {@code false} otherwise.
     */
    public boolean clearFile() {
        try {
            bodyRAFile.close();
            bodyFile.delete();
            bodyFile.createNewFile();
            bodyRAFile = new RandomAccessFile(bodyFile, "rw");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copies the content of the underlying body file to a new file
     * 
     * @param newHeaderFile the target destination
     * @throws IOException if I/O error occurs
     */
    public void copyTo(File newBodyFile) throws IOException {
        Files.copy(bodyFile.toPath(), newBodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies and overwrites the underlying body file with the specified file
     * 
     * @param newHeaderFile the source data file
     * @throws IOException if I/O error occurs
     */
    public void copyFrom(File newFile) throws IOException {
        Objects.requireNonNull(newFile);
        if (!newFile.isFile()) {
            throw new IllegalArgumentException();
        }
        bodyRAFile.close();
        Files.copy(newFile.toPath(), bodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        bodyRAFile = new RandomAccessFile(bodyFile, "rw");
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
        return bodyFile;
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
        return this.bodyFileName;
    }
}
