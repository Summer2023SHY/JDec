import java.util.*;
import java.io.*;

public class State {
    
		/* Private instance variables */
	
	private String label;
	private long id;
	private boolean marked;
	private ArrayList<Transition> transitions;

	// These masks allow us to store and access multiple true/false values in the same byte
	private static int EXISTS_MASK = 0b00000010; // Whether or not a state actually exists here
	private static int MARKED_MASK = 0b00000001; // Whether or not the state is marked

	/**
	 *	ID (used to identify states): a binary string consisting of all 0's is reserved to represent "null", so:
	 * 	-1 byte allows us to represent up to 255 possible states (2^8 - 1)
	 *	-2 bytes gives 65535 possible states (2^16 - 1)
	 *	-3 bytes gives 16777215 possible states (2^24 - 1)
	 *	...
	 *	-8 bytes gives ~9.2*10^18 possible states (2^63 - 1)
	 **/

	public State(String label, long id, boolean marked, ArrayList<Transition> transitions) {
		this.label = label;
		this.id = id;
		this.marked = marked;
		this.transitions = transitions;
	}

	// Create state with no transitions
	public State(String label, long id, boolean marked) {
		this.label = label;
		this.id = id;
		this.marked = marked;
		transitions = new ArrayList<Transition>();
	}

	public boolean isMarked() {
		return marked;
	}

	/**
	 *	Get the label of the event
 	 *	@return label
	 **/
	public String getLabel() {
		return label;
	}

	/**
	 *	Get the ID number of the state
 	 *	@return id
	 **/
	public long getID() {
		return id;
	}

	/**
	 *	Change the ID number of the state
	 **/
	public void setID(long id) {
		this.id = id;
	}

	public ArrayList<Transition> getTransitions() {
		return transitions;
	}

	public int getNumberOfTransitions() {
		return transitions.size();
	}


	public void addTransition(Transition transition) {
		transitions.add(transition);
	}

	public boolean writeToFile(RandomAccessFile file, long nBytesPerState, int labelLength, int nBytesPerStateID, int transitionCapacity) {

			/* Setup */

		byte[] bytesToWrite = new byte[(int) nBytesPerState];

			/* Exists and marked status */

		bytesToWrite[0] = (byte) (EXISTS_MASK);
		if (isMarked())
			bytesToWrite[0] |= MARKED_MASK;

			/* State's label */

		for (int i = 0; i < label.length(); i++)
			bytesToWrite[i + 1] = (byte) label.charAt(i);

			/* Transitions */
		
		int index = 1 + labelLength;
		for (Transition t : transitions) {

			// Event
			ByteManipulator.writeLongAsBytes(bytesToWrite, index, (long) (t.getEvent().getID()), Event.N_BYTES_OF_ID);
			index += Event.N_BYTES_OF_ID;

			// Target state
			ByteManipulator.writeLongAsBytes(bytesToWrite, index, t.getTargetStateID(), nBytesPerStateID);
			index += nBytesPerStateID;
			
		}

			/* Write to file */

		try {

			file.seek(id * nBytesPerState);
			file.write(bytesToWrite);

            return true;
          
	    } catch (IOException e) {

            e.printStackTrace();
            return false;

	    }

	}

	public static boolean stateExists(Automaton automaton, RandomAccessFile file, long id) {

		try {

			file.seek(id * automaton.getSizeOfState());
			
			return (file.readByte() & EXISTS_MASK) > 0;

		} catch (EOFException e) {

			// State does not exist yet because the file does not go this far
			return false;
			
	    } catch (IOException e) {

            e.printStackTrace();
            return false;

	    }

	}

	/**
	 * Light-weight method used when the transitions are not needed (because loading them takes a bit of time)
	 * NOTE: When using this method to load a state, it assumed that you will not be accessing or modifying the transitions.
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

	    boolean marked = (bytesRead[0] & MARKED_MASK) > 0;
	    boolean exists = (bytesRead[0] & EXISTS_MASK) > 0;

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

		return new State(new String(arr), id, marked);

	}

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

	    boolean marked = (bytesRead[0] & MARKED_MASK) > 0;
	    boolean exists = (bytesRead[0] & EXISTS_MASK) > 0;

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

		State state = new State(new String(arr), id, marked);

			/* Transitions */
		
		int index = 1 + automaton.getLabelLength();
		for (int t = 0; t < automaton.getTransitionCapacity(); t++) {

        	int eventID = (int) ByteManipulator.readBytesAsLong(bytesRead, index, Event.N_BYTES_OF_ID);
        	index += Event.N_BYTES_OF_ID;

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

	@Override public String toString() {
		return "("
			+ label + ",ID:"
			+ id + ","
			+ (marked ? "Marked" : "Unmarked") + ","
			+ "# Transitions: " + transitions.size()
			+ ")";
	}

}