import java.util.*;
import java.io.*;

public class State {
    
		/* Private instance variables */
	
	private String label;
	private long id;
	private boolean marked;
	private ArrayList<Transition> transitions;

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
			writeLongAsBytes(bytesToWrite, index, (long) (t.getEvent().getID()), Event.N_BYTES_OF_ID);
			index += Event.N_BYTES_OF_ID;

			// Target state
			writeLongAsBytes(bytesToWrite, index, t.getTargetStateID(), nBytesPerStateID);
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

	// Splits the specified number (which is a long) into the proper number of bytes and writes them one at a time into the array
	private static void writeLongAsBytes(byte[] arr, int index, long n, int nBytes) {

		for (int i = nBytes - 1; i >= 0; i--)
			arr[index++] = (byte) (n >> (i*8));

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

        	int eventID = (int) readBytesAsLong(bytesRead, index, Event.N_BYTES_OF_ID);
        	index += Event.N_BYTES_OF_ID;

        	long targetStateID = readBytesAsLong(bytesRead, index, automaton.getSizeOfStateID());
        	index += automaton.getSizeOfStateID();

        	// Indicates that we've hit padding, so let's stop
        	if (eventID == 0)
        		break;

        	// Add transition to the list
        	state.addTransition(new Transition(automaton.getEvent(eventID), targetStateID));
        }

	    return state;

	}

	// Joins the specified number of bytes into a long from an array of bytes
	private static long readBytesAsLong(byte[] arr, int index, int nBytes) {

		long n = 0;

		for (int i = nBytes - 1; i >= 0; i--) {
			n <<= 8;
			n += arr[index++];
		}

		return n;

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