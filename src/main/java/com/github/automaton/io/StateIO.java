package com.github.automaton.io;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.lang3.*;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;
import com.github.automaton.automata.util.ByteManipulator;

/**
 * I/O Utility class for {@link State}s.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @see com.github.automaton.automata.State
 * @since 1.1
 */
public class StateIO {

    /** 
     * The UTF-8 Charset used for state label encoding
     * 
     * @since 2.0
     */
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    /**
     * Bit field for checking whether or not a state actually exists
     * 
     * @see State#EXISTS_MASK
     * @since 2.0
     */
    private static final BitField EXISTS_FIELD = new BitField(State.EXISTS_MASK);
    /**
     * Bit field for checking whether or not a state is marked
     * 
     * @see State#MARKED_MASK
     * @since 2.0
     */
    private static final BitField MARKED_FIELD = new BitField(State.MARKED_MASK);
    /**
     * Bit field for checking whether or not a state actually exists
     * 
     * @see State#ENABLEMENT_MASK
     * @since 2.0
     */
    private static final BitField ENABLEMENT_FIELD = new BitField(State.ENABLEMENT_MASK);
    /**
     * Bit field for checking whether or not a state is a disablement state
     * 
     * @see State#DISABLEMENT_MASK
     * @since 2.0
     */
    private static final BitField DISABLEMENT_FIELD = new BitField(State.DISABLEMENT_MASK);

    private static Logger logger = LogManager.getLogger();

    /** Private constructor */
    private StateIO() {}

    /**
     * Light-weight method used when the transitions are not needed (because loading
     * them takes a bit of time)
     * 
     * @implNote When using this method to load a state, it assumed that you will
     *           not be
     *           accessing or modifying the transitions.
     * 
     * @param automaton The relevant automaton
     * @param bodyAccessFile The {@link BodyAccessFile} containing the state
     * @param id        The ID of the requested state
     * @return the state (with a reference to {@code null} as its list of
     *         transitions)
     **/
    @SuppressWarnings("deprecated")
    public static State readFromFileExcludingTransitions(Automaton automaton, BodyAccessFile bodyAccessFile, long id) {
        return readFromFileExcludingTransitions(automaton, bodyAccessFile.getRAFile(), id);
    }

    /**
     * Light-weight method used when the transitions are not needed (because loading
     * them takes a bit of time)
     * 
     * @implNote When using this method to load a state, it assumed that you will
     *           not be
     *           accessing or modifying the transitions.
     * 
     * @param automaton The relevant automaton
     * @param file      The {@code .bdy} file containing the state
     * @param id        The ID of the requested state
     * @return the state (with a reference to {@code null} as its list of
     *         transitions)
     * 
     * @deprecated {@code .bdy} files should not be directly read from nor written to. Use {@link #readFromFileExcludingTransitions(Automaton, BodyAccessFile, long)} instead.
     **/
    @Deprecated(since="1.1")
    public static State readFromFileExcludingTransitions(Automaton automaton, RandomAccessFile file, long id) {

        /* Setup */

        byte[] bytesRead = new byte[1 + automaton.getLabelLength()];

        /* Read bytes */

        try {

            file.seek((id * automaton.getSizeOfState()));
            file.read(bytesRead);

        } catch (IOException e) {

            logger.catching(e);
            return null;

        }

        /* Exists and marked status */

        boolean marked = MARKED_FIELD.isSet(bytesRead[0]);
        boolean exists = EXISTS_FIELD.isSet(bytesRead[0]);
        boolean enablement = ENABLEMENT_FIELD.isSet(bytesRead[0]);
        boolean disablement = DISABLEMENT_FIELD.isSet(bytesRead[0]);

        // Return null if this state doesn't actually exist
        if (!exists)
            return null;

        /* State's label */

        byte[] arr = new byte[automaton.getLabelLength()];
        System.arraycopy(bytesRead, 1, arr, 0, arr.length);

        int labelLength = ArrayUtils.indexOf(arr, (byte) 0);
        labelLength = labelLength == ArrayUtils.INDEX_NOT_FOUND ? arr.length : labelLength;

        return new State(new String(arr, 0, labelLength, UTF8_CHARSET), id, marked, null, enablement, disablement);

    }

    /**
     * Read a state (and all of its transitions) from file.
     * 
     * @param automaton The relevant automaton
     * @param bodyAccessFile The {@link BodyAccessFile} containing the state
     * @param id        The ID of the requested state
     * @return the state
     **/
    public static State readFromFile(Automaton automaton, BodyAccessFile bodyAccessFile, long id) {
        return readFromFile(automaton, bodyAccessFile.getRAFile(), id);
    }

