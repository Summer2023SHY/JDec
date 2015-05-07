import java.util.*;
import java.io.*;
import java.awt.image.*;

public class Automaton {

	// temporary
	public void printStateData() {
		System.out.println("nBytesPerStateID: " + nBytesPerStateID);
		System.out.println("nTransitionsPerState: " + nTransitionsPerState);
		System.out.println("nBytesPerState: " + nBytesPerState);
		System.out.println("nStates: " + nStates);
		System.out.println("currentMaxStates: " + currentMaxStates);
	}
	public void printEventData() {
		System.out.println("# events: " + events.size());
		System.out.println("# active events: " + activeEvents.size());
		System.out.println("# controllable events: " + controllableEvents.size());
		System.out.println("# observable events: " + observableEvents.size());
	}

		/* Class constants */

	public static long MAX_NUMBER_OF_STATES = Long.MAX_VALUE;

		/* Private instance variables */

	private Set<Event> 	events = new HashSet<Event>(),
						activeEvents = new HashSet<Event>(),
						controllableEvents = new HashSet<Event>(),
						observableEvents = new HashSet<Event>();

	/**
	 *	ID (used to identify states): a binary string consisting of all 0's is reserved to represent "null", so:
	 * 	-1 byte allows us to represent up to 255 possible states (2^8 - 1)
	 *	-2 bytes gives 65535 possible states (2^16 - 1)
	 *	-3 bytes gives 16777215 possible states (2^24 - 1)
	 *	...
	 *	-8 bytes gives ~9.2*10^18 possible states (2^63 - 1)
	 * 	NOTE: Whenever nBytesPerStateID or nTransitionsPerState is changed, this affects nBytesPerState, which means the binary file needs to be recreated.
	 **/
	private int nBytesPerStateID = 1;
	private int nTransitionsPerState = 1;
	private int nBytesPerState = 1;
	private long nStates = 0;
	private long currentMaxStates = 255;

	private File file = null;
	private RandomAccessFile 	headerFile, // Contains basic information about automaton, needed in order to read the bodyFile
								bodyFile;	// List each state in the automaton, with the transitions

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton
     **/
    public Automaton() {
    	updateNumberBytesPerState();
    }

    /**
     *	Convenience constructor: create automaton from a binary file
	 *	@param file - The binary file to load the automaton from
     **/
	public Automaton(File headerFile, File bodyFile) {
		this();

		try {
			this.headerFile = new RandomAccessFile(headerFile, "rw");
			this.bodyFile = new RandomAccessFile(bodyFile, "rw");
		 } catch (IOException e) {
            e.printStackTrace();
	    }

    }
    	/** AUTOMATA OPERATIONS **/

    static Automaton intersection(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    static Automaton union(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    	/** **/

    public BufferedImage generateImage() {

		return null; // temporary
	}

	/**
	 *	Add the specified state ot the automaton
	 *	@return whether or not the addition was successful 
	 **/
	public boolean addState(String label, boolean marked, ArrayList<Transition> transitions) {

		// Ensure that we haven't already reached the limit (NOTE: This will likely never be the case)
		if (nStates == MAX_NUMBER_OF_STATES)
			return false;

		// Increase the maximum allowed transitions per state
		if (transitions.size() > nTransitionsPerState) {
			nTransitionsPerState = transitions.size();
			recreateBinaryFile();
		}

		// Write new state to file
		State state = new State(label, ++nStates, marked, transitions);
		state.writeToFile(bodyFile, nBytesPerState, nTransitionsPerState);

		// Check to see if we need to re-write the entire binary file
		if (nStates > currentMaxStates) {

			// Adjust variables
			currentMaxStates = (currentMaxStates * 2) +  1;
			nBytesPerStateID++;

			
			updateNumberBytesPerState();

			recreateBinaryFile();

		}

		return true;
	}

	/**
	 * Re-calculate the amount of space required to store the transitions of a state
	 **/
	private void updateNumberBytesPerState() {
		nBytesPerState = nTransitionsPerState * (Event.N_BYTES_OF_ID + nBytesPerStateID);
	}

	/**
	 *	Add the specified event to the set (events with identical labels and different properties are considered unique)
	 *	@return whether or not the added event was unqiue (if false, then the list did not change in size)
	 **/
	public boolean addEvent(String label, boolean observable, boolean controllable) {

		// Keep track of the original 
		long originalSize = events.size();

		// Create and add the event
		Event event = new Event(label, events.size() + 1, observable, controllable);
		events.add(event);
		activeEvents.add(event);

		// Add event to corresponding lists
		if (observable)
			observableEvents.add(event);
		if (controllable)
			controllableEvents.add(event);

		// If the number of events have changed, that means that this was a unique event
		return originalSize != events.size();

	}

	private void recreateBinaryFile() {

		System.out.println("NOT IMPLEMENTED YET!!");

	}

    	/** STANDARD ACCESSOR AND MUTATOR METHODS **/  

    /**
     *	Given the ID number of a state, get the state information
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return State.readFromFile(bodyFile, id, nBytesPerState, nTransitionsPerState);
    }

    public Set<Event> getEvents() {
    	return events;
    }

    public Set<Event> getActiveEvents() {
    	return activeEvents;
    }

    public Set<Event> getControllableEvents() {
    	return controllableEvents;
    }

    public Set<Event> getObservableEvents() {
    	return observableEvents;
    }

}