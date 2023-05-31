package com.github.automaton.io;

import java.io.IOException;

import com.github.automaton.automata.*;
import com.github.automaton.io.json.AutomatonJsonAdapter;
import com.github.automaton.io.legacy.AutomatonBinaryAdapter;

public interface AutomatonAdapter {

    public static enum DataTypes {
        BINARY(AutomatonBinaryAdapter.class, "hdr"),
        JSON(AutomatonJsonAdapter.class, "json");

        private transient Class<? extends AutomatonAdapter> implClass;
        private String extension;

        private DataTypes(Class<? extends AutomatonAdapter> implClass, String extension) {
            this.implClass = implClass;
            this.extension = extension;
        }
    }

    

    public Automaton getAutomaton();

    public void save() throws IOException;

    
}
