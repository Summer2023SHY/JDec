/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.json;

import java.io.*;
import java.util.Objects;

import org.apache.commons.io.*;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;
import com.github.automaton.io.*;
import com.google.gson.*;

/**
 * A wrapper for automata represented as a JSON file.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class AutomatonJsonFileAdapter implements AutomatonIOAdapter {

    /** Default file extension used by JSON files. */
    public static final String EXTENSION = "json";

    private transient Logger logger;
    private String fileName;
    private transient File file;

    private Automaton automaton;

    private transient Gson gson;
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.disableHtmlEscaping();
        gson = gsonBuilder.create();
    }

    /**
     * Constructs a new {@code AutomatonAccessFile} with the given file
     * @param file an automaton data file
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    public AutomatonJsonFileAdapter(File file) throws IOException {
        this(file, true);
    }

    /**
     * Constructs a new {@code AutomatonAccessFile} with the given file
     * @param file an automaton data file
     * @param load whether or not to load data from the specified file
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    public AutomatonJsonFileAdapter(File file, boolean load) throws IOException {
        this.file = Objects.requireNonNull(file);
        this.fileName = this.file.getAbsolutePath();
        this.logger = LogManager.getLogger(this.getClass().getName() + "(" + this.file.getName() +")");
        if (load && !this.file.isFile()) {
            throw logger.throwing(new FileNotFoundException(file + " is not a file"));
        }
        if (load) {
            try (Reader reader = IOUtils.buffer(new FileReader(file))) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (!jsonElement.isJsonObject()) {
                    throw logger.throwing(new IllegalAutomatonJsonException("File does not contain a JSON object"));
                }
                JsonObject jsonObj = jsonElement.getAsJsonObject();
                this.automaton = Automaton.buildAutomaton(jsonObj);
            } catch (IOException ioe) {
                throw logger.throwing(ioe);
            }
        }
    }

    /**
     * Wraps an automaton so that it can be saved as a JSON file
     * 
     * @param <T> type of automaton
     * @param automaton automaton to wrap
     * @param file file to save data to
     * @return an {@code AutomatonJsonFileAdapter} that wraps the specified automaton
     * @throws IOException if an I/O error occurs
     */
    public static <T extends Automaton> AutomatonJsonFileAdapter wrap(T automaton, File file) throws IOException {
        AutomatonJsonFileAdapter adapter = new AutomatonJsonFileAdapter(file, false);
        adapter.automaton = Objects.requireNonNull(automaton);
        FileUtils.touch(file);
        adapter.save();
        return adapter;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile() {
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void save() throws IOException {
        try {
            FileUtils.delete(file);
            try (Writer writer = IOUtils.buffer(new FileWriter(file))) {
                gson.toJson(automaton.toJsonObject(), writer);
            }
        } catch (IOException ioe) {
            throw logger.throwing(ioe);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    @Override
    public void setAutomaton(Automaton automaton) {
        this.automaton = Objects.requireNonNull(automaton);
    }

    /** {@inheritDoc} */
    @Override
    public Automaton getAutomaton() {
        return automaton;
    }

    /**
     * Returns a string representation of this wrapper.
     * @return a string representation of this wrapper
     */
    @Override
    public String toString() {
        return fileName;
    }


}
