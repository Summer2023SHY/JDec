/**
 * Automaton - 	This extensive class is able to fully represent an automaton. The usage of .hdr and .bdy files
 * 				gives the flexibility to work with very large automata, since the entire automaton does not need
 *				to be stored in memory.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *	-Class Constants
 *	-Instance Variables
 *	-Constructors
 *	-Automata Operations
 *	-Image Generation
 *	-GUI Input Code Generation
 *	-Working with Files
 *	-Miscellaneous
 *	-Mutator Methods
 *	-Accessor Methods
 **/

import java.util.*;
import java.io.*;
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;

public class Automaton {

		/** CLASS CONSTANTS **/

	private static final int HEADER_SIZE = 32; // This is the fixed amount of space needed to hold the main variables in the .hdr file

	public static final long DEFAULT_STATE_CAPACITY = 255;
	public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;
	public static final int DEFAULT_TRANSITION_CAPACITY = 1;
	public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;
	public static final int DEFAULT_LABEL_LENGTH = 1;
	public static final int MAX_LABEL_LENGTH = 100;

	public static final long LIMIT_OF_STATES_FOR_PICTURE = 10000; // Arbitrary value which will be revised once we have tried generating large automata

	private static final String DEFAULT_HEADER_FILE_NAME = "temp.hdr",
								DEFAULT_BODY_FILE_NAME = "temp.bdy";
	private static final File 	DEFAULT_HEADER_FILE = new File(DEFAULT_HEADER_FILE_NAME),
								DEFAULT_BODY_FILE = new File(DEFAULT_BODY_FILE_NAME);

		/** PRIVATE INSTANCE VARIABLES **/

	private Set<Event> 	events = new TreeSet<Event>(),
						activeEvents = new HashSet<Event>(),
						controllableEvents = new HashSet<Event>(),
						observableEvents = new HashSet<Event>();

	private long nStates = 0;
	private long initialState = 0;
	
	// Variables which determine how large the .bdy file will become
	private long stateCapacity;
	private int transitionCapacity = 2;
	private int labelLength;

	// Initialized based on the above capacities
	private int nBytesPerStateID;
	private long nBytesPerState;
	
	// File variables
	private String 	headerFileName = DEFAULT_HEADER_FILE_NAME,
					bodyFileName = DEFAULT_BODY_FILE_NAME;
	private File 	headerFile,
					bodyFile;
	private RandomAccessFile 	headerRAFile, // Contains basic information about automaton, needed in order to read the bodyFile, as well as the events
								bodyRAFile;	// List each state in the automaton, with the transitions

