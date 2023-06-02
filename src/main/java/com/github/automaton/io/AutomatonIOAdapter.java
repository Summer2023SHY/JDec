package com.github.automaton.io;

import java.io.*;

import com.github.automaton.automata.*;
import com.github.automaton.io.json.AutomatonJsonFileAdapter;
import com.github.automaton.io.legacy.AutomatonBinaryFileAdapter;

/**
 * An I/O wrapper for various file formats that represent an automaton.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public interface AutomatonIOAdapter {

    public static enum DataTypes {
        BINARY(AutomatonBinaryFileAdapter.class, "hdr"),
        JSON(AutomatonJsonFileAdapter.class, "json");

        private transient Class<? extends AutomatonIOAdapter> implClass;
        private String extension;

        private DataTypes(Class<? extends AutomatonIOAdapter> implClass, String extension) {
            this.implClass = implClass;
            this.extension = extension;
        }
    }

    /**
     * Returns the file that this wrapper wraps.
     * @return a file
     */
    public File getFile();

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
