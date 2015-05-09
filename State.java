import java.util.*;
import java.io.*;

public class State {

		/* Class constants */

	public static final int MARKED_BIT = 0b10000000; // Leading bit of a byte
    
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

	public void addTransition(Transition transition) {
		transitions.add(transition);
	}

	public ArrayList<Transition> getTransitions() {
		return transitions;
	}

	public int getNumberOfTransitions() {
		return transitions.size();
	}

	public boolean writeToFile(RandomAccessFile file, long nBytesPerState, int nBytesPerStateID, int transitionCapacity) {

		try {

			// Special case if there are no transitions
			if (transitions.size() == 0) {

				// Write marked bit
				file.seek(id * nBytesPerState);
				writeLongAsBytes(file, marked ? MARKED_BIT : 0, Event.N_BYTES_OF_ID);

				// Pad with zeroes
				for (int t = 1; t < transitionCapacity; t++)
	            	for (int b = 0; b < Event.N_BYTES_OF_ID + nBytesPerStateID; b++)
	            		file.writeByte(0);

	           	return true;
			}

            // Write each transition to file (event ID followed by target state ID)
            file.seek(id * nBytesPerState);
            for (Transition t : transitions) {
            	writeLongAsBytes(file, (long) (t.getEvent().getID() | MARKED_BIT), Event.N_BYTES_OF_ID);
            	writeLongAsBytes(file, t.getTargetStateID(), nBytesPerStateID);
            }

            // Pad with zeroes
            for (int t = 0; t < transitionCapacity - transitions.size(); t++)
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
	public static State readFromFile(Automaton automaton, RandomAccessFile file, long id, long nBytesPerState, int nBytesPerStateID, int transitionCapacity) {

		try {

			// Read in marked state (which is stored as the leading bit of the targetStateID in the first transition)
			file.seek(id * nBytesPerState);
			boolean marked = ((readBytesAsLong(file, Event.N_BYTES_OF_ID)) & MARKED_BIT) > 0;

			System.out.println("READ: " + id + " " + marked);

			State state = new State("temporaryLabel" /** TEMPORARY UNTIL HEADER FILE **/, id, marked);

            file.seek(id * nBytesPerState);

            // Read in each transition
            for (int t = 0; t < transitionCapacity; t++) {

            	long temp = readBytesAsLong(file, Event.N_BYTES_OF_ID);
            		System.out.println("temp:" + temp);
            	int eventID = ((int) temp) & ~MARKED_BIT;
            	long targetStateID = readBytesAsLong(file, nBytesPerStateID);

            	System.out.println(id + " " + eventID);

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