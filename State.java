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

	/**
	 *	Get the label of the event
 	 *	@return label
	 **/
	public String getLabel() {
		return label;
	}

	public void addTransition(Transition transition) {
		transitions.add(transition);
	}

	public ArrayList<Transition> getTransitions() {
		return transitions;
	}

	public int getNumberOfTransitions() {
		return transitions.size();
	}

	public boolean writeToFile(RandomAccessFile file, long nBytesPerState, int nBytesPerStateID, int nTransitionsPerState) {

		try {

            file.seek(id * nBytesPerState);

            // Write each transition to file (event ID followed by target state ID)
            for (Transition t : transitions) {
            	writeLongAsBytes(file, (long) (t.getEvent().getID()), Event.N_BYTES_OF_ID);
            	writeLongAsBytes(file, t.getTargetStateID(), nBytesPerStateID);
            }

            // Pad with zeroes
            for (int t = 0; t < nTransitionsPerState - transitions.size(); t++)
            	for (int b = 0; b < Event.N_BYTES_OF_ID + nBytesPerStateID; b++)
            		file.writeByte(0);

            return true;
          
	    } catch (IOException e) {
            e.printStackTrace();
            return false;
	    }

	}

	/**
	 *	Get the ID number of the state
 	 *	@return id
	 **/
	public long getID() {
		return id;
	}

	// Splits the specified number (which is a long) into the proper number of bytes and writes them one at a time into the file
	private static void writeLongAsBytes(RandomAccessFile file, long n, int nBytes) throws IOException {

		for (int i = nBytes - 1; i >= 0; i--)
			file.writeByte((int) (n >> (i*8)));

	}

	// 	unfinished
	public static State readFromFile(Automaton automaton, RandomAccessFile file, long id, long nBytesPerState, int nBytesPerStateID, int nTransitionsPerState) {

		try {

			State state = new State("temporaryLabel" /** TEMPORARY UNTIL HEADER FILE **/, id, true /** TEMPORARY UNTIL HEADER FILE **/);

            file.seek(id * nBytesPerState);

            // Read in each transition
            for (int t = 0; t < nTransitionsPerState; t++) {
            	int eventID = (int) readBytesAsLong(file, Event.N_BYTES_OF_ID);
            	long targetStateID = readBytesAsLong(file, nBytesPerStateID);

            	// Indicates that we've hit padding, so let's stop
            	if (eventID == 0)
            		break;

            	// Add transition to the list
            	state.addTransition(new Transition(automaton.getEvent(eventID), targetStateID));
            }

            return state;
          
	    } catch (IOException e) {
            e.printStackTrace();
            return null;
	    }

	}

	// Joins the specified number of bytes into a long from file
	private static long readBytesAsLong(RandomAccessFile file, int nBytes) throws IOException {

		long n = 0;

		for (int i = nBytes - 1; i >= 0; i--) {
			n <<= 8;
			n += file.readUnsignedByte();
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