	// GUI input
	private StringBuilder 	eventInputBuilder,
							stateInputBuilder,
							transitionInputBuilder;

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton with default capacity
     **/
    public Automaton() {
    	this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, DEFAULT_STATE_CAPACITY, DEFAULT_TRANSITION_CAPACITY, DEFAULT_LABEL_LENGTH, true);
    }

    /**
     * Implicit constructor: load automaton from file
     **/
    public Automaton(File headerFile, boolean clearFiles) {
    	this(
    			(headerFile == null) ? DEFAULT_HEADER_FILE : headerFile,
    			(headerFile == null) ? DEFAULT_BODY_FILE : new File(headerFile.getName().substring(0, headerFile.getName().length() - 4) + ".bdy"),
    			DEFAULT_STATE_CAPACITY,
    			DEFAULT_TRANSITION_CAPACITY,
    			DEFAULT_LABEL_LENGTH,
    			clearFiles
    		);
    }

    /**
     *	Implicit constructor: create automaton with specified initial capacities
     *	NOTE: 	Choosing larger values increases the amount of space needed to store the binary file.
     *			Choosing smaller values increases the frequency that you need to re-write the entire binary file in order to expand it
	 *	@param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *			NOTE: the initial state capacity may be higher than the value you give it, since it has to be 256^x
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
	 *	@param labelLength - The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
	 *	@param clearFiles - Whether or not the header and body files should be cleared prior to use
     **/
    public Automaton(long stateCapacity, int transitionCapacity, int labelLength, boolean clearFiles) {
    	this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, stateCapacity, transitionCapacity, labelLength, clearFiles);
    }

    /**
     *	Implicit constructor: create automaton from a binary file
	 *	@param headerFile - The binary file to load the header information of the automaton from (information about events, etc.)
	 *	@param bodyFile - The binary file to load the body information of the automaton from (states and transitions)
	 *  @param stateCapacity - The initial state capacity (increases by a factor of 256 when it is exceeded)
	 *	@param transitionCapacity - The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
	 *	@param labelLength - The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
	 *	@param clearFiles - Whether or not the header and body files should be cleared prior to use
     **/

	public Automaton(File headerFile, File bodyFile, long stateCapacity, int transitionCapacity, int labelLength, boolean clearFiles) {

		this.headerFile = headerFile;
		this.bodyFile = bodyFile;
		this.headerFileName = headerFile.getName();
		this.bodyFileName = bodyFile.getName();

		// These variables will be overridden if we are loading information from file
		this.stateCapacity = stateCapacity;
		this.transitionCapacity = transitionCapacity;
		this.labelLength = labelLength;

		// The automaton should have room for at least 1 transition per state (otherwise our automaton will be pretty boring)
		if (this.transitionCapacity < 1)
			this.transitionCapacity = 1;

		// The requested length of the state labels should not exceed the limit, nor should it be non-positive
		if (this.labelLength < 1)
			this.labelLength = 1;
		if (this.labelLength > MAX_LABEL_LENGTH)
			this.labelLength = MAX_LABEL_LENGTH;

		// Clear files
		if (clearFiles)
			deleteFiles();
		
		// Open files and try to load data from header
		openRAFiles();

	    // Finish setting up
	    initializeVariables();
    	nBytesPerState = calculateNumberOfBytesPerState(nBytesPerStateID, this.transitionCapacity, this.labelLength);

    	// Update header file
		writeHeaderFile();

    }

    	/** AUTOMATA OPERATIONS **/

    /**
     * Create a new copy of this automaton that has any unreachable removed.
     **/
    public Automaton accessible() {

    		/* Setup */

    	Automaton automaton = new Automaton(new File("accessible.hdr"), true);
    	Stack<Long> stack = new Stack<Long>(); 

    	// Add the initial state to the stack
    	stack.push(getInitialStateID());

    		/* Build automaton from the accessible part of this automaton */

    	// Add events
    	for (Event e : events)
    		automaton.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    	// Add states and transition
    	while (stack.size() > 0) {

    		// Get next ID
    		long id = stack.pop();

    		// Error checking
    		if (id == 0) {
    			System.out.println("ERROR: Bad state ID.");
    			continue;
    		}

    		// This state has already been created in the new automaton, so it does not need to be created again
    		if (automaton.stateExists(id))
    			continue;

    		// Get state and transitions
    		State state = getState(id);
    		ArrayList<Transition> transitions = state.getTransitions();

    		// Add new state
    		automaton.addStateAt(
    				state.getLabel(),
    				state.isMarked(),
    				new ArrayList<Transition>(),
    				id == getInitialStateID(),
    				id
    			);

    		// Traverse each transition
    		for (Transition t : transitions) {

				// Add the target state to the stack
				stack.add(t.getTargetStateID());

				// Add transition to the new automaton
				automaton.addTransition(id, t.getEvent().getID(), t.getTargetStateID());

			}

    	}

    		/* Re-number states (by removing empty ones) */

    	automaton.renumberStates();

    		/* Return accessible automaton */

    	return automaton;
    }

    public static Automaton intersection(Automaton first, Automaton second) {

    		/* Setup */

    	Automaton automaton = new Automaton(new File("intersection.hdr"), true);

    	// These two stacks should always have the same size
    	Stack<Long> stack1 = new Stack<Long>(); 
    	Stack<Long> stack2 = new Stack<Long>();

    	// Add the initial states to the stack
    	stack1.push(first.getInitialStateID());
    	stack2.push(second.getInitialStateID());

    		/* Build product */

    	// Create event set (intersection of both event sets)
    	for (Event e : first.getEvents())
    		if (second.getEvents().contains(e))
    			automaton.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    	// Add states and transition
    	while (stack1.size() > 0) {

    		// Get next IDs
    		long id1 = stack1.pop();
    		long id2 = stack2.pop();

    		// Error checking
    		if (id1 == 0 || id2 == 0) {
    			System.out.println("ERROR: Bad state ID.");
    			continue;
    		}

    		// Create combined ID
    		long newStateID = calculateCombinedID(id1, first, id2, second);

    		// This state has already been created, so it does not need to be created again
    		if (automaton.stateExists(newStateID))
    			continue;

    		// Get states and transitions
    		State state1 = first.getState(id1);
    		State state2 = second.getState(id2);
    		ArrayList<Transition> transitions1 = state1.getTransitions();
    		ArrayList<Transition> transitions2 = state2.getTransitions();

    		// Add new state
    		automaton.addStateAt(
    				state1.getLabel() + "_" + state2.getLabel(),
    				state1.isMarked() && state2.isMarked(),
    				new ArrayList<Transition>(),
    				id1 == first.getInitialStateID() && id2 == second.getInitialStateID(),
    				newStateID
    			);

    		// Find every pair of transitions that have the same events
    		for (Transition t1 : transitions1)
    			for (Transition t2 : transitions2)
    				if (t1.getEvent().equals(t2.getEvent())) {

    					// Add this pair to the stack
    					stack1.add(t1.getTargetStateID());
    					stack2.add(t2.getTargetStateID());

    					// Add transition to the new automaton
    					long targetID = calculateCombinedID(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
    					automaton.addTransition(newStateID, t1.getEvent().getID(), targetID);

    				}

    	}

    		/* Re-number states (by removing empty ones) */

    	automaton.renumberStates();

    		/* Return produced automaton */

    	return automaton;
    }

    public static Automaton union(Automaton first, Automaton second) {

    		/* Setup */

    	Automaton automaton = new Automaton(new File("union.hdr"), true);

    	// These two stacks should always have the same size
    	Stack<Long> stack1 = new Stack<Long>(); 
    	Stack<Long> stack2 = new Stack<Long>();

    	// Add the initial states to the stack
    	stack1.push(first.getInitialStateID());
    	stack2.push(second.getInitialStateID());

    		/* Build automata by parallel composition */

    	// Create two sets containing each automata's private events
    	Set<Event> privateEvents1 = new TreeSet<Event>();
    	privateEvents1.addAll(first.getEvents());
    	privateEvents1.removeAll(second.getEvents());
    	Set<Event> privateEvents2 = new TreeSet<Event>();
    	privateEvents2.addAll(second.getEvents());
    	privateEvents2.removeAll(first.getEvents());

    	// Create event set (union of both event sets)
    	for (Event e : first.getEvents())
    		automaton.addEvent(e.getLabel(), e.isObservable(), e.isControllable());
    	for (Event e : second.getEvents())
    		automaton.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    	// Add states and transition
    	while (stack1.size() > 0) {

    		// Get next IDs
    		long id1 = stack1.pop();
    		long id2 = stack2.pop();

    		// Error checking
    		if (id1 == 0 || id2 == 0) {
    			System.out.println("ERROR: Bad state ID.");
    			continue;
    		}

    		// Create combined ID
    		long newStateID = calculateCombinedID(id1, first, id2, second);

    		// This state has already been created, so it does not need to be created again
    		if (automaton.stateExists(newStateID))
    			continue;

    		// Get states and transitions
    		State state1 = first.getState(id1);
    		State state2 = second.getState(id2);
    		ArrayList<Transition> transitions1 = state1.getTransitions();
    		ArrayList<Transition> transitions2 = state2.getTransitions();

    		// Add new state
    		automaton.addStateAt(
    				state1.getLabel() + "_" + state2.getLabel(),
    				state1.isMarked() && state2.isMarked(),
    				new ArrayList<Transition>(),
    				id1 == first.getInitialStateID() && id2 == second.getInitialStateID(),
    				newStateID
    			);

    		// Find every pair of transitions that have the same events (this accounts for public events)
    		for (Transition t1 : transitions1)
    			for (Transition t2 : transitions2)
    				if (t1.getEvent().equals(t2.getEvent())) {

						// Add this pair to the stack
    					stack1.add(t1.getTargetStateID());
    					stack2.add(t2.getTargetStateID());

    					// Add transition to the new automaton
    					long targetID = calculateCombinedID(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
    					automaton.addTransition(newStateID, t1.getEvent().getID(), targetID);

    				}

    		// Take care of the first automaton's private events
    		for (Transition t : transitions1)
    			if (privateEvents1.contains(t.getEvent())) {

					// Add the pair of states to the stack
					stack1.add(t.getTargetStateID());
					stack2.add(id2);

					// Add transition to the new automaton
					long targetID = calculateCombinedID(t.getTargetStateID(), first, id2, second);
					automaton.addTransition(newStateID, t.getEvent().getID(), targetID);

    			}

    		// Take care of the second automaton's private events
    		for (Transition t : transitions2)
    			if (privateEvents2.contains(t.getEvent())) {

					// Add the pair of states to the stack
					stack1.add(id1);
					stack2.add(t.getTargetStateID());

					// Add transition to the new automaton
					long targetID = calculateCombinedID(id1, first, t.getTargetStateID(), second);
					automaton.addTransition(newStateID, t.getEvent().getID(), targetID);

    			}

    	}

    		/* Re-number states (by removing empty ones) */

    	automaton.renumberStates();

    		/* Return generated automaton */

    	return automaton;

    }

	/**
	 * This method looks for blank spots in the .bdy file (which indicates that no state exists there),
	 * and re-numbers all of the states accordingly. This must be done after operations such as intersection or union.
	 * NOTE: To make this method more efficient we could make the buffer larger.
	 **/
    private void renumberStates() {

		try {

				/* Create a file containing the mappings (where the new IDs can be indexed using the old IDs) */

			File mappingFile = new File("mappings.tmp");
			RandomAccessFile mappingRAFile = new RandomAccessFile(mappingFile, "rw");

			long newID = 1;
			for (long s = 1; s <= stateCapacity; s++)
				if (stateExists(s)) {
					byte[] buffer = new byte[nBytesPerStateID];
					mappingRAFile.seek(nBytesPerStateID * s);
					ByteManipulator.writeLongAsBytes(buffer, 0, newID++, nBytesPerStateID);
					mappingRAFile.write(buffer);
				}

				/* Create new .bdy file with renumbered states */

			File newBodyFile = new File("body.tmp");
			RandomAccessFile newBodyRAFile = new RandomAccessFile(newBodyFile, "rw");

			for (long s = 1; s <= stateCapacity; s++) {

				State state = null;

				if ( (state = getState(s)) != null ) {
					
					// Get new ID of state
					byte[] buffer = new byte[nBytesPerStateID];
					mappingRAFile.seek(nBytesPerStateID * s);
					mappingRAFile.read(buffer);
					long newStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

					// Update ID of state
					state.setID(newStateID);

					// Update IDs of the target state of each
					for (Transition t : state.getTransitions()) {

						// Get new id of state
						buffer = new byte[nBytesPerStateID];
						mappingRAFile.seek(nBytesPerStateID * t.getTargetStateID());
						mappingRAFile.read(buffer);
						long newTargetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

						t.setTargetStateID(newTargetStateID);
					}

					// Write the updated state to the new file
					if (!state.writeToFile(newBodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity))
						System.out.println("ERROR: Could not write state to file.");

				}

			}

				/* Remove old body file and mappings file */

			try {

				bodyRAFile.close();
	    		bodyFile.delete();
	    		mappingFile.delete();

	    	} catch (SecurityException e) {
	    		e.printStackTrace();
	    	}
	    		/* Rename new body file */

			newBodyFile.renameTo(new File(bodyFileName));
			bodyRAFile = newBodyRAFile;

	    } catch (IOException e) {
	    		e.printStackTrace();
    	}

    }

    // Unique ID created in a way that no other combination of valid id1 and id2 from the same pair of automatons will map to this ID
    private static long calculateCombinedID(long id1, Automaton first, long id2, Automaton second) {

    	return ((id2 - 1) * first.getNumberOfStates() + id1);

    }

    	/** IMAGE GENERATION **/

    /**
     * Output this automaton in a format that is readable by GraphViz's dot program, which is used to generate the graph's image.
     * @param size - The requested width and height in pixels
     **/
    public boolean outputDOT(int size) {

    		/* Abort the operation if the automaton is too large to do this in a reasonable amount of time */
    	
    	if (nStates > LIMIT_OF_STATES_FOR_PICTURE) {
    		System.out.println("ERROR: Aborted due to the fact that this graph is quite large!");
    		return false;
    	}

    		/* Setup */

    	StringBuilder str = new StringBuilder();
    	str.append("digraph G {");
    	str.append("node [shape=circle, style=bold];");
    	double inches = ((double) size) / 96.0; // Assuming DPI is 96
    	str.append("size=\"" + inches + "," + inches + "\";");
    	str.append("ratio=fill;");
    	
    		/* Draw all states and their transitions */

    	for (long s = 1; s <= nStates; s++) {

    		// Get state from file
    		State state = getState(s);

    		// Draw state
    		if (state.isMarked())
    			str.append(state.getLabel() + " [peripheries=2];");
    		else
    			str.append(state.getLabel()  + " [peripheries=1];");

    		// Draw each of its transitions
    		for (Transition t : state.getTransitions()) {
    			str.append(state.getLabel() + "->" + State.readLabelFromFile(this, bodyRAFile, t.getTargetStateID()));
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
    	if (initialState > 0) {
    		str.append("node [shape=plaintext];");
    		str.append("entry->" + State.readLabelFromFile(this, bodyRAFile, initialState) + ";");
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

		/** GUI INPUT CODE GENERATION **/

	public void generateInputForGUI() {

		eventInputBuilder = new StringBuilder();
		stateInputBuilder = new StringBuilder();
		transitionInputBuilder = new StringBuilder();

			/* Generate event input */

		int counter = 0;
		for (Event e : events) {

			eventInputBuilder.append(e.getLabel());
			eventInputBuilder.append((e.isObservable() ? ",T" : ",F"));
			eventInputBuilder.append((e.isControllable() ? ",T" : ",F"));
			
			if (++counter < events.size())
				eventInputBuilder.append("\n");

		}

			/* Generate state and transition input */

		boolean firstTransitionInStringBuilder = true;

		for (long s = 1; s <= nStates; s++) {

			State state = getState(s);

			if (state == null) {
				System.out.println("ERROR: State could not be loaded..");
				continue;
			}

			// Place asterisk before label if this is the initial state
			if (s == initialState)
				stateInputBuilder.append("*");

			// Append label and properties
			stateInputBuilder.append(state.getLabel());
			stateInputBuilder.append((state.isMarked() ? ",T" : ",F"));
			
			// Add line separator after unless this is the last state
			if (s < nStates)
				stateInputBuilder.append("\n");

			// Append all transitions
			for (Transition t : state.getTransitions()) {

				// Add line separator before unless this is the very first transition
				if (firstTransitionInStringBuilder)
					firstTransitionInStringBuilder = false;
				else
					transitionInputBuilder.append("\n");

				// Append transition
				transitionInputBuilder.append(
						state.getLabel()
						+ "," + t.getEvent().getLabel()
						+ "," + State.readLabelFromFile(this, bodyRAFile, t.getTargetStateID())
					);
			
			}
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

		/** WORKING WITH FILES **/

	private void openRAFiles() {

		try {

			headerRAFile = new RandomAccessFile(headerFile, "rw");
			bodyRAFile = new RandomAccessFile(bodyFile, "rw");

			readHeaderFile();

		} catch (IOException e) {
            e.printStackTrace();
	    }	

	}

	private void deleteFiles() {

		try {

    		headerFile.delete();
    		bodyFile.delete();

    	} catch (SecurityException e) {
    		e.printStackTrace();
    	}

	}

	/**
	 * Delete the temporary files (if they exist)
	 **/
	public static void clearTemporaryFiles() {

    	try {

    		DEFAULT_HEADER_FILE.delete();
    		DEFAULT_BODY_FILE.delete();

    	} catch (SecurityException e) {
    		e.printStackTrace();
    	}

    }

    /**
     *	Write all of the header information to file.
	 **/
	private void writeHeaderFile() {

			/* Write the header of the .hdr file */
		
		byte[] buffer = new byte[HEADER_SIZE];

		ByteManipulator.writeLongAsBytes(buffer, 0, nStates, 8);
		ByteManipulator.writeLongAsBytes(buffer, 8, stateCapacity, 8);
		ByteManipulator.writeLongAsBytes(buffer, 12, transitionCapacity, 4);
		ByteManipulator.writeLongAsBytes(buffer, 16, labelLength, 4);
		ByteManipulator.writeLongAsBytes(buffer, 20, initialState, 8);
		ByteManipulator.writeLongAsBytes(buffer, 28, events.size(), 4);

		try {

			headerRAFile.seek(0);
			headerRAFile.write(buffer);

				/* Write the events to the .hdr file */

			for (Event e : events) {
			
				// Fill the buffer
				buffer = new byte[2 + 4 + e.getLabel().length()];

				// Read event properties
				buffer[0] = (byte) (e.isObservable() ? 1 : 0);
				buffer[1] = (byte) (e.isControllable() ? 1 : 0);

				// Write the length of the label
				ByteManipulator.writeLongAsBytes(buffer, 2, e.getLabel().length(), 4);

				// Write each character of the label
				int index = 6;
				for (int i = 0; i < e.getLabel().length(); i++)
					buffer[index++] = (byte) e.getLabel().charAt(i);

				headerRAFile.write(buffer);

			}

		} catch (IOException e) {
            e.printStackTrace();
	    }	

	}

	/**
	 *	Read all of the header information from file.
	 **/
	private void readHeaderFile() {

		byte[] buffer = new byte[HEADER_SIZE];

		try {

			// Do not try to load an empty file
			if (headerRAFile.length() == 0)
				return;

			// Go to the proper position and read in the bytes
			headerRAFile.seek(0);
			headerRAFile.read(buffer);

			// Calculate the values stored in these bytes
			nStates = ByteManipulator.readBytesAsLong(buffer, 0, 8);
			stateCapacity = ByteManipulator.readBytesAsLong(buffer, 8, 8);
			transitionCapacity = (int) ByteManipulator.readBytesAsLong(buffer, 12, 4);
			labelLength = (int) ByteManipulator.readBytesAsLong(buffer, 16, 4);
			initialState = ByteManipulator.readBytesAsLong(buffer, 20, 8);
			int nEvents = (int) ByteManipulator.readBytesAsLong(buffer, 28, 4);

			// Read in the events
			for (int e = 1; e <= nEvents; e++) {

				// Read properties
				boolean observable = (headerRAFile.readByte()) == 1;
				boolean controllable = (headerRAFile.readByte()) == 1;

				// Read the number of characters in the label
				buffer = new byte[4];
				headerRAFile.read(buffer);
				int eventLabelLength = (int) ByteManipulator.readBytesAsLong(buffer, 0, 4);

				// Read each character of the label, building an array of characters
				buffer = new byte[eventLabelLength];
				headerRAFile.read(buffer);
				char[] arr = new char[eventLabelLength];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (char) buffer[i];

				// Crete the event and add it to the list
				addEvent(new String(arr), observable, controllable);

			}

		} catch (IOException e) {
            e.printStackTrace();
	    }	

	}

	private void recreateBodyFile(long newStateCapacity, int newTransitionCapacity, int newLabelLength, int newNBytesPerStateID, long newNBytesPerState) {

		System.out.println("DEBUG: Re-creating body file.");

			/* Setup files */

		File newBodyFile = new File(".tmp");
		RandomAccessFile newBodyRAFile = null;

		try {
		
			newBodyRAFile = new RandomAccessFile(newBodyFile, "rw");

		} catch (FileNotFoundException e) {
    		e.printStackTrace();
    		return;
    	}

			/* Copy over body file */

		long counter = 0; // Keeps track of blank states
		byte[] buffer = new byte[(int) nBytesPerState];

		for (long s = 1; s <= nStates + counter; s++) {
			State state = getState(s);

			// Check for non-existent state
			if (state == null) {

				// Pad with zeroes, which will indicate a non-existent state
				try {
					newBodyRAFile.write(buffer);
				} catch (IOException e) {
	    			e.printStackTrace();
		    	}

				counter++;
				
				continue;	
			}

			// Try writing to file
			if (!state.writeToFile(newBodyRAFile, newNBytesPerState, newLabelLength, newNBytesPerStateID, newTransitionCapacity)) {
				System.out.println("ERROR: Could not write copy over state to file. Aborting re-creation of .bdy file.");
				return;
			}
		}

			/* Remove old file, rename new one */

		try {
			bodyRAFile.close();
    		bodyFile.delete();
    	} catch (SecurityException | IOException e) {
    		e.printStackTrace();
    	}

		newBodyFile.renameTo(new File(bodyFileName));

			/* Update variables */

		stateCapacity = newStateCapacity;
		transitionCapacity = newTransitionCapacity;
		labelLength = newLabelLength;
		nBytesPerStateID = newNBytesPerStateID;
		nBytesPerState = newNBytesPerState;

		bodyRAFile = newBodyRAFile;

	}

		/** MISCELLANEOUS **/

	/**
	 * Calculate the amount of space required to store a state, given the specified conditions.
	 * @param newNBytesPerStateID - The number of bytes per state ID
	 * @param newTransitionCapacity - The transition capacity
	 * @param newLabelLength - The maximum label length
	 **/
	private long calculateNumberOfBytesPerState(long newNBytesPerStateID, int newTransitionCapacity, int newLabelLength) {
		return
			1 // To hold up to 8 boolean values (such as 'Marked' and 'Exists' status)
			+ (long) newLabelLength // The state's labels
			+ (long) newTransitionCapacity * (long) (Event.N_BYTES_OF_ID + newNBytesPerStateID); // All of the state's transitions
	}

	/**
	 * Resets nBytesPerStateID and stateCapacity as appropriate.
	 **/
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

		/** MUTATOR METHODS **/  

	/**
	 * Adds a transition based on the specified IDs (which means that the states and event must already exist).
	 * NOTE: This method could be made more efficient since the entire state is written to file instead of only
	 * writing the new transition to file.
	 * @param startingStateID - The ID of the state where the transition originates from
	 * @param eventID - The ID of the event that triggers the transition
	 * @param targetStateID - The ID of the state where the transition leads to
	 **/
	public boolean addTransition(long startingStateID, int eventID, long targetStateID) {

		// Create starting state from ID
		State startingState = getState(startingStateID);

		// Increase the maximum allowed transitions per state
		if (startingState.getNumberOfTransitions() == transitionCapacity) {

			// If we cannot increase the capacity, return false (NOTE: This will likely never happen)
			if (transitionCapacity == MAX_TRANSITION_CAPACITY) {
				System.out.println("ERROR: Could not add transition to file (reached maximum transition capacity).");
				return false;
			}

			// Update body file
			recreateBodyFile(
					stateCapacity,
					transitionCapacity + 1,
					labelLength,
					nBytesPerStateID,
					calculateNumberOfBytesPerState(nBytesPerStateID, transitionCapacity + 1, labelLength)
				);

			// Update header file
			writeHeaderFile();

		}

		// Add transition and update the file
		Event event = getEvent(eventID);
		startingState.addTransition(new Transition(event, targetStateID));
		if (!startingState.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity)) {
			System.out.println("ERROR: Could not add transition to file.");
			return false;
		}
		activeEvents.add(event);

		return true;

	}

	/**
	 *	Add the specified state to the automaton with an empty transition list
	 *	@param 	label - The "name" of the new state
	 *	@param 	marked - Whether or not the states is marked
	 *	@param 	isInitialState - Whether or not this is the initial state
	 *	@return the ID of the added state (0 indicates the addition was unsuccessful)
	 **/
	public long addState(String label, boolean marked, boolean isInitialState) {
		return addState(label, marked, new ArrayList<Transition>(), isInitialState);
	}

	/**
	 *	Add the specified state to the automaton
	 *	@param 	label - The "name" of the new state
	 *	@param 	marked - Whether or not the states is marked
	 *	@param 	transitions - The list of transitions
	 *	@param 	isInitialState - Whether or not this is the initial state
	 *	@return the ID of the added state (0 indicates the addition was unsuccessful)
	 **/
	public long addState(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState) {

		// Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs)
		if (nStates == MAX_STATE_CAPACITY) {
			System.out.println("ERROR: Could not write state to file.");
			return 0;
		}

		// Increase the maximum allowed characters per state label
		if (label.length() > labelLength) {

			// If we cannot increase the capacity, indicate a failure
			if (label.length() > MAX_LABEL_LENGTH) {
				System.out.println("ERROR: Could not write state to file.");
				return 0;
			}

			// Re-create binary file
			recreateBodyFile(
					stateCapacity,
					transitionCapacity,
					label.length(),
					nBytesPerStateID,
					calculateNumberOfBytesPerState(nBytesPerStateID, transitionCapacity, label.length())
				);

		}

		// Increase the maximum allowed transitions per state
		if (transitions.size() > transitionCapacity) {

			// If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
			if (transitions.size() > MAX_TRANSITION_CAPACITY) {
				System.out.println("ERROR: Could not write state to file.");
				return 0;
			}

			// Re-create binary file
			recreateBodyFile(
					stateCapacity,
					transitions.size(),
					labelLength,
					nBytesPerStateID,
					calculateNumberOfBytesPerState(nBytesPerStateID, transitions.size(), labelLength)
				);

		}

		// Check to see if we need to re-write the entire binary file
		if (nStates == stateCapacity) {

			// Re-create binary file
			recreateBodyFile(
					((stateCapacity + 1) << 8) - 1,
					transitionCapacity,
					labelLength,
					nBytesPerStateID + 1,
					calculateNumberOfBytesPerState(nBytesPerStateID + 1, transitionCapacity, labelLength)
				);

		}

		long id = ++nStates;

		// Write new state to file
		State state = new State(label, id, marked, transitions);
		if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity)) {
			System.out.println("ERROR: Could not write state to file.");
			return 0;
		}

		// Change initial state
		if (isInitialState)
			initialState = id;

		// Update header file
		writeHeaderFile();

		return id;
	}

	/**
	 *	Add the specified state to the automaton. NOTE: This method assumes that no state already exists with the specified id.
	 *  The method renumberStates() must be called some time after using this method has been called since it can create empty
	 *	spots in the .bdy file where states don't actually exist (this happens during automata operations such as intersection).
	 *	@param 	label - The "name" of the new state
	 *	@param 	marked - Whether or not the states is marked
	 *	@param 	transitions - The list of transitions
	 *	@param 	isInitialState - Whether or not this is the initial state
	 *	@param 	id - The index where the state should be added at
	 *	@return whether or not the addition was successful (returns false if a state already existed there)
	 **/
	public boolean addStateAt(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState, long id) {

		// Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs)
		if (nStates == MAX_STATE_CAPACITY) {
			System.out.println("ERROR: Could not write state to file.");
			return false;
		}

		// Increase the maximum allowed characters per state label
		if (label.length() > labelLength) {

			// If we cannot increase the capacity, indicate a failure
			if (label.length() > MAX_LABEL_LENGTH) {
				System.out.println("ERROR: Could not write state to file.");
				return false;
			}

			recreateBodyFile(
					stateCapacity,
					transitionCapacity,
					label.length(),
					nBytesPerStateID,
					calculateNumberOfBytesPerState(nBytesPerStateID, transitionCapacity, label.length())
				);

		}

		// Increase the maximum allowed transitions per state
		if (transitions.size() > transitionCapacity) {

			// If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
			if (transitions.size() > MAX_TRANSITION_CAPACITY) {
				System.out.println("ERROR: Could not write state to file.");
				return false;
			}

			recreateBodyFile(
					stateCapacity,
					transitions.size(),
					labelLength,
					nBytesPerStateID,
					calculateNumberOfBytesPerState(nBytesPerStateID, transitions.size(), labelLength)
				);

		}

		// Check to see if we need to re-write the entire binary file
		if (id > stateCapacity) {

			// Determine how much stateCapacity and nBytesPerStateID need to be increased by
			long newStateCapacity = stateCapacity;
			int newNBytesPerStateID = nBytesPerStateID;
			while (newStateCapacity < id) {
				newStateCapacity = ((newStateCapacity + 1) << 8) - 1;
				newNBytesPerStateID++;
			}

			// Re-create binary file
			recreateBodyFile(
					newStateCapacity,
					transitionCapacity,
					labelLength,
					newNBytesPerStateID,
					calculateNumberOfBytesPerState(newNBytesPerStateID, transitionCapacity, labelLength)
				);

		}

		// Write new state to file
		State state = new State(label, id, marked, transitions);
		if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity)) {
			System.out.println("ERROR: Could not write state to file.");
			return false;
		}

		nStates++;

		// Change initial state
		if (isInitialState)
			initialState = id;

		// Update header file
		writeHeaderFile();

		return true;
	}

	/**
	 *	Add the specified event to the set (events with identical labels and different properties are currently considered unique)
	 *	@param label - The "name" of the new event
	 *	@param observable - Whether or not the event is observable
	 *	@param controllable - Whether or not the event is controllable
	 *	@return the ID of the added event (0 indicates the addition was unsuccessful, which means the set did not change in size)
	 **/
	public int addEvent(String label, boolean observable, boolean controllable) {

		// Ensure that no other event already exists with this label (this is necessary because of the strange comparison criteria in Event.compareTo())
		for (Event e : events)
			if (e.getLabel().equals(label))
				return 0; 

		// Keep track of the original 
		long originalSize = events.size();

		// Create and add the event
		Event event = new Event(label, events.size() + 1, observable, controllable);
		events.add(event);

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

		/** ACCESSOR METHODS **/

	/**
	 * Check to see if a state exists.
	 * NOTE: 	This is a light-weight method which can be used instead of calling "getState(id) != null").
	 * 			It does not load all of the state information, but only checks the first byte to see if it exists or not.
	 * @param id - The unique identifier corresponding to the state we are looking for
	 **/
	public boolean stateExists(long id) {
		return State.stateExists(this, bodyRAFile, id);
	}

    /**
     *	Given the ID number of a state, get the state information
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return State.readFromFile(this, bodyRAFile, id);
    }

    /**
     *	Given the ID number of an event, get the event information
	 *	@param id - The unique identifier corresponding to the requested event
	 *	@return state - the requested event (or null if it does not exist)
     **/
    public Event getEvent(int id) {

    	for (Event e : events)
    		if (e.getID() == id)
    			return e;

    	return null;

    }

    /**
     *	Return the set of all events (in order by ID).
	 *	@return the set of all events
     **/
    public Set<Event> getEvents() {
    	return events;
    }

    /**
     *	Return the set of all active events.
	 *	@return the set of all active events
     **/
    public Set<Event> getActiveEvents() {
    	return activeEvents;
    }

    /**
     *	Return the set of all controllable events.
	 *	@return the set of all controllable events
     **/
    public Set<Event> getControllableEvents() {
    	return controllableEvents;
    }

    /**
     *	Return the set of all observable events.
	 *	@return the set of all observable events
     **/
    public Set<Event> getObservableEvents() {
    	return observableEvents;
    }

    /**
     * Get the number of states that are currently in this automaton.
     * @return number of states
     **/
    public long getNumberOfStates() {
    	return nStates;
    }

    /**
     * Get the number of states that can be held in this automaton.
     * @return current state capacity
     **/
    public long getStateCapacity() {
    	return stateCapacity;
    }

    /**
     * Get the number of transitions that can be attached to each state.
     * @return current transition capacity
     **/
    public int getTransitionCapacity() {
    	return transitionCapacity;
    }

    /**
     * Get the number of characters that can be used for a state's label.
     * @return current maximum label length
     **/
    public int getLabelLength() {
    	return labelLength;
    }

    /**
     * Get the amount of space needed to store a state ID.
     * @return number of bytes per state ID
     **/
    public int getSizeOfStateID() {
    	return nBytesPerStateID;
    }

    /**
     * Get the amount of space needed to store a state.
     * @return number of bytes per state
     **/
    public long getSizeOfState() {
    	return nBytesPerState;
    }

    /**
     * Get the ID of the state where the automaton begins (the entry point).
     * @return ID of the initial state
     **/
    public long getInitialStateID() {
    	return initialState;
    }

}