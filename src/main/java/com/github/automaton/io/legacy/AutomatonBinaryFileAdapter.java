package com.github.automaton.io.legacy;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import com.github.automaton.automata.*;
import com.github.automaton.io.AutomatonIOAdapter;
import com.github.automaton.io.json.JsonUtils;
import com.google.gson.*;

/**
 * Provides implementation of {@link AutomatonIOAdapter} that is compatible
 * with {@code .hdr} and {@code .bdy} files.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class AutomatonBinaryFileAdapter implements AutomatonIOAdapter, Closeable {

    /** The number of events that an automaton can hold by default. */
    public static final int DEFAULT_EVENT_CAPACITY = 255;

    /** The maximum number of events that an automaton can hold. */
    public static final int MAX_EVENT_CAPACITY = Integer.MAX_VALUE;

    /** The number of states that an automaton can hold by default. */
    public static final long DEFAULT_STATE_CAPACITY = 255;

    /** The maximum number of states that an automaton can hold. */
    public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;

    /**
     * The number of transitions that each state in an automaton can hold by
     * default.
     */
    public static final int DEFAULT_TRANSITION_CAPACITY = 1;

    /**
     * The maximum number of transitions that each state in an automaton can hold.
     */
    public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;

    /**
     * The number of characters that each state label in an automaton can hold by
     * default.
     */
    public static final int DEFAULT_LABEL_LENGTH = 1;

    /**
     * The maximum number of characters that each state label in an automaton can
     * hold.
     * 
     * @implNote This value was originally 100, but was increased drastically in
     *           order to accommodate long
     *           state vectors that are formed in the crush.
     **/
    public static final int MAX_LABEL_LENGTH = 100000;

    /**
     * The UTF-8 Charset used for state label encoding
     */
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private HeaderAccessFile haf;
    private BodyAccessFile baf;

    private Automaton automaton;

    /**
     * Constructs a new {@code AutomatonBinaryFileAdapter}.
     * 
     * @param headerFile the {@code .hdr} file
     * @param bodyFile   the {@code .bdy} file
     * 
     * @throws IOException if an I/O error occurs
     */
    public AutomatonBinaryFileAdapter(File headerFile, File bodyFile) throws IOException {
        this(headerFile, bodyFile, true);
    }

    /**
     * Constructs a new {@code AutomatonBinaryFileAdapter}.
     * 
     * @param headerFile the {@code .hdr} file
     * @param bodyFile   the {@code .bdy} file
     * @param load whether or not to load data from the specified file 
     * 
     * @throws IOException if an I/O error occurs
     */
    public AutomatonBinaryFileAdapter(File headerFile, File bodyFile, boolean load) throws IOException {

        haf = new HeaderAccessFile(headerFile);
        baf = new BodyAccessFile(bodyFile);

        if (load) {
            JsonObject automatonData = new JsonObject();
            parseHeaderFile(automatonData);
            this.automaton = Automaton.buildAutomaton(automatonData);
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
    public static <T extends Automaton> AutomatonBinaryFileAdapter wrap(T automaton, File headerFile, File bodyFile) throws IOException {
        AutomatonBinaryFileAdapter adapter = new AutomatonBinaryFileAdapter(headerFile, bodyFile, false);
        adapter.automaton = Objects.requireNonNull(automaton);
        adapter.save();
        return adapter;
    }

    /**
     * Parses the {@code .hdr} file and adds parsed data to a JSON object.
     * 
     * @param jsonObj the JSON object to add parsed data to
     * @throws IOException if an I/O error occurs
     */
    private void parseHeaderFile(JsonObject jsonObj) throws IOException {
        haf.seek(0);
        byte[] buffer = haf.readHeaderBytes(HeaderAccessFile.HEADER_SIZE);

        byte type = (byte) ByteManipulator.readBytesAsLong(buffer, 0, Byte.BYTES);
        long nStates = ByteManipulator.readBytesAsLong(buffer, 1, Long.BYTES);
        int eventCapacity = ByteManipulator.readBytesAsInt(buffer, 9, Integer.BYTES);
        long stateCapacity = ByteManipulator.readBytesAsLong(buffer, 13, Long.BYTES);
        int transitionCapacity = ByteManipulator.readBytesAsInt(buffer, 21, Integer.BYTES);
        int labelLength = ByteManipulator.readBytesAsInt(buffer, 25, Integer.BYTES);
        long initialState = ByteManipulator.readBytesAsLong(buffer, 29, Long.BYTES);
        int nControllers = ByteManipulator.readBytesAsInt(buffer, 37, Integer.BYTES);
        int nEvents = ByteManipulator.readBytesAsInt(buffer, 41, Integer.BYTES);

        jsonObj.addProperty("type", type);
        jsonObj.addProperty("nStates", nStates);
        jsonObj.addProperty("initialState", initialState);
        jsonObj.addProperty("nControllers", nControllers);

        List<Event> events = new ArrayList<>(nEvents);

        for (int e = 1; e <= nEvents; e++) {

            // Read properties
            buffer = haf.readHeaderBytes(nControllers * 2);
            boolean[] observable = new boolean[nControllers];
            boolean[] controllable = new boolean[nControllers];
            for (int i = 0; i < nControllers; i++) {
                observable[i] = (buffer[2 * i] == 1);
                controllable[i] = (buffer[(2 * i) + 1] == 1);
            }

            // Read the number of characters in the label
            buffer = haf.readHeaderBytes(Integer.BYTES);
            int eventLabelLength = ByteManipulator.readBytesAsInt(buffer, 0, Integer.BYTES);

            // Read each character of the label, building an array of characters
            buffer = haf.readHeaderBytes(eventLabelLength);

            // Create the event and add it to the list
            events.add(new Event(new String(buffer, UTF8_CHARSET), e, observable, controllable));

        }

        JsonUtils.addListPropertyToJsonObject(jsonObj, "events", events, Event.class);

        /*
         * This is where the .hdr content corresponding to the relevant automaton type
         * is read
         */

        readSpecialTransitionsFromHeader(jsonObj);

        /*
         * The automaton should have room for at least 1 transition per state (otherwise
         * our automaton will be pretty boring)
         */

        if (transitionCapacity < 1)
            transitionCapacity = 1;

        /*
         * The requested length of the state labels should not exceed the limit, nor
         * should it be non-positive
         */

        if (labelLength < 1)
            labelLength = 1;
        if (labelLength > MAX_LABEL_LENGTH)
            labelLength = MAX_LABEL_LENGTH;

        /*
         * The number of controllers should be greater than 0, but it should not exceed
         * the maximum
         */

        /* Calculate the amount of space needed to store each state ID */

        // Special case if the state capacity is not positive
        int nBytesPerStateID = stateCapacity < 1 ? 1 : 0;

        long temp = stateCapacity;

        while (temp > 0) {
            nBytesPerStateID++;
            temp >>= 8;
        }

        /*
         * Calculate the maximum number of states that we can have before we have to
         * allocate more space for each state's ID
         */

        stateCapacity = 1;

        for (int i = 0; i < nBytesPerStateID; i++)
            stateCapacity <<= 8;

        /*
         * Special case when the user gives a value between 2^56 - 1 and 2^64
         * (exclusive)
         */

        if (stateCapacity == 0)
            stateCapacity = MAX_STATE_CAPACITY;
        else
            stateCapacity--;

        /* Cap the state capacity */

        if (stateCapacity > MAX_STATE_CAPACITY)
            stateCapacity = MAX_STATE_CAPACITY;

        /* Calculate the amount of space needed to store each event ID */

        // Special case if the event capacity is not positive
        int nBytesPerEventID = eventCapacity < 1 ? 1 : 0;

        temp = eventCapacity;

        while (temp > 0) {
            nBytesPerEventID++;
            temp >>= 8;
        }

        /*
         * Calculate the maximum number of events that we can have before we have to
         * allocate more space for each event's ID
         */

        eventCapacity = 1;

        for (int i = 0; i < nBytesPerEventID; i++)
            eventCapacity <<= 8;

        /*
         * Special case when the user gives a value between 2^24 - 1 and 2^32
         * (exclusive)
         */

        if (eventCapacity == 0)
            eventCapacity = MAX_EVENT_CAPACITY;
        else
            eventCapacity--;

        /* Cap the event capacity */

        if (eventCapacity > MAX_EVENT_CAPACITY)
            eventCapacity = MAX_EVENT_CAPACITY;

        long nBytesPerState = calculateNumberOfBytesPerState(nBytesPerEventID, nBytesPerStateID, transitionCapacity,
                labelLength);

        Map<String, Number> properties = new HashMap<>();
        properties.put("eventCapacity", eventCapacity);
        properties.put("labelLength", labelLength);
        properties.put("nBytesPerStateID", nBytesPerStateID);
        properties.put("nBytesPerEventID", nBytesPerEventID);
        properties.put("stateCapacity", stateCapacity);
        properties.put("transitionCapacity", transitionCapacity);
        properties.put("nBytesPerState", nBytesPerState);

        parseBodyFile(jsonObj, events, properties);
    }

    /**
     * Reads special transitions from a {@code .hdr} file and adds read data to a
     * JSON object.
     * 
     * @param jsonObj the JSON object to add read data to
     * @throws IOException if an I/O error occurs
     */
    private void readSpecialTransitionsFromHeader(JsonObject jsonObj) throws IOException {
        Automaton.Type type = Automaton.Type.getType(jsonObj.getAsJsonPrimitive("type").getAsByte());
        switch (type) {
            case AUTOMATON:
                readAutomatonSpecialTransitions(jsonObj);
                break;
            case U_STRUCTURE:
            case PRUNED_U_STRUCTURE:
                readUStructureSpecialTransitions(jsonObj);
                break;
            default:
                throw haf.getLogger()
                        .throwing(new AutomatonException("Invalid automaton type: " + Objects.toString(type)));
        }
    }

    /**
     * Reads special transitions from a {@code .hdr} file for a normal automaton.
     * 
     * @param jsonObj the JSON object to add read data to
     * @throws IOException if an I/O error occurs
     * 
     * @see Automaton
     */
    private void readAutomatonSpecialTransitions(JsonObject jsonObj) throws IOException {
        byte[] buffer = haf.readHeaderBytes(Integer.BYTES);
        int nBadTransitions = ByteManipulator.readBytesAsInt(buffer, 0, Integer.BYTES);

        List<TransitionData> badTransitions = readTransitionDataFromHeader(nBadTransitions);

        JsonUtils.addListPropertyToJsonObject(jsonObj, "badTransitions", badTransitions, TransitionData.class);
    }

    /**
     * Reads special transitions from a {@code .hdr} file for a U-Structure.
     * 
     * @param jsonObj the JSON object to add read data to
     * @throws IOException if an I/O error occurs
     * 
     * @see UStructure
     */
    private void readUStructureSpecialTransitions(JsonObject jsonObj) throws IOException {
        byte[] buffer = haf.readHeaderBytes(28);
        int nUnconditionalViolations = ByteManipulator.readBytesAsInt(buffer, 0, Integer.BYTES);
        int nConditionalViolations = ByteManipulator.readBytesAsInt(buffer, 4, Integer.BYTES);
        int nPotentialCommunications = ByteManipulator.readBytesAsInt(buffer, 8, Integer.BYTES);
        int nInvalidCommunications = ByteManipulator.readBytesAsInt(buffer, 12, Integer.BYTES);
        int nNashCommunications = ByteManipulator.readBytesAsInt(buffer, 16, Integer.BYTES);
        int nDisablementDecisions = ByteManipulator.readBytesAsInt(buffer, 20, Integer.BYTES);
        int nSuppressedTransitions = ByteManipulator.readBytesAsInt(buffer, 24, Integer.BYTES);

        List<TransitionData> unconditionalViolations = readTransitionDataFromHeader(nUnconditionalViolations);
        List<TransitionData> conditionalViolations = readTransitionDataFromHeader(nConditionalViolations);
        List<CommunicationData> potentialCommunications = readCommunicationDataFromHeader(nPotentialCommunications,
                jsonObj);
        List<TransitionData> invalidCommunications = readTransitionDataFromHeader(nInvalidCommunications);
        List<NashCommunicationData> nashCommunications = readNashCommunicationDataFromHeader(nNashCommunications,
                jsonObj);
        List<DisablementData> disablementDecisions = readDisablementDataFromHeader(nDisablementDecisions, jsonObj);
        List<TransitionData> suppressedTransitions = readTransitionDataFromHeader(nSuppressedTransitions);

        JsonUtils.addListPropertyToJsonObject(jsonObj, "unconditionalViolations", unconditionalViolations,
                TransitionData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "conditionalViolations", conditionalViolations,
                TransitionData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "potentialCommunications", potentialCommunications,
                CommunicationData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "invalidCommunications", invalidCommunications,
                TransitionData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "nashCommunications", nashCommunications,
                NashCommunicationData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "disablementDecisions", disablementDecisions,
                DisablementData.class);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "suppressedTransitions", suppressedTransitions,
                TransitionData.class);
    }

    /**
     * A helper method to read a list of special transitions from the header file.
     *
     * @param nTransitions The number of transitions that need to be read
     * @param list         The list of transition data
     * @throws IOException If there was problems reading from file
     **/
    private List<TransitionData> readTransitionDataFromHeader(int nTransitions) throws IOException {

        /* Read from file */

        byte[] buffer = haf.readHeaderBytes(nTransitions * 20);
        int index = 0;

        List<TransitionData> list = new ArrayList<>(nTransitions);

        /* Add transitions to the list */

        for (int i = 0; i < nTransitions; i++) {

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, Integer.BYTES);
            index += Integer.BYTES;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            list.add(new TransitionData(initialStateID, eventID, targetStateID));

        }

        return list;

    }

    /**
     * A helper method to read a list of communication transitions from the header
     * file.
     *
     * @param nCommunications The number of communications that need to be read
     * @param list            The list of communication data
     * @throws IOException If there was problems reading from file
     **/
    private List<CommunicationData> readCommunicationDataFromHeader(int nCommunications,
            JsonObject jsonObj) throws IOException {

        int nControllers = jsonObj.getAsJsonPrimitive("nControllers").getAsInt();

        List<CommunicationData> list = new ArrayList<>(nCommunications);

        byte[] buffer = haf.readHeaderBytes(nCommunications * (20 + nControllers));
        int index = 0;

        for (int i = 0; i < nCommunications; i++) {

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, Integer.BYTES);
            index += Integer.BYTES;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            CommunicationRole[] roles = new CommunicationRole[nControllers];
            for (int j = 0; j < roles.length; j++)
                roles[j] = CommunicationRole.getRole(buffer[index++]);

            list.add(new CommunicationData(initialStateID, eventID, targetStateID, roles));

        }

        return list;

    }

    /**
     * A helper method to read a list of nash communication transitions from the
     * header file.
     *
     * @param nCommunications The number of communications that need to be read
     * @param list            The list of nash communication data
     * @throws IOException If there was problems reading from file
     **/
    private List<NashCommunicationData> readNashCommunicationDataFromHeader(int nCommunications,
            JsonObject jsonObj) throws IOException {

        int nControllers = jsonObj.getAsJsonPrimitive("nControllers").getAsInt();

        List<NashCommunicationData> list = new ArrayList<>(nCommunications);

        byte[] buffer = haf.readHeaderBytes(nCommunications * (36 + nControllers));
        int index = 0;

        for (int i = 0; i < nCommunications; i++) {

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, Integer.BYTES);
            index += Integer.BYTES;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            double cost = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES));
            index += Long.BYTES;

            double probability = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES));
            index += Long.BYTES;

            CommunicationRole[] roles = new CommunicationRole[nControllers];
            for (int j = 0; j < roles.length; j++)
                roles[j] = CommunicationRole.getRole(buffer[index++]);

            list.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));

        }

        return list;

    }

    /**
     * A helper method to read a list of disablement decisions from the header file.
     *
     * @param nDisablements The number of disablement decisions that need to be read
     * @param list          The list of disablement decisions
     * @throws IOException If there were any problems reading from file
     **/
    private List<DisablementData> readDisablementDataFromHeader(int nDisablements,
            JsonObject jsonObj) throws IOException {

        int nControllers = jsonObj.getAsJsonPrimitive("nControllers").getAsInt();

        List<DisablementData> list = new ArrayList<>(nDisablements);

        byte[] buffer = haf.readHeaderBytes(nDisablements * (20 + nControllers));
        int index = 0;

        for (int i = 0; i < nDisablements; i++) {

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, Integer.BYTES);
            index += Integer.BYTES;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, Long.BYTES);
            index += Long.BYTES;

            boolean[] controllers = new boolean[nControllers];
            for (int j = 0; j < controllers.length; j++)
                controllers[j] = (buffer[index++] == 1);

            list.add(new DisablementData(initialStateID, eventID, targetStateID, controllers));

        }

        return list;

    }

    /**
     * Calculate the amount of space required to store a state, given the specified
     * conditions.
     * 
     * @param newNBytesPerEventID   The number of bytes per event ID
     * @param newNBytesPerStateID   The number of bytes per state ID
     * @param newTransitionCapacity The transition capacity
     * @param newLabelLength        The maximum label length
     * @return The number of bytes needed to store a state
     **/
    private long calculateNumberOfBytesPerState(int newNBytesPerEventID, long newNBytesPerStateID,
            int newTransitionCapacity, int newLabelLength) {
        return 1 // To hold up to 8 boolean values (such as 'Marked' and 'Exists' status)
                + (long) newLabelLength // The state's labels
                + (long) newTransitionCapacity * (long) (newNBytesPerEventID + newNBytesPerStateID); // All of the
                                                                                                     // state's
                                                                                                     // transitions
    }

    /**
     * Parses the {@code .bdy} file and adds parsed data to a JSON object.
     * 
     * @param jsonObj the JSON object to add parsed data to
     * @throws IOException if an I/O error occurs
     */
    private void parseBodyFile(JsonObject jsonObj, List<Event> events, Map<String, Number> properties) {

        long nStates = jsonObj.getAsJsonPrimitive("nStates").getAsLong();

        long counter = 0; // Keeps track of blank states

        Gson gson = new Gson();

        Set<State> states = new LinkedHashSet<>();

        for (long s = 1; s <= nStates + counter; s++) {

            State state = StateIO.readFromFile(events, properties, baf, s);

            // Check for non-existent state
            if (state == null) {

                counter++;
                continue;

            } else {
                states.add(state);
            }

        } // for

        jsonObj.add("states", gson.toJsonTree(states, TypeUtils.parameterize(states.getClass(), State.class)));
    }

    @Override
    public Automaton getAutomaton() {
        return automaton;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the underlying header file
     */
    @Override
    public File getFile() {
        return getHeaderFile();
    }

    /**
     * Returns the header file this wrapper wraps.
     * 
     * @return a {@code .hdr} file
     */
    public File getHeaderFile() {
        return haf.getFile();
    }

    /**
     * Returns the body file this wrapper wraps.
     * 
     * @return a {@code .bdy} file
     */
    public File getBodyFile() {
        return baf.getFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() throws IOException {
        writeHeaderFile();
        writeBodyFile();
    }

    private void writeHeaderFile() throws IOException {

        haf.clearFile();

        byte[] buffer = new byte[HeaderAccessFile.HEADER_SIZE];

        // Type of automaton
        ByteManipulator.writeLongAsBytes(buffer, 0, getAutomaton().getType().getNumericValue(), Byte.BYTES);
        // Number of states
        ByteManipulator.writeLongAsBytes(buffer, 1, getAutomaton().getNumberOfStates(), Long.BYTES);
        // Event capacity
        ByteManipulator.writeLongAsBytes(buffer, 9, getAutomaton().getNumberOfEvents(), Integer.BYTES);
        // State capacity
        ByteManipulator.writeLongAsBytes(buffer, 13, getAutomaton().getNumberOfStates(), Long.BYTES);
        // Transition capacity per state
        ByteManipulator.writeLongAsBytes(buffer, 21, calculateTransitionCapacity(), Integer.BYTES);
        // State label length
        ByteManipulator.writeLongAsBytes(buffer, 25, calculateLabelLength(), Integer.BYTES);
        // Initial state
        ByteManipulator.writeLongAsBytes(buffer, 29, getAutomaton().getInitialStateID(), Long.BYTES);
        // Number of controllers
        ByteManipulator.writeLongAsBytes(buffer, 37, getAutomaton().getNumberOfControllers(), Integer.BYTES);
        // Number of events
        ByteManipulator.writeLongAsBytes(buffer, 41, getAutomaton().getNumberOfEvents(), Integer.BYTES);

        haf.seek(0);
        haf.write(buffer);

        for (Event e : getAutomaton().getEvents()) {

            // Fill the buffer
            buffer = new byte[(2 * getAutomaton().getNumberOfControllers()) + Integer.BYTES + e.getLabel().length()];

            // Read event properties (NOTE: If we ever need to condense the space required
            // to hold an event
            // in a file, we can place a property in each bit instead of each byte)
            int index = 0;
            for (int i = 0; i < getAutomaton().getNumberOfControllers(); i++) {
                buffer[index] = (byte) (e.isObservable()[i] ? 1 : 0);
                buffer[index + 1] = (byte) (e.isControllable()[i] ? 1 : 0);
                index += 2;
            }

            // Write the length of the label
            ByteManipulator.writeLongAsBytes(buffer, index, e.getLabel().length(), Integer.BYTES);
            index += Integer.BYTES;

            // Write characters of the label
            byte[] labelData = e.getLabel().getBytes(UTF8_CHARSET);
            System.arraycopy(labelData, 0, buffer, index, labelData.length);

            haf.write(buffer);

            writeSpecialTransitionsToHeader();

            /*
             * Trim the file so that there is no garbage at the end (removing events, for
             * example, shortens the .hdr file)
             */
            haf.trim();

        }
    }

    /**
     * Calculates transition capacity for the wrapped automaton.
     * 
     * @return the transition capacity
     */
    private int calculateTransitionCapacity() {
        int maxTransitions = 0;
        for (long i = 0L; i < getAutomaton().getNumberOfStates(); i++) {
            State s = getAutomaton().getState(i);
            maxTransitions = Math.max(maxTransitions, s.getNumberOfTransitions());
        }
        return maxTransitions;
    }

    /**
     * Calculates transition capacity for the wrapped automaton.
     * 
     * @return the transition capacity
     */
    private int calculateLabelLength() {
        int maxLabelLength = 0;
        for (long i = 0L; i < getAutomaton().getNumberOfStates(); i++) {
            State s = getAutomaton().getState(i);
            maxLabelLength = Math.max(maxLabelLength, s.getLabel().length());
        }
        return maxLabelLength;
    }

    private void writeSpecialTransitionsToHeader() throws IOException {
        switch (automaton.getType()) {
            case AUTOMATON:
                writeAutomatonSpecialTransitions();
                break;
            case U_STRUCTURE:
            case PRUNED_U_STRUCTURE:
                writeUStructureSpecialTransitions();
                break;
            default:
                throw new AutomatonException("Unknown type of automaton");
        }
    }

    /**
     * Write all of the special transitions to the header, which is relevant to this
     * particular automaton type.
     * 
     * @throws IOException If there were any problems writing to file
     **/
    private void writeAutomatonSpecialTransitions() throws IOException {

        /*
         * Write a number which indicates how many special transitions are in the file
         */

        byte[] buffer = new byte[Integer.BYTES];
        ByteManipulator.writeLongAsBytes(buffer, 0, getAutomaton().getBadTransitions().size(), Integer.BYTES);
        haf.write(buffer);

        /* Write special transitions to the .hdr file */

        writeTransitionDataToHeader(getAutomaton().getBadTransitions());

    }

    private void writeUStructureSpecialTransitions() throws IOException {
        UStructure uStructure = (UStructure) automaton;
        byte[] buffer = new byte[28];
        // Unconditional violations
        ByteManipulator.writeLongAsBytes(buffer, 0,
                readUStructureData(uStructure, "unconditionalViolations", TransitionData.class).size(), Integer.BYTES);
        // Conditional violations
        ByteManipulator.writeLongAsBytes(buffer, 4,
                readUStructureData(uStructure, "conditionalViolations", TransitionData.class).size(), Integer.BYTES);
        // Potential communications
        ByteManipulator.writeLongAsBytes(buffer, 8, uStructure.getPotentialCommunications().size(), Integer.BYTES);
        // Invalid communications
        ByteManipulator.writeLongAsBytes(buffer, 12,
                readUStructureData(uStructure, "invalidCommunications", TransitionData.class).size(), Integer.BYTES);
        // Nash communications
        ByteManipulator.writeLongAsBytes(buffer, 16, uStructure.getNashCommunications().size(), Integer.BYTES);
        // Disablement decisions
        ByteManipulator.writeLongAsBytes(buffer, 20, uStructure.getDisablementDecisions().size(), Integer.BYTES);
        // Suppressed transitions
        ByteManipulator.writeLongAsBytes(buffer, 24,
                readUStructureData(uStructure, "suppressedTransitions", TransitionData.class).size(), Integer.BYTES);
        haf.write(buffer);

        /* Write special transitions to the .hdr file */

        writeTransitionDataToHeader(readUStructureData(uStructure, "unconditionalViolations", TransitionData.class));
        writeTransitionDataToHeader(readUStructureData(uStructure, "conditionalViolations", TransitionData.class));
        writeCommunicationDataToHeader(uStructure.getPotentialCommunications());
        writeTransitionDataToHeader(readUStructureData(uStructure, "invalidCommunications", TransitionData.class));
        writeNashCommunicationDataToHeader(uStructure.getNashCommunications());
        writeDisablementDataToHeader(uStructure.getDisablementDecisions());
        writeTransitionDataToHeader(readUStructureData(uStructure, "suppressedTransitions", TransitionData.class));
    }

    /**
     * Retrieves field data of type {@link List} in a U-Structure by its name.
     * 
     * @param <T>        type of data stored in the list
     * @param uStructure a U-Structure
     * @param name       name of the field
     * @param dataType   type of data stored in the list
     * @return the data stored in the specified field, or {@code null} if something
     *         went wrong
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> readUStructureData(UStructure uStructure, String name, Class<T> dataType) {
        try {
            return (List<T>) FieldUtils.readDeclaredField(uStructure, name, true);
        } catch (IllegalAccessException e) {
            haf.getLogger().catching(e);
            return null;
        }
    }

    /**
     * A helper method to write a list of special transitions to the header file.
     * 
     * @param list The list of transition data
     * @throws IOException If there were any problems writing to file
     **/
    protected void writeTransitionDataToHeader(List<TransitionData> list) throws IOException {

        /* Setup */

        byte[] buffer = new byte[list.size() * 20];
        int index = 0;

        /* Write each piece of transition data into the buffer */

        for (TransitionData data : list) {

            ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, Integer.BYTES);
            index += Integer.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, Long.BYTES);
            index += Long.BYTES;

        }

        /* Write the buffer to file */

        haf.write(buffer);

    }

    /**
     * A helper method to write a list of communications to the header file.
     * NOTE: This could be made more efficient by using one buffer for all
     * communication data. This
     * is possible because each piece of data in the list is supposed to have the
     * same number of roles.
     * 
     * @param list The list of communication data
     * @throws IOException If there was problems writing to file
     **/
    private void writeCommunicationDataToHeader(List<CommunicationData> list) throws IOException {

        for (CommunicationData data : list) {

            byte[] buffer = new byte[20 + data.roles.length];
            int index = 0;

            ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, Integer.BYTES);
            index += Integer.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, Long.BYTES);
            index += Long.BYTES;

            for (CommunicationRole role : data.roles)
                buffer[index++] = role.getNumericValue();

            haf.write(buffer);

        }

    }

    /**
     * A helper method to write a list of communications to the header file.
     * NOTE: This could be made more efficient by using one buffer for all
     * communication data. This
     * is possible because each piece of data in the list is supposed to have the
     * same number of roles.
     * 
     * @param list The list of nash communication data
     * @throws IOException If there was problems writing to file
     **/
    private void writeNashCommunicationDataToHeader(List<NashCommunicationData> list) throws IOException {

        for (NashCommunicationData data : list) {

            byte[] buffer = new byte[36 + data.roles.length];
            int index = 0;

            ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, Integer.BYTES);
            index += Integer.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, Double.doubleToLongBits(data.cost), Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, Double.doubleToLongBits(data.probability), Long.BYTES);
            index += Long.BYTES;

            for (CommunicationRole role : data.roles)
                buffer[index++] = role.getNumericValue();

            haf.write(buffer);

        }

    }

    /**
     * A helper method to write a list of disablement decisions to the header file.
     * NOTE: This could be made more efficient by using one buffer for all
     * disablement decisions.
     * 
     * @param list The list of disablement decisions
     * @throws IOException If there were any problems writing to file
     **/
    private void writeDisablementDataToHeader(List<DisablementData> list) throws IOException {

        for (DisablementData data : list) {

            byte[] buffer = new byte[20 + data.controllers.length];
            int index = 0;

            ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, Long.BYTES);
            index += Long.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, Integer.BYTES);
            index += Integer.BYTES;

            ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, Long.BYTES);
            index += Long.BYTES;

            for (boolean b : data.controllers)
                buffer[index++] = (byte) BooleanUtils.toInteger(b);

            haf.write(buffer);

        }

    }

    private void writeBodyFile() throws IOException {

        int labelLength = calculateLabelLength();

        long nBytesPerState = calculateNumberOfBytesPerState(Integer.BYTES, Long.BYTES,
                calculateTransitionCapacity(), labelLength);

        /* Setup files */

        baf.clearFile();

        /* Copy over body file */

        long counter = 0; // Keeps track of blank states
        byte[] buffer = new byte[(int) nBytesPerState];

        for (long s = 1; s <= automaton.getNumberOfStates() + counter; s++) {

            State state = automaton.getState(s);

            // Check for non-existent state
            if (state == null) {

                // Pad with zeros, which will indicate a non-existent state
                try {
                    baf.write(buffer);
                } catch (IOException e) {
                    baf.getLogger().catching(e);
                }

                counter++;

                continue;
            }

            // Try writing to file
            if (!StateIO.writeToFile(state, baf, nBytesPerState, labelLength, Integer.BYTES, Long.BYTES)) {
                baf.getLogger().error("Could not write copy over state to file. Aborting creation of .bdy file.");
                return;
            }

        } // for

    }

    /**
     * Closes header and body files and releases any system resource
     * associated with these files.
     * 
     * @throws IOException if I/O error occurs
     */
    @Override
    public void close() throws IOException {
        haf.close();
        baf.close();
    }

    /**
     * Checks whether some other object is "equal to" this
     * {@code AutomatonBinaryFileAdapter}.
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if argument is "equal to" this
     *         {@code AutomatonBinaryFileAdapter}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof AutomatonBinaryFileAdapter) {
            AutomatonBinaryFileAdapter other = (AutomatonBinaryFileAdapter) obj;
            return Objects.equals(this.haf, other.haf) && Objects.equals(this.baf, other.baf);
        } else
            return false;
    }

}
