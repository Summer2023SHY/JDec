package com.github.automaton.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class HeaderAccessFile implements Closeable {

    /** The fixed amount of space needed to hold the main variables in the {@code .hdr} file, which apply to all automaton types. */
    public static final int HEADER_SIZE = 45; 

    private String headerFileName;
    private File headerFile;
    private RandomAccessFile headerRAFile; // Contains basic information about automaton, needed in order to read the bodyFile, as well as the events


    public HeaderAccessFile(File headerFile) throws FileNotFoundException {
        this.headerFile = Objects.requireNonNull(headerFile);
        this.headerFileName = this.headerFile.getAbsolutePath();
        headerRAFile = new RandomAccessFile(headerFile, "rw");
    }

    public boolean delete() {
        try {
            headerRAFile.close();
            return headerFile.delete();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] readHeaderBytes(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException(
                String.format("Invalid number of bytes: %d", length)
            );
        }
        byte[] buffer = new byte[length];
        headerRAFile.seek(0);
        if (headerRAFile.read(buffer) == -1) {
            throw new EOFException();
        }
        return buffer;
    }

    @Override
    public void close() throws IOException {
        headerRAFile.close();
    }

    public boolean exists() {
        return headerFile.exists();
    }

    public boolean isEmpty() throws IOException {
        return headerRAFile.length() == 0;
    }

    public void copy(File newHeaderFile) throws IOException {
        Files.copy(headerFile.toPath(), newHeaderFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void seek(long pos) throws IOException {
        headerRAFile.seek(pos);
    }

    public void write(byte[] data) throws IOException {
        headerRAFile.write(data);
    }

    public void trim() throws IOException {
        headerRAFile.setLength(headerRAFile.getFilePointer());
    }

    public File getHeaderFile() {
        return this.headerFile;
    }

    @Override
    public String toString() {
        return this.headerFileName;
    }
}
