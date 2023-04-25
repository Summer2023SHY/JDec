package com.github.automaton.io;

import java.io.*;
import java.util.*;

import com.github.automaton.automata.*;
import com.github.automaton.automata.util.ByteManipulator;

/**
 * I/O Utility class for {@link State}s.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @see com.github.automaton.automata.State
 */
public class StateIO {

    /** Private constructor */
    private StateIO() {}

    /**
     * Light-weight method used when the transitions are not needed (because loading
     * them takes a bit of time)
     * @implNote When using this method to load a state, it assumed that you will not be
     * accessing or modifying the transitions.
     * 
     * @param automaton The relevant automaton
     * @param file      The {@code .bdy} file containing the state
     * @param id        The ID of the requested state
     * @return the state (with a reference to {@code null} as its list of transitions)
     **/
    public static State readFromFileExcludingTransitions(Automaton automaton, RandomAccessFile file, long id) {

        /* Setup */

        byte[] bytesRead = new byte[1 + automaton.getLabelLength()];

        /* Read bytes */

        try {

            file.seek((id * automaton.getSizeOfState()));
            file.read(bytesRead);

        } catch (IOException e) {

            e.printStackTrace();
            return null;

        }

        /* Exists and marked status */

        boolean marked = (bytesRead[0] & State.MARKED_MASK) > 0;
        boolean exists = (bytesRead[0] & State.EXISTS_MASK) > 0;

        // Return null if this state doesn't actually exist
        if (!exists)
            return null;

        /* State's label */

        char[] arr = new char[automaton.getLabelLength()];
        for (int i = 0; i < arr.length; i++) {

            // Indicates end of label
            if (bytesRead[i + 1] == 0) {

                arr = Arrays.copyOfRange(arr, 0, i);
                break;

                // Read and store character
            } else
                arr[i] = (char) bytesRead[i + 1];

        }

        return new State(new String(arr), id, marked, null);

    }

    /**
     * Read a state (and all of its transitions) from file.
     * 
     * @param automaton The relevant automaton
     * @param file      The {@code .bdy} file containing the state
     * @param id        The ID of the requested state
     * @return the state
     **/
    public static State readFromFile(Automaton automaton, RandomAccessFile file, long id) {

        /* Setup */

        byte[] bytesRead = new byte[(int) automaton.getSizeOfState()];

        /* Read bytes */

        try {

            file.seek(id * automaton.getSizeOfState());
            file.read(bytesRead);

        } catch (IOException e) {

            e.printStackTrace();
            return null;

        }

        /* Exists and marked status */

        boolean marked = (bytesRead[0] & State.MARKED_MASK) > 0;
        boolean exists = (bytesRead[0] & State.EXISTS_MASK) > 0;

        // Return null if this state doesn't actually exist
        if (!exists)
            return null;

        /* State's label */

        char[] arr = new char[automaton.getLabelLength()];
        for (int i = 0; i < arr.length; i++) {

            // Indicates end of label
            if (bytesRead[i + 1] == 0) {

                arr = Arrays.copyOfRange(arr, 0, i);
                break;

                // Read and store character
            } else
                arr[i] = (char) bytesRead[i + 1];

        }

        // Instantiate the state
        State state = new State(new String(arr), id, marked);

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
}
