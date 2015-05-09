import java.util.*;
import java.io.*;
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;

public class Automaton {

	// temporary
	public void printStateData() {
		System.out.println("nBytesPerStateID: " + nBytesPerStateID);
		System.out.println("nTransitionsPerState: " + nTransitionsPerState);
		System.out.println("nBytesPerState: " + nBytesPerState);
		System.out.println("nStates: " + nStates);
		System.out.println("currentMaxStates: " + currentMaxStates);
	}

		/* Class constants */

	public static long MAX_NUMBER_OF_STATES = Long.MAX_VALUE;
	public static long LIMIT_OF_STATES_FOR_PICTURE = 10000; // arbitrary

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

	private static File defaultHeaderFile = new File("temp.hdr"),
						defaultBodyFile = new File("temp.bdy");
	private RandomAccessFile 	headerRAFile, // Contains basic information about automaton, needed in order to read the bodyFile
								bodyRAFile;	// List each state in the automaton, with the transitions

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton
     **/
    public Automaton() {
    	this(defaultHeaderFile, defaultBodyFile);
    	updateNumberBytesPerState();
    }

    /**
     *	Convenience constructor: create automaton from a binary file
	 *	@param headerFile - The binary file to load the header information of the automaton from (information about events, etc.)
	 *	@param bodyFile - The binary file to load the body information of the automaton from (states and transitions)
     **/
	public Automaton(File headerFile, File bodyFile) {
		
		try {
			this.headerRAFile = new RandomAccessFile(headerFile, "rw");
			this.bodyRAFile = new RandomAccessFile(bodyFile, "rw");
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


    public void outputDOT() {

    	// Abort the operation if the automaton is too large to do this in a reasonable amount of time
    	if (nStates > LIMIT_OF_STATES_FOR_PICTURE) {
    		System.out.println("ERROR: Aborted due to the fact that this graph is large...!");
    		return;
    	}

    	StringBuilder str = new StringBuilder();

    	str.append("digraph G {");
    	// str.append("size=\"4,4\";");
    	str.append("node [shape=circle, style=bold]");
    	
    	for (long s = 1; s <= nStates; s++) {
    		State state = getState(s);

    		for (Transition t : state.getTransitions()) {
    			str.append(state.getID() + "->" + t.getTargetStateID());
    			// str.append(state.getLabel() + "->" + getState(t.getTargetStateID()).getLabel());
    			str.append(" [label=\"" + t.getEvent().getLabel() + "\"]");
    			str.append(";");
    		}

    	}

    	str.append("}");

    	try {

    		// Write DOT language to file
	    	PrintStream out = new PrintStream(new FileOutputStream("out.tmp"));
			out.print(str.toString());

			// Produce PNG from DOT language
	        Process process = new ProcessBuilder(
	                "dot",
	                "-Tpng",
	                "out.tmp",
	                "-o",
	                "image.png"
	            ).start();

	        process.waitFor();

	    } catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		
    	
    }

    public BufferedImage loadImageFromFile() {

    	try {
			return ImageIO.read(getClass().getResource("image.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null; // temporary
	}

	// CURRENTLY IN-EFFICENT!!!!!!!!!! (rewrites the entire state to file instead of only writing the new transition)
	
	public void addTransition(long initialStateID, int eventID, long targetStateID) {

		State initialState = getState(initialStateID);

		// Increase the maximum allowed transitions per state
		if (initialState.getNumberOfTransitions() == nTransitionsPerState) {
			nTransitionsPerState++;
			recreateBinaryFile();
		}

		initialState.addTransition(new Transition(getEvent(eventID), targetStateID));
		initialState.writeToFile(bodyRAFile, nBytesPerState, nBytesPerStateID, nTransitionsPerState);

	}

	public long addState(String label, boolean marked) {
		return addState(label, marked, new ArrayList<Transition>());
	}

	/**
	 *	Add the specified state ot the automaton
	 *	@param 	UNFINISHED!!!!!!!!!!!!!
	 *	@return the ID of the added state (0 indicates the addition was unsuccessful)
	 **/
	public long addState(String label, boolean marked, ArrayList<Transition> transitions) {

		// Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs)
		if (nStates == MAX_NUMBER_OF_STATES)
			return 0;

		// Increase the maximum allowed transitions per state
		if (transitions.size() > nTransitionsPerState) {
			nTransitionsPerState = transitions.size();
			recreateBinaryFile();
		}

		// Write new state to file
		State state = new State(label, ++nStates, marked, transitions);
		state.writeToFile(bodyRAFile, nBytesPerState, nBytesPerStateID, nTransitionsPerState);

		// Check to see if we need to re-write the entire binary file
		if (nStates > currentMaxStates) {

			// Adjust variables
			currentMaxStates = (currentMaxStates * 2) +  1;
			nBytesPerStateID++;
			updateNumberBytesPerState();

			// Re-create binary file
			recreateBinaryFile();

		}

		return nStates;
	}

	/**
	 * Re-calculate the amount of space required to store the transitions of a state
	 **/
	private void updateNumberBytesPerState() {
		nBytesPerState = nTransitionsPerState * (Event.N_BYTES_OF_ID + nBytesPerStateID);
	}

	/**
	 *	Add the specified event to the set (events with identical labels and different properties are considered unique)
	 *	@return the ID of the added event (0 indicates the addition was unsuccessful, which means the set did not change in size)
	 **/
	public int addEvent(String label, boolean observable, boolean controllable) {

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
		return originalSize != events.size() ? events.size() : 0;

	}

	private void recreateBinaryFile() {

		System.out.println("RECREATE BINARY FILE NOT IMPLEMENTED YET!!");

	}

    	/** STANDARD ACCESSOR AND MUTATOR METHODS **/  

    /**
     *	Given the ID number of a state, get the state information
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return State.readFromFile(this, bodyRAFile, id, nBytesPerState, nBytesPerStateID, nTransitionsPerState);
    }

    /**
     *	TEMPORARY SOLUTION: Later we will read this directly from the .hdr file.
     *	Given the ID number of an event, get the event information
	 *	@param id - The unique identifier corresponding to the requested event
	 *	@return state - the requested event (or null if it does not exist)
     **/
    public Event getEvent(int id) {

    	for (Event e : events) {
    		if (e.getID() == id)
    			return e;
    	}

    	return null;

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