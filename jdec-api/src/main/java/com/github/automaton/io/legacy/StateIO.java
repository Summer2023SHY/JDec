/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.legacy;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;

/**
 * Legacy I/O Utility class for {@link State}s.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @see com.github.automaton.automata.State
 * @since 1.1
 * @revised 2.0
 */
public class StateIO {

    /**
     * Bitmask for checking whether or not a state actually exists here
     * 
     * @since 2.0
     */
    public static final int EXISTS_MASK = 0b00000010;
    /**
     * Bitmask for checking whether or not a state is marked
     * 
     * @since 2.0
     */
    public static final int MARKED_MASK = 0b00000001;
    /**
     * Bitmask for checking whether or not a state is an enablement state
     * 
     * @since 2.0
     */
    public static final int ENABLEMENT_MASK = 0b00000100;
    /**
     * Bitmask for checking whether or not a state is a disablement state
     * 
     * @since 2.0
     */
    public static final int DISABLEMENT_MASK = 0b00001000;

    /**
     * Bit field for checking whether or not a state actually exists
     * 
     * @see State#EXISTS_MASK
     * @since 1.3
     */
    private static final BitField EXISTS_FIELD = new BitField(EXISTS_MASK);
    /**
     * Bit field for checking whether or not a state is marked
     * 
     * @see State#MARKED_MASK
     * @since 1.3
     */
    private static final BitField MARKED_FIELD = new BitField(MARKED_MASK);
    /**
     * Bit field for checking whether or not a state actually exists
     * 
     * @see State#ENABLEMENT_MASK
     * @since 1.3
     */
    private static final BitField ENABLEMENT_FIELD = new BitField(ENABLEMENT_MASK);
    /**
     * Bit field for checking whether or not a state is a disablement state
     * 
     * @see State#DISABLEMENT_MASK
     * @since 1.3
     */
    private static final BitField DISABLEMENT_FIELD = new BitField(DISABLEMENT_MASK);

    private static Logger logger = LogManager.getLogger();

    /** Private constructor */
    private StateIO() {}

    /**
     * Read a state (and all of its transitions) from file.
     * 
     * @param events the list of events that trigger transitions
     * @param properties properties of the automaton storing the returned state
     * @param bodyAccessFile The {@link BodyAccessFile} containing the state
     * @param id        The ID of the requested state
     * @return the state with the specified ID, or {@code null} if the state
     * with matching ID does not exist
     * 
     * @throws IllegalArgumentException if either one of {@code events} or
     * {@code properties} is empty, or {@code id} is negative
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if any one of the arguments is {@code null}
     **/
    public static State readFromFile(List<Event> events, Map<String, Number> properties, BodyAccessFile bodyAccessFile, long id) throws IOException {

        if (id < 0) 
            throw new IllegalArgumentException("Invalid state ID: " + id);

        if (Objects.requireNonNull(events).isEmpty())
            throw new IllegalArgumentException("Invalid list of events: list is empty");

        if (Objects.requireNonNull(properties).isEmpty())
            throw new IllegalArgumentException("Invalid argument for 'properties'");

        RandomAccessFile file = Objects.requireNonNull(bodyAccessFile).getRAFile();

        /* Setup */

        int nBytesPerState = properties.get("nBytesPerState").intValue();

        byte[] bytesRead = new byte[nBytesPerState];

        /* Read bytes */

        file.seek(Math.multiplyExact(id, nBytesPerState));
        file.read(bytesRead);

        /* Exists and marked status */

        boolean marked = MARKED_FIELD.isSet(bytesRead[0]);
        boolean exists = EXISTS_FIELD.isSet(bytesRead[0]);
        boolean enablement = ENABLEMENT_FIELD.isSet(bytesRead[0]);
        boolean disablement = DISABLEMENT_FIELD.isSet(bytesRead[0]);

        // Return null if this state doesn't actually exist
        if (!exists) {
            logger.debug("State with ID " + id + " does not exist.");
            return null;
        }

        /* State's label */

        int maxLabelLength = properties.get("labelLength").intValue();

        byte[] arr = new byte[maxLabelLength];
        System.arraycopy(bytesRead, 1, arr, 0, arr.length);

        int labelLength = ArrayUtils.indexOf(arr, (byte) 0);
        labelLength = labelLength == ArrayUtils.INDEX_NOT_FOUND ? arr.length : labelLength;

        // Instantiate the state
        State state = new State(new String(arr, 0, labelLength, UTF_8), id, marked);

        /* Transitions */

        int index = 1 + maxLabelLength;

        int nBytesPerEventID = properties.get("nBytesPerEventID").intValue();
        int nBytesPerStateID = properties.get("nBytesPerStateID").intValue();
        
        for (int t = 0; t < properties.get("transitionCapacity").intValue(); t++) {

            int eventID = ByteManipulator.readBytesAsInt(bytesRead, index, nBytesPerEventID);
            index += nBytesPerEventID;

            long targetStateID = ByteManipulator.readBytesAsLong(bytesRead, index, nBytesPerStateID);
            index += nBytesPerStateID;

            // Indicates that we've hit padding, so let's stop
            if (eventID == 0)
                break;

            // Add transition to the list
            state.addTransition(new Transition(events.get(eventID - 1), targetStateID));

        }

        return state;

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

        RandomAccessFile file = bodyAccessFile.getRAFile();

        byte[] bytesToWrite = new byte[(int) nBytesPerState];

        /* Exists and marked status */

        bytesToWrite[0] = (byte) (EXISTS_MASK);
        bytesToWrite[0] = MARKED_FIELD.setByteBoolean(bytesToWrite[0], s.isMarked());
        bytesToWrite[0] = ENABLEMENT_FIELD.setByteBoolean(bytesToWrite[0], s.isEnablementState());
        bytesToWrite[0] = DISABLEMENT_FIELD.setByteBoolean(bytesToWrite[0], s.isDisablementState());

        /* State's label */

        System.arraycopy(s.getLabel().getBytes(UTF_8), 0, bytesToWrite, 1, Math.min(labelLength, s.getLabel().length()));

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

            file.seek(Math.multiplyExact(s.getID(), nBytesPerState));
            file.write(bytesToWrite);

            return true;

        } catch (IOException e) {

            logger.catching(e);

            return false;

        }

    }
}
