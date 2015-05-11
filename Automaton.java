import java.util.*;
import java.io.*;
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;

public class Automaton {

		/* Class constants */

	private static final int HEADER_SIZE = 24; // This is the fixed amount of space needed to hold the main variables in the .hdr file

	public static final long DEFAULT_STATE_CAPACITY = 255;
	public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;
	public static final int DEFAULT_TRANSITION_CAPACITY = 1;
	public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;
	public static final int DEFAULT_LABEL_LENGTH = 1;
	public static final int MAX_LABEL_LENGTH = 100;
	public static final long LIMIT_OF_STATES_FOR_PICTURE = 10000; // Arbitrary value which will be revised once we have tried generating large automata

		/* Private instance variables */

	private Set<Event> 	events = new HashSet<Event>(),
						activeEvents = new HashSet<Event>(),
						controllableEvents = new HashSet<Event>(),
						observableEvents = new HashSet<Event>();

	private long nStates = 0;
	
	// Variables which determine how large the .bdy file will become
	private long stateCapacity;
	private int transitionCapacity = 2;
	private int labelLength;

	// Initialized based on the above capacities
	private int nBytesPerStateID;
	private long nBytesPerState;


	// Files
	private static final File 	DEFAULT_HEADER_FILE = new File("temp.hdr"),
								DEFAULT_BODY_FILE = new File("temp.bdy");
	private File 	headerFile,
					bodyFile;
	private RandomAccessFile 	headerRAFile, // Contains basic information about automaton, needed in order to read the bodyFile, as well as the events
								bodyRAFile;	// List each state in the automaton, with the transitions

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton
     **/
    public Automaton() {
    	this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, DEFAULT_STATE_CAPACITY, DEFAULT_TRANSITION_CAPACITY, DEFAULT_LABEL_LENGTH);
    }

    /**
     *	Implicit constructor: create automaton with specified initial capacities
     *	NOTE: 	Choosing larger values increases the amount of space needed to store the binary file.
     *			Choosing smaller values increases the frequency that you need to re-write the entire binary file in order to expand it
	 *	@param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *			NOTE: the initial state capacity may be higher than the value you give it, since it has to be 256^x
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exeeded)
	 *	@param labelLength - The initial maximum number characters per state label (increases by 1 whenever it is exeeded)
     **/
    public Automaton(long stateCapacity, int transitionCapacity, int labelLength) {
    	this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, stateCapacity, transitionCapacity, labelLength);
    }

    /**
     *	Implicit constructor: create automaton from a binary file
	 *	@param headerFile - The binary file to load the header information of the automaton from (information about events, etc.)
	 *	@param bodyFile - The binary file to load the body information of the automaton from (states and transitions)
	 *  @param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exeeded)
	 *	@param labelLength - The initial maximum number characters per state label (increases by 1 whenever it is exeeded)
     **/
	public Automaton(File headerFile, File bodyFile, long stateCapacity, int transitionCapacity, int labelLength) {

		this.headerFile = headerFile;
		this.bodyFile = bodyFile;

		// Will be overriden if we are loading information from file
		this.stateCapacity = stateCapacity;
		this.transitionCapacity = transitionCapacity;
		this.labelLength = labelLength;

		// The automaton needs at least 1 transition per state (specifically, since we store the marked status bit in it)
		if (this.transitionCapacity < 1)
			this.transitionCapacity = 1;

		// The requested length of the state labels should not exceed the limit, nor should it be non-positive
		if (this.labelLength < 1)
			this.labelLength = 1;
		if (this.labelLength > MAX_LABEL_LENGTH)
			this.labelLength = MAX_LABEL_LENGTH;
		
		// Open data from files (if it exists)
		openRAFiles();

	    // Finish setting up
	    initializeVariables();
    	updateNumberBytesPerState();

    	// Update header file
		writeHeaderFile();

    }

    	/** AUTOMATA OPERATIONS **/

    static Automaton intersection(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    static Automaton union(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    	/** IMAGE GENERATION **/

    public boolean outputDOT() {

    		/* Abort the operation if the automaton is too large to do this in a reasonable amount of time */
    	
    	if (nStates > LIMIT_OF_STATES_FOR_PICTURE) {
    		System.out.println("ERROR: Aborted due to the fact that this graph is quite large!");
    		return false;
    	}

    		/* Setup */

    	StringBuilder str = new StringBuilder();
    	str.append("digraph G {");
    	str.append("node [shape=circle, style=bold];");
    	str.append("size=\"4,4\";");
    	str.append("ratio=fill;");
    	
    		/* Draw all states and their transitions */

    	for (long s = 1; s <= nStates; s++) {

    		// Get state from file
    		State state = getState(s);

    		// Draw states
    		if (state.isMarked())
    			str.append(state.getLabel() + " [peripheries=2];");
    		else
    			str.append(state.getLabel()  + " [peripheries=1];");

    		// Draw each of its transitions
    		for (Transition t : state.getTransitions()) {
    			str.append(state.getLabel() + "->" + getState(t.getTargetStateID()).getLabel());
    			str.append(" [constraint=false,label=\"" + t.getEvent().getLabel() + "\"");
    			
    			if (!t.getEvent().isObservable())
    				str.append(",style=dotted");

    			if (!t.getEvent().isControllable())
    				str.append(",color=red");

    			str.append("];");
    		}

    	}

    		/* Finish up */

    	// Create arrow towards initial state (currently assumed to be the first state)
    	if (nStates > 0) {
    		str.append("node [shape=plaintext];");
    		str.append("entry->1;");
    	}

    	str.append("}");

    	// Generate image
    	try {

    		// Write DOT language to file
	    	PrintStream out = new PrintStream(new FileOutputStream("out.tmp"));
			out.print(str.toString());

			// Produce PNG from DOT language
	        Process process = new ProcessBuilder("dot", "-Tpng", "out.tmp", "-o", "image.png").start();

	        // Wait for it to finish
	        process.waitFor();

	    } catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
    	
    	return true;

    }

    public BufferedImage loadImageFromFile() {

    	try {
			return ImageIO.read(getClass().getResource("image.png"));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	private void openRAFiles() {

		try {

			headerRAFile = new RandomAccessFile(headerFile, "rw");
			bodyRAFile = new RandomAccessFile(bodyFile, "rw");

			readHeaderFile();

		} catch (IOException e) {
            e.printStackTrace();
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

			// Update body file
			recreateBodyFile();

			// Update header file
			writeHeaderFile();

		}

		// Add transition and update the file
		initialState.addTransition(new Transition(getEvent(eventID), targetStateID));
		initialState.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity);

		return true;

	}

	/**
	 *	Add the specified state to the automaton with an empty transition list
	 *	@param 	label - The "name" of the new state
	 *	@param 	marked - Whether or not the states is marked
	 *	@return the ID of the added state (0 indicates the addition was unsuccessful)
	 **/
	public long addState(String label, boolean marked) {
		return addState(label, marked, new ArrayList<Transition>());
	}

	/**
	 *	Add the specified state to the automaton
	 *	@param 	label - The "name" of the new state
	 *	@param 	marked - Whether or not the states is marked
	 *	@param 	transitions - The list of transitions
	 *	@return the ID of the added state (0 indicates the addition was unsuccessful)
	 **/
	public long addState(String label, boolean marked, ArrayList<Transition> transitions) {

		// Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs)
		if (nStates == MAX_STATE_CAPACITY)
			return 0;

		// Increase the maximum allowed characters per state label
		if (label.length() > labelLength) {

			// If we cannot increase the capacity, indicate a failure
			if (label.length() > MAX_LABEL_LENGTH)
				return 0;

			labelLength = label.length();
			recreateBodyFile();

		}

		// Increase the maximum allowed transitions per state
		if (transitions.size() > transitionCapacity) {

			// If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
			if (transitions.size() > MAX_TRANSITION_CAPACITY)
				return 0;

			transitionCapacity = transitions.size();
			recreateBodyFile();
		}

		// Write new state to file
		State state = new State(label, ++nStates, marked, transitions);
		state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity);

		// Check to see if we need to re-write the entire binary file
		if (nStates > stateCapacity) {

			// Adjust variables
			stateCapacity = ((stateCapacity + 1) << 8) - 1;
			nBytesPerStateID++;
			updateNumberBytesPerState();

			// Re-create binary file
			recreateBodyFile();

		}

		// Update header file
		writeHeaderFile();

		return nStates;
	}

	/**
	 * Re-calculate the amount of space required to store the transitions of a state
	 **/
	private void updateNumberBytesPerState() {
		nBytesPerState =
			1 // Whether the state is marked or not
			+ (long) labelLength // The state's labels
			+ (long) transitionCapacity * (long) (Event.N_BYTES_OF_ID + nBytesPerStateID); // All of the state transitions
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
	 *	Add the specified event to the set (events with identical labels and different properties are currently considered unique)
	 *	@param label - The "name" of the new event
	 *	@param observable - Whether or not the event is observable
	 *	@param controllable - Whether or not the event is controllable
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

		// Update header file
		writeHeaderFile();

		// If the number of events have changed, that means that this was a unique event
		return originalSize != events.size() ? events.size() : 0;

	}

	/**
	 *	Write all of the header information to file.
	 **/
	private void writeHeaderFile() {

			/* Write the header of the .hdr file */
		
		byte[] bytesToWrite = new byte[HEADER_SIZE];

		ByteManipulator.writeLongAsBytes(bytesToWrite, 0, nStates, 8);
		ByteManipulator.writeLongAsBytes(bytesToWrite, 8, stateCapacity, 8);
		ByteManipulator.writeLongAsBytes(bytesToWrite, 12, transitionCapacity, 4);
		ByteManipulator.writeLongAsBytes(bytesToWrite, 16, labelLength, 4);
		ByteManipulator.writeLongAsBytes(bytesToWrite, 20, events.size(), 4);

		try {

			headerRAFile.seek(0);
			headerRAFile.write(bytesToWrite);

				/* Write the events to the .hdr file */

			for (int e = 1; e <= events.size(); e++) {

				Event event = getEvent(e); // This could be made a lot more efficient!!!!!!!!!!!! Perhaps turn HashSet into a TreeSet so that we can iterate the events by ID?

				// Fill the buffer
				bytesToWrite = new byte[2 + 4 + event.getLabel().length()];

				// Read event properties
				bytesToWrite[0] = (byte) (event.isObservable() ? 1 : 0);
				bytesToWrite[1] = (byte) (event.isControllable() ? 1 : 0);

				// Write the length of the label
				ByteManipulator.writeLongAsBytes(bytesToWrite, 2, event.getLabel().length(), 4);

				// Write each character of the label
				int index = 6;
				for (int i = 0; i < event.getLabel().length(); i++) {
					bytesToWrite[index++] = (byte) event.getLabel().charAt(i);
					System.out.println("write char:" + ((byte) event.getLabel().charAt(i)));
				}

				headerRAFile.write(bytesToWrite);

			}

		} catch (IOException e) {
            e.printStackTrace();
	    }	

	}

	/**
	 *	Read all of the header information from file.
	 **/
	private void readHeaderFile() {

		byte[] bytesRead = new byte[HEADER_SIZE];

		try {

			if (headerRAFile.length() == 0) {
				System.out.println("Could not load the automaton from file, because there was nothing stored there.");
				return;
			}

			headerRAFile.seek(0);
			headerRAFile.read(bytesRead);

			nStates = ByteManipulator.readBytesAsLong(bytesRead, 0, 8);
			stateCapacity = ByteManipulator.readBytesAsLong(bytesRead, 8, 8);
			transitionCapacity = (int) ByteManipulator.readBytesAsLong(bytesRead, 12, 4);
			labelLength = (int) ByteManipulator.readBytesAsLong(bytesRead, 16, 4);
			int nEvents = (int) ByteManipulator.readBytesAsLong(bytesRead, 20, 4);

			// Read in the events
			for (int e = 1; e <= nEvents; e++) {

				// Read properties
				boolean observable = (headerRAFile.readByte()) == 1;
				boolean controllable = (headerRAFile.readByte()) == 1;

				// Read the number of characters in the label
				bytesRead = new byte[4];
				headerRAFile.read(bytesRead);
				int eventLabelLength = (int) ByteManipulator.readBytesAsLong(bytesRead, 0, 4);

				// Read each character of the label, building an array of characters
				bytesRead = new byte[eventLabelLength];
				headerRAFile.read(bytesRead);
				char[] arr = new char[eventLabelLength];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (char) bytesRead[i];

				// Crete the event and add it to the list
				addEvent(new String(arr), observable, controllable);

			}

		} catch (IOException e) {
            e.printStackTrace();
	    }	

	}

	private void recreateBodyFile() {


		System.out.println("RECREATE BINARY FILE NOT IMPLEMENTED YET!! Dumping stack..");
		Thread.dumpStack();

	}

		/** GUI INPUT CODE GENERATION **/

	StringBuilder 	eventInputBuilder,
					stateInputBuilder,
					transitionInputBuilder;

	public void generateInputForGUI() {

		eventInputBuilder = new StringBuilder();
		stateInputBuilder = new StringBuilder();
		transitionInputBuilder = new StringBuilder();

			/* Generate event input */

		for (int e = 1; e <= events.size(); e++) {

			Event event = getEvent(e); // This could be made a lot more efficient!!!!!!!!!!!! Perhaps turn HashSet into a TreeSet so that we can iterate the events by ID?

			eventInputBuilder.append(event.getLabel());
			eventInputBuilder.append((event.isObservable() ? ",T" : ",F"));
			eventInputBuilder.append((event.isControllable() ? ",T" : ",F"));
			
			if (e < events.size())
				eventInputBuilder.append("\n");

		}

			/* Generate state and transition input */

		for (int s = 1; s <= nStates; s++) {

			State state = getState(s);

			stateInputBuilder.append(state.getLabel());
			stateInputBuilder.append((state.isMarked() ? ",T" : ",F"));
			
			if (s < nStates)
				stateInputBuilder.append("\n");

			for (Transition t : state.getTransitions())
				transitionInputBuilder.append(state.getLabel() + "," + t.getEvent().getLabel() + "," + t.getTargetState().getLabel());

		}

	}

	public String getEventInput() {

		if (eventInputBuilder == null)
			return null;

		return eventInputBuilder.toString();

	}

	public String getStateInput() {

		if (stateInputBuilder == null)
			return null;

		return stateInputBuilder.toString();

	}

	public String getTransitionInput() {

		if (transitionInputBuilder == null)
			return null;

		return transitionInputBuilder.toString();

	}

    	/** ACCESSOR METHODS **/  

    /**
     *	Given the ID number of a state, get the state information
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return State.readFromFile(this, bodyRAFile, id);
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

    public int getLabelLength() {
    	return labelLength;
    }

    public int getSizeOfStateID() {
    	return nBytesPerStateID;
    }

    public long getSizeOfState() {
    	return nBytesPerState;
    }

}