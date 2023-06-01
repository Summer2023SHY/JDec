package com.github.automaton.io.legacy;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.reflect.TypeUtils;

import com.github.automaton.automata.*;
import com.github.automaton.automata.util.ByteManipulator;
import com.github.automaton.io.AutomatonAdapter;
import com.github.automaton.io.json.JsonUtils;
import com.google.gson.*;

public class AutomatonBinaryAdapter implements AutomatonAdapter, Closeable {

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

    public AutomatonBinaryAdapter(File headerFile, File bodyFile) {

        try {
            haf = new HeaderAccessFile(headerFile);
            baf = new BodyAccessFile(bodyFile);
            JsonObject automatonData = new JsonObject();
            parseHeaderFile(automatonData);
            this.automaton = Automaton.buildAutomaton(automatonData);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private void parseHeaderFile(JsonObject jsonObj) throws IOException {
        haf.seek(0);
        byte[] buffer = haf.readHeaderBytes(HeaderAccessFile.HEADER_SIZE);

        byte type = (byte) ByteManipulator.readBytesAsLong(buffer, 0, 1);
        long nStates = ByteManipulator.readBytesAsLong(buffer, 1, 8);
        int eventCapacity = ByteManipulator.readBytesAsInt(buffer, 9, 4);
        long stateCapacity = ByteManipulator.readBytesAsLong(buffer, 13, 8);
        int transitionCapacity = ByteManipulator.readBytesAsInt(buffer, 21, 4);
        int labelLength = ByteManipulator.readBytesAsInt(buffer, 25, 4);
        long initialState = ByteManipulator.readBytesAsLong(buffer, 29, 8);
        int nControllers = ByteManipulator.readBytesAsInt(buffer, 37, 4);
        int nEvents = ByteManipulator.readBytesAsInt(buffer, 41, 4);

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
            buffer = haf.readHeaderBytes(4);
            int eventLabelLength = ByteManipulator.readBytesAsInt(buffer, 0, 4);

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

    private void readAutomatonSpecialTransitions(JsonObject jsonObj) throws IOException {
        byte[] buffer = haf.readHeaderBytes(4);
        int nBadTransitions = ByteManipulator.readBytesAsInt(buffer, 0, 4);

        List<TransitionData> badTransitions = readTransitionDataFromHeader(nBadTransitions);

        JsonUtils.addListPropertyToJsonObject(jsonObj, "badTransitions", badTransitions, TransitionData.class);
    }

    private void readUStructureSpecialTransitions(JsonObject jsonObj) throws IOException {
        byte[] buffer = haf.readHeaderBytes(28);
        int nUnconditionalViolations = ByteManipulator.readBytesAsInt(buffer, 0, 4);
        int nConditionalViolations = ByteManipulator.readBytesAsInt(buffer, 4, 4);
        int nPotentialCommunications = ByteManipulator.readBytesAsInt(buffer, 8, 4);
        int nInvalidCommunications = ByteManipulator.readBytesAsInt(buffer, 12, 4);
        int nNashCommunications = ByteManipulator.readBytesAsInt(buffer, 16, 4);
        int nDisablementDecisions = ByteManipulator.readBytesAsInt(buffer, 20, 4);
        int nSuppressedTransitions = ByteManipulator.readBytesAsInt(buffer, 24, 4);

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

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, 4);
            index += 4;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

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

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, 4);
            index += 4;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

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

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, 4);
            index += 4;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

            double cost = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, 8));
            index += 8;

            double probability = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, 8));
            index += 8;

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

            long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

            int eventID = ByteManipulator.readBytesAsInt(buffer, index, 4);
            index += 4;

            long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
            index += 8;

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

    @Override
    public File getFile() {
        return getHeaderFile();
    }

    public File getHeaderFile() {
        return haf.getFile();
    }

    public File getBodyFile() {
        return baf.getFile();
    }

    @Override
    public void save() throws IOException {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
        haf.close();
        baf.close();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj instanceof AutomatonBinaryAdapter) {
            AutomatonBinaryAdapter other = (AutomatonBinaryAdapter) obj;
            return Objects.equals(this.haf, other.haf) && Objects.equals(this.baf, other.baf);
        }
        else return false;
    }

}
