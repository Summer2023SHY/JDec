import java.util.*;
import java.io.*;
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;

public class Automaton {

		/* Class constants */

	public static final long DEFAULT_STATE_CAPACITY = 255;
	public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;
	public static final int DEFAULT_TRANSITION_CAPACITY = 1;
	public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;
	public static final long LIMIT_OF_STATES_FOR_PICTURE = 10000; // Arbitrary value which will be revised once we have tried generating large automata

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
	 * 	NOTE: Whenever nBytesPerStateID or transitionCapacity is changed, this affects nBytesPerState, which means the binary file needs to be recreated.
	 **/

	private long nStates = 0;
	
	// Variables which determine how large the .bdy file will become
	private long stateCapacity;
	private int transitionCapacity = 2;

	// Initialized based on the above capacities
	private int nBytesPerStateID;
	private long nBytesPerState;


	// Files
	private static File defaultHeaderFile = new File("temp.hdr"),
						defaultBodyFile = new File("temp.bdy");
	private RandomAccessFile 	headerRAFile, // Contains basic information about automaton, needed in order to read the bodyFile
								bodyRAFile;	// List each state in the automaton, with the transitions

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton
     **/
    public Automaton() {
    	this(defaultHeaderFile, defaultBodyFile, DEFAULT_STATE_CAPACITY, DEFAULT_TRANSITION_CAPACITY);
    }

    /**
     *	Implicit constructor: create automaton with specified initial capacities
     *	NOTE: 	Choosing larger values increases the amount of space needed to store the binary file.
     *			Choosing smaller values increases the frequency that you need to re-write the entire binary file in order to expand it
	 *	@param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *			NOTE: the initial state capacity may be higher than the value you give it, since it has to be 256^x
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exeeded)
     **/
    public Automaton(long stateCapacity, int transitionCapacity) {
    	this(defaultHeaderFile, defaultBodyFile, stateCapacity, transitionCapacity);
    }

    /**
     *	Implicit constructor: create automaton from a binary file
	 *	@param headerFile - The binary file to load the header information of the automaton from (information about events, etc.)
	 *	@param bodyFile - The binary file to load the body information of the automaton from (states and transitions)
	 *  @param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exeeded)
     **/
	public Automaton(File headerFile, File bodyFile, long stateCapacity, int transitionCapacity) {

		// Will be overriden if we are loading information from file
		this.stateCapacity = stateCapacity;
		this.transitionCapacity = transitionCapacity;

		// It does not make sense for an automaton to have 0 transitions (or a negative number of transitions)
		if (this.transitionCapacity < 1)
			this.transitionCapacity = 1;
		
		// Create file (TO-DO: CURRENTLY DOESN'T INITIALIZE AUTOMATON FROM FILE!!!)
		try {
			this.headerRAFile = new RandomAccessFile(headerFile, "rw");
			this.bodyRAFile = new RandomAccessFile(bodyFile, "rw");
		} catch (IOException e) {
            e.printStackTrace();
	    }	

	    // Finish setting up
	    initializeVariables();
    	updateNumberBytesPerState();

    }

    	/** AUTOMATA OPERATIONS **/

    static Automaton intersection(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    static Automaton union(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    	/** IMAGE GENERATION **/


    public void outputDOT() {

    	// Abort the operation if the automaton is too large to do this in a reasonable amount of time
    	if (nStates > LIMIT_OF_STATES_FOR_PICTURE) {
    		System.out.println("ERROR: Aborted due to the fact that this graph is large...!");
    		return;
    	}

    	StringBuilder str = new StringBuilder();

    	str.append("digraph G {");
    	str.append("size=\"4,4\";");
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
	        Process process = new ProcessBuilder("dot", "-Tpng", "out.tmp", "-o", "image.png").start();

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
			return null;
		}

	}

		/** MUTATOR METHODS **/  

	// CURRENTLY IN-EFFICENT!!!!!!!!!! (rewrites the entire state to file instead of only writing the new transition)
	
	public boolean addTransition(long initialStateID, int eventID, long targetStateID) {

		// Create initial state from ID
		State initialState = getState(initialStateID);

		// Increase the maximum allowed transitions per state
		if (initialState.getNumberOfTransitions() == transitionCapacity) {

			// If we cannot increase the capacity, return false (NOTE: This will likely never happen)
			if (transitionCapacity == MAX_TRANSITION_CAPACITY)
				return false;

			transitionCapacity++;
			recreateBinaryFile();
		}

		// Add transition and update the file
		initialState.addTransition(new Transition(getEvent(eventID), targetStateID));
		initialState.writeToFile(bodyRAFile, nBytesPerState, nBytesPerStateID, transitionCapacity);

		return true;

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
		if (nStates == MAX_STATE_CAPACITY)
			return 0;

		// Increase the maximum allowed transitions per state
		if (transitions.size() > transitionCapacity) {

			// If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
			if (transitions.size() > MAX_TRANSITION_CAPACITY)
				return 0;

			transitionCapacity = transitions.size();
			recreateBinaryFile();
		}

		// Write new state to file
		State state = new State(label, ++nStates, marked, transitions);
		state.writeToFile(bodyRAFile, nBytesPerState, nBytesPerStateID, transitionCapacity);

		// Check to see if we need to re-write the entire binary file
		if (nStates > stateCapacity) {

			// Adjust variables
			stateCapacity = ((stateCapacity + 1) << 8) - 1;
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
		nBytesPerState = (long) transitionCapacity * (long) (Event.N_BYTES_OF_ID + nBytesPerStateID);
	}

	private void initializeVariables() {

			/* Calculate the amount of space needed to store each state ID */

		// Special case if the state capacity is not positive
		nBytesPerStateID = stateCapacity < 1 ? 1 : 0;

		long temp = stateCapacity;
		
		while (temp > 0) {
			nBytesPerStateID++;
			temp >>= 8;
		}

			/* Calculate the maximum number of states that we can have before we have to allocate more space for each state ID */

		stateCapacity = 1;

		for (int i = 0; i < nBytesPerStateID; i++)
			stateCapacity <<= 8;

		// Special case when the user gives a value between 2^56 - 1 and 2^64 (exclusive)
		if (stateCapacity == 0)
			stateCapacity = MAX_STATE_CAPACITY;
		else
			stateCapacity--;

		// Cap the state capacity
		if (stateCapacity > MAX_STATE_CAPACITY)
			stateCapacity = MAX_STATE_CAPACITY;

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

    	/** ACCESSOR METHODS **/  

    /**
     *	Given the ID number of a state, get the state information
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return State.readFromFile(this, bodyRAFile, id, nBytesPerState, nBytesPerStateID, transitionCapacity);
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

    public long getNumberOfStates() {
    	return nStates;
    }

    public long getStateCapacity() {
    	return stateCapacity;
    }

    public int getTransitionCapacity() {
    	return transitionCapacity;
    }

    public int getSizeOfStateID() {
    	return nBytesPerStateID;
    }

    public long getSizeOfState() {
    	return nBytesPerState;
    }

}