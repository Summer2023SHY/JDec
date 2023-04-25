package com.github.automaton.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class BodyAccessFile implements Closeable {
    private String bodyFileName;
    private File bodyFile;
    /** List each state in the automaton, with the transitions */
    private RandomAccessFile bodyRAFile;

    public BodyAccessFile(File bodyFile) throws FileNotFoundException {
        this.bodyFile = Objects.requireNonNull(bodyFile);
        this.bodyFileName   = this.bodyFile.getAbsolutePath();
        bodyRAFile = new RandomAccessFile(bodyFile, "rw");
    }

    public boolean exists() {
        return bodyFile.exists();
    }

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

    public void copyTo(File newBodyFile) throws IOException {
        Files.copy(bodyFile.toPath(), newBodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void copyFrom(File newFile) throws IOException {
        Objects.requireNonNull(newFile);
        if (!newFile.isFile()) {
            throw new IllegalArgumentException();
        }
        bodyRAFile.close();
        Files.copy(newFile.toPath(), bodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        bodyRAFile = new RandomAccessFile(bodyFile, "rw");
    }

    RandomAccessFile getRAFile() {
        return bodyRAFile;
    }

    public File getBodyFile() {
        return bodyFile;
    }

    @Override
    public void close() throws IOException {
        bodyRAFile.close();
    }
}