    /**
     * Read a state (and all of its transitions) from file.
     * 
     * @param automaton The relevant automaton
     * @param file      The {@code .bdy} file containing the state
     * @param id        The ID of the requested state
     * @return the state
     * 
     * @deprecated {@code .bdy} files should not be directly read from nor written to. Use {@link #readFromFile(Automaton, BodyAccessFile, long)} instead.
     **/
    @Deprecated(since="1.1")
    public static State readFromFile(Automaton automaton, RandomAccessFile file, long id) {

        /* Setup */

        byte[] bytesRead = new byte[(int) automaton.getSizeOfState()];

        /* Read bytes */

        try {

            file.seek(id * automaton.getSizeOfState());
            file.read(bytesRead);

        } catch (IOException e) {

            logger.catching(e);
            return null;

        }

        /* Exists and marked status */

        boolean marked = MARKED_FIELD.isSet(bytesRead[0]);
        boolean exists = EXISTS_FIELD.isSet(bytesRead[0]);
        boolean enablement = ENABLEMENT_FIELD.isSet(bytesRead[0]);
        boolean disablement = DISABLEMENT_FIELD.isSet(bytesRead[0]);

        // Return null if this state doesn't actually exist
        if (!exists)
            return null;

        /* State's label */

        byte[] arr = new byte[automaton.getLabelLength()];
        System.arraycopy(bytesRead, 1, arr, 0, arr.length);

        int labelLength = ArrayUtils.indexOf(arr, (byte) 0);
        labelLength = labelLength == ArrayUtils.INDEX_NOT_FOUND ? arr.length : labelLength;

        // Instantiate the state
        State state = new State(new String(arr, 0, labelLength, UTF8_CHARSET), id, marked, enablement, disablement);

        /* Transitions */

        int index = 1 + automaton.getLabelLength();
        for (int t = 0; t < automaton.getTransitionCapacity(); t++) {

            int eventID = ByteManipulator.readBytesAsInt(bytesRead, index, automaton.getSizeOfEventID());
            index += automaton.getSizeOfEventID();

            long targetStateID = ByteManipulator.readBytesAsLong(bytesRead, index, automaton.getSizeOfStateID());
            index += automaton.getSizeOfStateID();

            // Indicates that we've hit padding, so let's stop
            if (eventID == 0)
                break;

            // Add transition to the list
            state.addTransition(new Transition(automaton.getEvent(eventID), targetStateID));

        }

        return state;

    }

    /**
     * Check to see if the specified state actually exists in the file (or if it's
     * just a blank spot filled with padding).
     * 
     * @param automaton The automaton in consideration
     * @param bodyAccessFile The {@link BodyAccessFile} containing the states associated with this
     *                  automaton
     * @param id        The ID of the state we are checking to see if it exists
     * @return whether or not the state exists
     **/
    public static boolean stateExists(Automaton automaton, BodyAccessFile bodyAccessFile, long id) {
        return stateExists(automaton, bodyAccessFile.getRAFile(), id);
    }

    /**
     * Check to see if the specified state actually exists in the file (or if it's
     * just a blank spot filled with padding).
     * 
     * @param automaton The automaton in consideration
     * @param bodyAccessFile The {@link BodyAccessFile} containing the states associated with this
     *                  automaton
     * @param state     The state we are checking to see if it exists
     * @return whether or not the state exists
     * 
     * @since 2.0
     **/
    public static boolean stateExists(Automaton automaton, BodyAccessFile bodyAccessFile, State state) {
        return stateExists(automaton, bodyAccessFile, state.getID());
    }

    /**
     * Check to see if the specified state actually exists in the file (or if it's
     * just a blank spot filled with padding).
     * 
     * @param automaton The automaton in consideration
     * @param file      The {@code .bdy} file containing the states associated with this
     *                  automaton
     * @param id        The ID of the state we are checking to see if it exists
     * @return whether or not the state exists
     * @deprecated {@code .bdy} files should not be directly read from nor written to. Use {@link #stateExists(Automaton, BodyAccessFile, long)} instead.
     **/
    @Deprecated(since="1.1")
    public static boolean stateExists(Automaton automaton, RandomAccessFile file, long id) {

        try {

            file.seek(id * automaton.getSizeOfState());
            return (file.readByte() & State.EXISTS_MASK) > 0;

        } catch (EOFException e) {

            // State does not exist yet because the file does not go this far
            return false;

        } catch (IOException e) {

            logger.catching(e);
            return false;

        }

    }

    /* WORKING WITH FILES */

