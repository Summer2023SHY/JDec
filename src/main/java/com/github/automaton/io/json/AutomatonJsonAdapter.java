package com.github.automaton.io.json;

import java.io.*;
import java.util.Objects;

import org.apache.commons.io.*;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;
import com.github.automaton.io.*;
import com.google.gson.*;
import com.google.gson.JsonParser;

public class AutomatonJsonAdapter implements AutomatonAdapter {

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
    public AutomatonJsonAdapter(File file) throws IOException {
        this(file, true);
    }

    public AutomatonJsonAdapter(File file, boolean load) throws IOException {
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

    public static <T extends Automaton> AutomatonJsonAdapter wrap(T automaton, File file) throws IOException {
        AutomatonJsonAdapter adapter = new AutomatonJsonAdapter(file, false);
        adapter.automaton = Objects.requireNonNull(automaton);
        FileUtils.touch(file);
        adapter.save();
        return adapter;
    }

    @Override
    public File getFile() {
        return file;
    }

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

    @Override
    public Automaton getAutomaton() {
        return automaton;
    }

    @Override
    public String toString() {
        return fileName;
    }


}