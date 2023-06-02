package com.github.automaton.io;

import java.io.*;

import com.github.automaton.automata.*;
import com.github.automaton.io.json.AutomatonJsonFileAdapter;
import com.github.automaton.io.legacy.AutomatonBinaryFileAdapter;

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

    public File getFile();

    public Automaton getAutomaton();

    public void save() throws IOException;

    
}
