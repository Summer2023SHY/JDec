import java.util.*;
import java.io.*;

public class State {
    
		/* Private instance variables */
	
	private String label;
	private long id;
	private boolean marked;
	private ArrayList<Transition> transitions;

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

	public ArrayList<Transition> getTransitions() {
		return transitions;
	}

	public int getNumberOfTransitions() {
		return transitions.size();
	}


	public void addTransition(Transition transition) {
		transitions.add(transition);
	}

	public boolean writeToFile(RandomAccessFile file, long nBytesPerState, int labelCapacity, int nBytesPerStateID, int transitionCapacity) {

			/* Setup */

		byte[] bytesToWrite = new byte[(int) nBytesPerState];

			/* Marked status */

		bytesToWrite[0] = (byte) (marked ? 1 : 0);

			/* State's label */

		for (int i = 0; i < label.length(); i++)
			bytesToWrite[i + 1] = (byte) label.charAt(i);

			/* Transitions */
		
		int index = 1 + labelCapacity;
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

	/* Light-weight method used just to get the label (because loading transitions as well take a bit of time) */
	public static String readLabelFromFile(Automaton automaton, RandomAccessFile file, long id) {

		/* Setup */

		byte[] bytesRead = new byte[automaton.getLabelLength()];

			/* Read bytes */

		try {

			file.seek((id * automaton.getSizeOfState()) + 1);
			file.read(bytesRead);
			
	    } catch (IOException e) {

            e.printStackTrace();
            return null;

	    }
	    	/* State's label */

	    char[] arr = new char[automaton.getLabelLength()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = (char) bytesRead[i];

		return new String(arr);

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

	    	/* Marked status */

	    boolean marked = (bytesRead[0] == 1);

	    	/* State's label */

	    char[] arr = new char[automaton.getLabelLength()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = (char) bytesRead[i + 1];

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