    /**
     * Write a given state to file.
     * 
     * @param s                The state to write
     * @param bodyAccessFile   The {@link BodyAccessFile} we are writing to
     * @param nBytesPerState   The number of bytes used to store each state in the
     *                         file
     * @param labelLength      The amount of characters reserved for the label in
     *                         each state
     * @param nBytesPerEventID The number of bytes used to store an event ID
     * @param nBytesPerStateID The number of bytes used to store a state ID
     * @return Whether or not the operation was successful
     **/
    public static boolean writeToFile(State s, BodyAccessFile bodyAccessFile, long nBytesPerState, int labelLength,
            int nBytesPerEventID, int nBytesPerStateID) {
        return writeToFile(s, bodyAccessFile.getRAFile(), nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID);
    }

    /**
     * Writes a given state to file.
     * 
     * @param s                The state to write
     * @param file             The RandomAccessFile we are using to write to
     * @param nBytesPerState   The number of bytes used to store each state in the
     *                         file
     * @param labelLength      The amount of characters reserved for the label in
     *                         each state
     * @param nBytesPerEventID The number of bytes used to store an event ID
     * @param nBytesPerStateID The number of bytes used to store a state ID
     * @return Whether or not the operation was successful
     * 
     * @deprecated {@code .bdy} files should not be directly read from nor written to. Use {@link #writeToFile(State, BodyAccessFile, long, int, int, int)} instead.
     **/
    @Deprecated(since="1.1")
    public static boolean writeToFile(State s, RandomAccessFile file, long nBytesPerState, int labelLength, int nBytesPerEventID,
            int nBytesPerStateID) {

        /* Setup */

        byte[] bytesToWrite = new byte[(int) nBytesPerState];

        /* Exists and marked status */

        bytesToWrite[0] = (byte) (State.EXISTS_MASK);
        bytesToWrite[0] = MARKED_FIELD.setByteBoolean(bytesToWrite[0], s.isMarked());
        bytesToWrite[0] = ENABLEMENT_FIELD.setByteBoolean(bytesToWrite[0], s.isEnablementState());
        bytesToWrite[0] = DISABLEMENT_FIELD.setByteBoolean(bytesToWrite[0], s.isDisablementState());

        /* State's label */

        System.arraycopy(s.getLabel().getBytes(UTF8_CHARSET), 0, bytesToWrite, 1, Math.min(labelLength, s.getLabel().length()));

        /* Transitions */

        int index = 1 + labelLength;
        for (Transition t : s.getTransitions()) {

            // Event
            ByteManipulator.writeLongAsBytes(bytesToWrite, index, (long) (t.getEvent().getID()), nBytesPerEventID);
            index += nBytesPerEventID;

            // Target state
            ByteManipulator.writeLongAsBytes(bytesToWrite, index, t.getTargetStateID(), nBytesPerStateID);
            index += nBytesPerStateID;

        }

        /* Try writing to file */

        try {

            file.seek(s.getID() * nBytesPerState);
            file.write(bytesToWrite);

            return true;

        } catch (IOException e) {

            logger.catching(e);

            return false;

        }

    }

    /**
     * Rewrites the status of a state in the given automaton
     * 
     * @param automaton the automaton that contains the given state
     * @param state     the state with modified status
     * @param baf       The {@link BodyAccessFile} containing the states associated
     *                  with this automaton
     * 
     * @throws StateNotFoundException   if {@code automaton} does not contain a
     *                                  state with the matching ID
     * @throws IllegalArgumentException if {@code state} is not equal to the one
     *                                  stored in {@code automaton}
     * @throws IOException              if I/O error occurs
     * 
     * @since 2.0
     */
    public static void rewriteStatus(Automaton automaton, BodyAccessFile baf, State state) throws IOException {
        if (!stateExists(automaton, baf, state.getID())) {
            throw new StateNotFoundException(state.getID());
        } else if (!Objects.equals(readFromFileExcludingTransitions(automaton, baf, state.getID()), state)) {
            throw new IllegalArgumentException("The provided state is not equal to the one stored in the automaton.");
        }
        baf.getRAFile().seek(state.getID() * automaton.getSizeOfState());
        baf.getLogger().trace("StateIO.rewriteStatus() - FP: " + baf.getRAFile().getFilePointer());
        byte newStatus = State.EXISTS_MASK;
        newStatus = MARKED_FIELD.setByteBoolean(newStatus, state.isMarked());
        newStatus = ENABLEMENT_FIELD.setByteBoolean(newStatus, state.isEnablementState());
        newStatus = DISABLEMENT_FIELD.setByteBoolean(newStatus, state.isDisablementState());
        baf.getRAFile().write(newStatus);
    }
}
