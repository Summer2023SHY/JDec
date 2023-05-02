package com.github.automaton.automata;

/* TABLE OF CONTENTS:
 *  -Class Constants
 *  -Class Variables
 *  -Instance Variables
 *  -Automaton Type Enum
 *  -Constructors
 *  -Automata Operations
 *  -Automata Operations Helper Methods
 *  -Image Generation
 *  -GUI Input Code Generation
 *  -Working with Files
 *  -Miscellaneous
 *  -Overridden Method
 *  -Mutator Methods
 *  -Accessor Methods
 */

import java.util.*;
import java.io.*;
import java.math.*;

import com.github.automaton.automata.util.ByteManipulator;
import com.github.automaton.io.*;

import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import guru.nidi.graphviz.parse.*;

/**
 * Class that is able to fully represent an automaton. The usage of .hdr and .bdy files
 * gives the potential to work with very large automata, since the entire automaton does not need
 * to be stored in memory.
 *
 * @implNote The static method {@code clearTemporaryFiles()} should be run on a regular basis when using this class (typically
 *           done when launching your GUI or after running a test routine).
 *
 * @author Micah Stairs
 **/
public class Automaton implements AutoCloseable {

    /* PUBLIC CLASS CONSTANTS */

  /** The number of events that an automaton can hold by default. */
  public static final int DEFAULT_EVENT_CAPACITY = 255;

  /** The maximum number of events that an automaton can hold. */
  public static final int MAX_EVENT_CAPACITY = Integer.MAX_VALUE;

  /** The number of states that an automaton can hold by default. */
  public static final long DEFAULT_STATE_CAPACITY = 255;

  /** The maximum number of states that an automaton can hold. */
  public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;

  /** The number of transitions that each state in an automaton can hold by default. */
  public static final int DEFAULT_TRANSITION_CAPACITY = 1;

  /** The maximum number of transitions that each state in an automaton can hold. */
  public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;

  /** The number of characters that each state label in an automaton can hold by default. */
  public static final int DEFAULT_LABEL_LENGTH = 1;

  /**
   * The maximum number of characters that each state label in an automaton can hold.
   * @implNote This value was originally 100, but was increased drastically in order to accommodate long
   *           state vectors that are formed in the crush.
   **/
  public static final int MAX_LABEL_LENGTH = 100000;

  /** The default number of controllers in an automaton. */
  public static final int DEFAULT_NUMBER_OF_CONTROLLERS = 1;

  /** The maximum number of controllers in an automaton. */
  public static final int MAX_NUMBER_OF_CONTROLLERS = 10;

  /**
   * The label used to indicate a dump state.
   * @implNote A {@link com.github.automaton.gui.JDec} user cannot mess up the complement operation by adding a fake dump state, since spaces
   *           are not considered part of a valid state label.
   **/
  public static final String DUMP_STATE_LABEL = "Dump State";

    /* INSTANCE VARIABLES */

  // Events
  protected List<Event> events = new ArrayList<Event>();
  protected Map<String, Event> eventsMap = new HashMap<String, Event>();

  // Special transitions
  private List<TransitionData> badTransitions;

  // Basic properties of the automaton
  protected Type type;
  protected long nStates      = 0;
  protected long initialState = 0;
  protected int nControllers;
  
  // Variables which determine how large the .bdy file will become
  protected int eventCapacity;
  protected long stateCapacity;
  protected int transitionCapacity;
  protected int labelLength;

  // Initialized based on the above capacities
  protected int nBytesPerEventID;
  protected int nBytesPerStateID;
  protected long nBytesPerState;

  // File variables
  protected final HeaderAccessFile haf;
  protected final BodyAccessFile baf;
  protected boolean headerFileNeedsToBeWritten;

  // GUI input
  protected StringBuilder eventInputBuilder;
  protected StringBuilder stateInputBuilder;
  protected StringBuilder transitionInputBuilder;

    /* AUTOMATON TYPE ENUM */

  /** Enum constant that represents the type of the {@link Automaton}
   * 
   * @author Micah Stairs
   */
  public static enum Type {

    /** The basic automaton */
    AUTOMATON((byte) 0, Automaton.class),

    /** The U-Structure */
    U_STRUCTURE((byte) 1, UStructure.class),

    /** The pruned U-Structure */
    PRUNED_U_STRUCTURE((byte) 2, PrunedUStructure.class),

    /** A pruned U-Structure after being 'crushed' with respect to a given controller */
    CRUSH((byte) 3, Crush.class);

    // Private variables
    private final byte numericValue;
    private final Class<? extends Automaton> classType;

    /**
     * Construct a Type enum object.
     * @param numericValue  The numeric value associated with this enum value (used in {@code .hdr} file)
     * @param classType     The associated class
     **/
    Type(byte numericValue, Class<? extends Automaton> classType) {
      this.numericValue = numericValue;
      this.classType    = classType;
    }

    /**
     * Get the numeric value associated with this enumeration value.
     * @return  The numeric value
     **/
    public byte getNumericValue() {
      return numericValue;
    }

    /**
     * Given a numeric value, get the associated automaton type.
     * @param value The numeric value
     * @return      The automaton type (or {@code null}, if it could not be found)
     **/
    public static Type getType(byte value) {

      for (Type type : Type.values())
        if (type.numericValue == value)
          return type;

      return null;

    }

    /**
     * Given a class, get the associated enumeration value.
     * @param classType The class
     * @return          The automaton type (or {@code null}, if it could not be found)
     **/
    public static Type getType(Class<?> classType) {

      for (Type type : Type.values())
        if (type.classType == classType)
          return type;

      return null;

    }

    /**
     * Given a header file of an automaton, get the associated enumeration value.
     * @param file  The header file of the automaton
     * @return      The automaton type (or {@code null}, if it could not be found)
     **/
    public static Type getType(File file) {

      try (RandomAccessFile raf = (new RandomAccessFile(file, "r"))) {
      
        return Automaton.Type.getType(raf.readByte());
      
      } catch (IOException e) {
        
        e.printStackTrace();
        return null;
      
      }

    }

    /**
     * Returns the string representation of the type of Automaton this enum
     * constant represents.
     * @return the string representation of type
     */
    @Override
    public String toString() {
      switch (this) {
        
        case AUTOMATON:
          return "Automaton";
        
        case U_STRUCTURE:
          return "U-Structure";
        
        case PRUNED_U_STRUCTURE:
          return "Pruned U-Structure";
        
        case CRUSH:
          return "Crush";
      
      }

      return null;

    }

  }

    /* CONSTRUCTORS */

  /**
   * Default constructor: create empty automaton with default capacity using temporary files.
   **/
  public Automaton() {
    this(
      null,
      null,
      DEFAULT_NUMBER_OF_CONTROLLERS
    );
  }

  /**
   * Implicit constructor: create a new automaton with a specified number of controllers in the given files.
   * @param headerFile    The file where the header should be stored
   * @param bodyFile      The file where the body should be stored
   * @param nControllers  The number of controllers that this automaton has
   **/
  public Automaton(File headerFile, File bodyFile, int nControllers) {
    this(
      headerFile,
      bodyFile,
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      nControllers,
      true
    );
  }

  /**
   * Implicit constructor: load automaton from file or create a new automaton.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   * @param clearFiles  Whether or not the header and body files should be wiped before use
   **/
  public Automaton(File headerFile, File bodyFile, boolean clearFiles) {
    this(
      headerFile,
      bodyFile,
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      DEFAULT_NUMBER_OF_CONTROLLERS,
      clearFiles
    );

  }

  /**
   * Implicit constructor: create automaton with specified initial capacities using temporary files.
   * @implNote Choosing larger values increases the amount of space needed to store the binary file.
   * Choosing smaller values increases the frequency that you need to re-write the entire binary file in order to expand it
   * @param eventCapacity        The initial event capacity (increases by a factor of 256 when it is exceeded)
   *                             (NOTE: the initial event capacity may be higher than the value you give it, since it has to be in the form 256^x - 1)
   * @param stateCapacity        The initial state capacity (increases by a factor of 256 when it is exceeded)
   *                             (NOTE: the initial state capacity may be higher than the value you give it, since it has to be in the form 256^x - 1)
   * @param transitionCapacity   The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength          The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllers         The number of controllers that the automaton has (1 implies centralized control, >1 implies decentralized control)
   * @param clearFiles           Whether or not the header and body files should be cleared prior to use
   **/
  public Automaton(int eventCapacity, long stateCapacity, int transitionCapacity, int labelLength, int nControllers, boolean clearFiles) {
    this(null, null, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllers, clearFiles);
  }

  /**
   * Main constructor.
   * @param headerFile         The binary file to load the header information of the automaton from (information about events, etc.)
   * @param bodyFile           The binary file to load the body information of the automaton from (states and transitions)
   * @param eventCapacity      The initial event capacity (increases by a factor of 256 when it is exceeded)
   * @param stateCapacity      The initial state capacity (increases by a factor of 256 when it is exceeded)
   * @param transitionCapacity The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength        The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllers       The number of controllers that the automaton has (1 implies centralized control, >1 implies decentralized control)
   * @param clearFiles         Whether or not the header and body files should be cleared prior to use
   **/
  public Automaton(File headerFile, File bodyFile, int eventCapacity, long stateCapacity, int transitionCapacity, int labelLength, int nControllers, boolean clearFiles) {

      /* Initialize lists, so that there are no NullPointerExceptions */

    initializeLists();

      /* Store variables and open files */
    try {
      this.haf = new HeaderAccessFile(Objects.requireNonNullElse(headerFile, IOUtility.getTemporaryFile()));
      this.baf = new BodyAccessFile(Objects.requireNonNullElse(bodyFile, IOUtility.getTemporaryFile()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

      /* These variables will be overridden if we are loading information from file */

    this.eventCapacity      = eventCapacity;
    this.stateCapacity      = stateCapacity;
    this.transitionCapacity = transitionCapacity;
    this.labelLength        = labelLength;
    this.nControllers       = nControllers;

      /* Clear files */

    if (clearFiles)
      clearFiles();
    
      /* Load data from header */

    readHeaderFile();

      /* Finish setting up */

    initializeVariables();
    nBytesPerState = calculateNumberOfBytesPerState(nBytesPerEventID, nBytesPerStateID, this.transitionCapacity, this.labelLength);
    type = Type.getType(this.getClass());
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Used to initialize all lists in order to prevent the possibility of NullPointerExceptions.
   * @apiNote This method must be called at the beginning of the constuctor of Automaton. This method is intended to
   * be overridden by sub-classes, however, any sub-classes of Automaton do not need to explicitly call it.
   **/
  protected void initializeLists() {

    badTransitions = new ArrayList<TransitionData>();
  
  }

    /** AUTOMATA OPERATIONS **/

  /**
   * Create a new copy of this automaton that has all unreachable states and transitions removed.
   * @param newHeaderFile   The header file where the accesible automaton should be stored
   * @param newBodyFile     The body file where the accesible automaton should be stored
   * @return                The accessible automaton
   **/
  public Automaton accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new Automaton(newHeaderFile, newBodyFile, nControllers));
  }

  /**
   * A helper method used to generate the accessible portion of this automaton.
   * @param automaton The generic automaton object
   * @return          The same automaton that was passed into the method, now containing the accessible part of this automaton
   **/
  protected final <T extends Automaton> T accessibleHelper(T automaton) {

      /* Setup */

    // Add events
    automaton.addAllEvents(events);

    // If there is no initial state, return null, so that the GUI knows to alert the user
    if (initialState == 0)
      return null;

    // Add the initial state to the stack
    Stack<Long> stack = new Stack<Long>(); 
    stack.push(initialState);

      /* Build automaton from the accessible part of this automaton */

    // Add states and transition
    while (stack.size() > 0) {

      // Get next ID
      long id = stack.pop();

      // This state has already been created in the new automaton, so it does not need to be created again
      if (automaton.stateExists(id))
        continue;

      // Get state and transitions
      State state = getState(id);
      List<Transition> transitions = state.getTransitions();

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
        stack.push(t.getTargetStateID());

        // Add transition to the new automaton
        automaton.addTransition(id, t.getEvent().getID(), t.getTargetStateID());

      }
    
    }

      /* Add special transitions if they still appear in the accessible part */

    copyOverSpecialTransitions(automaton);

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return accessible automaton */

    return automaton;
  }

  /**
   * Create a new copy of this automaton that has all states removed which are unable to reach a marked state.
   * @implNote This method should be overridden by subclasses, using the {@link #coaccessibleHelper()} method.
   * @param newHeaderFile  The header file where the new automaton should be stored
   * @param newBodyFile    The body file where the new automaton should be stored
   * @return               The co-accessible automaton
   **/
  public Automaton coaccessible(File newHeaderFile, File newBodyFile) {
    return coaccessibleHelper(new Automaton(newHeaderFile, newBodyFile, nControllers), invert());
  }

  /**
   * A helper method used to generate the co-accessible portion of this automaton.
   * @param automaton The generic automaton object
   * @return          The same automaton that was passed into the method, now containing the co-accessible
   *                  part of this automaton
   **/
  protected final <T extends Automaton> T coaccessibleHelper(T automaton, T invertedAutomaton) {

      /* Build co-accessible automaton by seeing which states are accessible from the marked states in the inverted automaton */

    // Add events
    automaton.addAllEvents(events);

    // Add all marked states to the stack (NOTE: This may have complications if there are more than Integer.MAX_VALUE marked states)
    Stack<Long> stack = new Stack<Long>();
    for (long s = 1; s <= nStates; s++) {

      State state = invertedAutomaton.getStateExcludingTransitions(s);

      if (state.isMarked())
        stack.push(s);

    }

    // Add all reachable states to the co-accessible automaton
    while (stack.size() > 0) {

      long s = stack.pop();

      // Skip this state is it has already been taken care of
      if (automaton.stateExists(s))
        continue;

      State state = getState(s);
      State stateWithInvertedTransitions = invertedAutomaton.getState(s);

      // Add this state (and its transitions) to the co-accessible automaton
      automaton.addStateAt(state.getLabel(), state.isMarked(), new ArrayList<Transition>(), s == initialState, s);

      // Add all directly reachable states from this one to the stack
      for (Transition t : stateWithInvertedTransitions.getTransitions()) {

        // Add transition if both states already exist in the co-accessible automaton
        if (automaton.stateExists(t.getTargetStateID()))
          automaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), s);

        // Otherwise add this to the stack since it is not yet in the co-accessible automaton
        else
          stack.push(t.getTargetStateID());

      }

      // Required to catch transitions if we didn't add them the first time around (since this state was not yet in the co-accessible automaton)
      for (Transition t : state.getTransitions()) {

        // Add transition if both states already exist in the co-accessible automaton
        if (automaton.stateExists(t.getTargetStateID()))
          // We don't want to add self-loops twice
          if (s != t.getTargetStateID())
            automaton.addTransition(s, t.getEvent().getID(), t.getTargetStateID());

      }
  
    }

      /* Add special transitions if they still appear in the accessible part */

    copyOverSpecialTransitions(automaton);

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return co-accessible automaton */

    return automaton;
  }

  /**
   * Create a new copy of this automaton that has the marking status of all states toggled, and that has an added
   * 'dead' or 'dump' state where all undefined transitions lead.
   * @implNote This method should be overridden by subclasses, using the {@link #complementHelper()} method.
   * @param newHeaderFile             The header file where the new automaton should be stored
   * @param newBodyFile               The body file where the new automaton should be stored
   * @return                          The complement automaton
   * @throws OperationFailedException When there already exists a dump state, indicating that this
   *                                  operation has already been performed on this automaton
   **/
  public Automaton complement(File newHeaderFile, File newBodyFile) throws OperationFailedException {

    Automaton automaton = new Automaton(
      newHeaderFile,
      newBodyFile,
      eventCapacity,
      stateCapacity,
      events.size(), // This is the new number of transitions that will be required for each state
      labelLength,
      nControllers,
      true
    );

    return complementHelper(automaton);
  }

  /**
   * A helper method used to generate complement of this automaton.
   * @param automaton                 The generic automaton object
   * @return                          The same automaton that was passed into the method, now containing
   *                                  the complement of this automaton
   * @throws OperationFailedException When there already exists a dump state, indicating that this
   *                                  operation has already been performed on this automaton
   **/
  protected final <T extends Automaton> T complementHelper(T automaton) throws OperationFailedException {

      /* Add events */
    
    automaton.addAllEvents(events);

      /* Build complement of this automaton */

    long dumpStateID = nStates + 1;
    boolean needToAddDumpState = false;

    // Add each state to the new automaton
    for (long s = 1; s <= nStates; s++) {

      State state = getState(s);

      // Indicate that a dump state already exists, and the complement shouldn't be taken again
      if (state.getLabel().equals(DUMP_STATE_LABEL))
        throw new OperationFailedException();

      long id = automaton.addState(state.getLabel(), !state.isMarked(), s == initialState);

      // Add transitions for each event (even if they were previously undefined, these transitions will lead to the dump state)
      for (Event e : events) {

        boolean foundMatch = false;

        // Search through each transition for the event
        for (Transition t : state.getTransitions())
          if (t.getEvent().equals(e)) {
            automaton.addTransition(id, e.getID(), t.getTargetStateID());
            foundMatch = true;
          }

        // Add new transition leading to dump state if this event if undefined at this state
        if (!foundMatch) {
          automaton.addTransition(id, e.getID(), dumpStateID);
          needToAddDumpState = true;
        }

      }

    }

      /* Create dump state if it needs to be made */

    if (needToAddDumpState) {
    
      long id = automaton.addState(DUMP_STATE_LABEL, false, false);

      if (id != dumpStateID)
        System.err.println("ERROR: Dump state ID did not match expected ID.");
    
    }

      /* Add special transitions */

    copyOverSpecialTransitions(automaton);

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return complement automaton */

    return automaton;
  }

  /**
   * Creates a new copy of this automaton that is trim (both accessible and co-accessible).
   * @implNote I am taking the accessible part of the automaton before the co-accessible part of the automaton
   * because the {@link #accessible()} method has less overhead than the {@link #coaccessible(File, File)} method.
   * @param newHeaderFile  The header file where the new automaton should be stored
   * @param newBodyFile    The body file where the new automaton should be stored
   * @return               The trim automaton, or {@code null} if there was no initial state specified
   **/
  public Automaton trim(File newHeaderFile, File newBodyFile) {
    return accessible(null, null).coaccessible(newHeaderFile, newBodyFile);
  }

  /**
   * Create a new version of this automaton which has all of the transitions going the opposite direction.
   * @implNote An inverted automaton is needed when you want to efficiently determine which transitions lead to a particular state.
   * @implNote This is just a shallow copy of the automaton (no special transition data is retained), which makes it slightly more efficient.
   * @implNote This method should be overridden by subclasses, using the {@link #invertHelper(Automaton)} method.
   * @return  The inverted automaton
   * 
   * @see #invertHelper(Automaton)
   **/
  public Automaton invert() {
    return invertHelper(new Automaton(eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllers, true));
  }

  /**
   * A helper method used to generate the inverse of this automaton.
   * @implNote The states in the inverted automaton should still have the same IDs.
   * @implNote This automaton is lightweight, meaning it has no special transition information, only the
   *       states, events, and transitions.
   * @param automaton The generic automaton object
   * @return          The same automaton that was passed into the method, now containing the inverse of this automaton
   **/
  protected final <T extends Automaton> T invertHelper(T automaton) {

      /* Create a new automaton that has each of the transitions going the opposite direction */

    // Add events
    automaton.addAllEvents(events);

    // Add states
    for (long s = 1; s <= getNumberOfStates(); s++) {
      State state = getStateExcludingTransitions(s);
      automaton.addState(state.getLabel(), state.isMarked(), s == initialState);
    }

    // Add transitions
    for (long s = 1; s <= getNumberOfStates(); s++)
      for (Transition t : getState(s).getTransitions())
        automaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), s);

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

    return automaton;

  }

  /**
   * Generate the intersection of the two specified automata.
   * @param first                           The first automaton
   * @param second                          The second automaton
   * @param newHeaderFile                   The header file where the new automaton should be stored
   * @param newBodyFile                     The body file where the new automaton should be stored
   * @return                                The intersection
   * @throws IncompatibleAutomataException  If the number of controllers do not match, or the automata have incompatible events
   **/
  public static Automaton intersection(Automaton first, Automaton second, File newHeaderFile, File newBodyFile) throws IncompatibleAutomataException {

      /* Error checking */

    if (first.getNumberOfControllers() != second.getNumberOfControllers())
      throw new IncompatibleAutomataException();

      /* Setup */

    Automaton automaton = new Automaton(newHeaderFile, newBodyFile, first.getNumberOfControllers());

    // These two stacks should always have the same size
    Stack<Long> stack1 = new Stack<Long>(); 
    Stack<Long> stack2 = new Stack<Long>();

    // Add the initial states to the stack
    stack1.push(first.getInitialStateID());
    stack2.push(second.getInitialStateID());

      /* Build product */

    // Create event set (intersection of both event sets)
    for (Event e1 : first.getEvents())
      for (Event e2 : second.getEvents())
        if (e1.equals(e2)) {

          // Ensure that these automata are compatible (meaning that no events have the same name, but with different properties)
          if (!Arrays.equals(e1.isObservable(), e2.isObservable()) || !Arrays.equals(e1.isControllable(), e2.isControllable()))
            throw new IncompatibleAutomataException();

          automaton.addEvent(e1.getLabel(), e1.isObservable(), e1.isControllable());

        }

    // Add states and transition
    while (stack1.size() > 0) {

      // Get next IDs
      long id1 = stack1.pop();
      long id2 = stack2.pop();

      // Error checking
      if (id1 == 0 || id2 == 0) {
        System.err.println("ERROR: Bad state ID.");
        continue;
      }

      // Create combined ID
      long newStateID = combineTwoIDs(id1, first, id2, second);

      // This state has already been created, so it does not need to be created again
      if (automaton.stateExists(newStateID))
        continue;

      // Get states and transitions
      State state1 = first.getState(id1);
      State state2 = second.getState(id2);
      List<Transition> transitions1 = state1.getTransitions();
      List<Transition> transitions2 = state2.getTransitions();

      // Add new state
      automaton.addStateAt(state1.getLabel() + "_" + state2.getLabel(),
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
            long targetID = combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
            int eventID   = automaton.addTransition(newStateID, t1.getEvent().getLabel(), targetID);

            // Mark as bad transition if both of them are bad
            if (first.isBadTransition(id1, t1.getEvent().getID(), t1.getTargetStateID()) && second.isBadTransition(id2, t2.getEvent().getID(), t2.getTargetStateID()))
              automaton.markTransitionAsBad(newStateID, eventID, targetID);

          }

    }

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return produced automaton */

    return automaton;
  }

  /**
   * Generate the union of the two specified automata.
   * @param first                           The first automaton
   * @param second                          The second automaton
   * @param newHeaderFile                   The header file where the new automaton should be stored
   * @param newBodyFile                     The body file where the new automaton should be stored
   * @return                                The union
   * @throws IncompatibleAutomataException  If the number of controllers do not match, or the automata have incompatible events
   **/
  public static Automaton union(Automaton first, Automaton second, File newHeaderFile, File newBodyFile) throws IncompatibleAutomataException {

      /* Error checking */

    if (first.getNumberOfControllers() != second.getNumberOfControllers())
      throw new IncompatibleAutomataException();

      /* Setup */

    Automaton automaton = new Automaton(newHeaderFile, newBodyFile, first.getNumberOfControllers());

    // These two stacks should always have the same size
    Stack<Long> stack1 = new Stack<Long>(); 
    Stack<Long> stack2 = new Stack<Long>();

    // Add the initial states to the stack
    stack1.push(first.getInitialStateID());
    stack2.push(second.getInitialStateID());

      /* Build automata by parallel composition */

    // Create two sets containing each automata's private events
    List<Event> privateEvents1 = new ArrayList<Event>();
    privateEvents1.addAll(first.getEvents());
    for (Event e : second.getEvents())
      privateEvents1.remove(e);
    List<Event> privateEvents2 = new ArrayList<Event>();
    privateEvents2.addAll(second.getEvents());
    for (Event e : first.getEvents())
      privateEvents2.remove(e);

    // Create event set (union of both event sets)
    automaton.addAllEvents(first.getEvents());
    automaton.addEventsWithErrorChecking(second.getEvents());

    // Add states and transition
    while (stack1.size() > 0) {

      // Get next IDs
      long id1 = stack1.pop();
      long id2 = stack2.pop();

      // Error checking
      if (id1 == 0 || id2 == 0) {
        System.err.println("ERROR: Bad state ID.");
        continue;
      }

      // Create combined ID
      long newStateID = combineTwoIDs(id1, first, id2, second);

      // This state has already been created, so it does not need to be created again
      if (automaton.stateExists(newStateID))
        continue;

      // Get states and transitions
      State state1 = first.getState(id1);
      State state2 = second.getState(id2);
      List<Transition> transitions1 = state1.getTransitions();
      List<Transition> transitions2 = state2.getTransitions();

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
            long targetID = combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
            int eventID   = automaton.addTransition(newStateID, t1.getEvent().getLabel(), targetID);

            // Mark as bad transition if either of them are bad
            if (first.isBadTransition(id1, t1.getEvent().getID(), t1.getTargetStateID()) || second.isBadTransition(id2, t2.getEvent().getID(), t2.getTargetStateID()))
              automaton.markTransitionAsBad(newStateID, eventID, targetID);

          }

      // Take care of the first automaton's private events
      for (Transition t : transitions1)
        if (privateEvents1.contains(t.getEvent())) {
        
          // Add the pair of states to the stack
          stack1.add(t.getTargetStateID());
          stack2.add(id2);

          // Add transition to the new automaton
          long targetID = combineTwoIDs(t.getTargetStateID(), first, id2, second);
          int eventID = automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

          // Mark as bad transition if it is bad
          if (first.isBadTransition(id1, t.getEvent().getID(), t.getTargetStateID()))
            automaton.markTransitionAsBad(newStateID, eventID, targetID);

        }

      // Take care of the second automaton's private events
      for (Transition t : transitions2)
        if (privateEvents2.contains(t.getEvent())) {
        
          // Add the pair of states to the stack
          stack1.add(id1);
          stack2.add(t.getTargetStateID());

          // Add transition to the new automaton
          long targetID = combineTwoIDs(id1, first, t.getTargetStateID(), second);
          int eventID = automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

          // Mark as bad transition if it is bad
          if (second.isBadTransition(id2, t.getEvent().getID(), t.getTargetStateID()))
            automaton.markTransitionAsBad(newStateID, eventID, targetID);

        }

    }

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return generated automaton */

    return automaton;

  }

  /**
   * Apply the synchronized composition algorithm to an automaton to produce the U-Structure.
   * @param newHeaderFile  The header file where the new automaton should be stored
   * @param newBodyFile    The body file where the new automaton should be stored
   * @return               The U-Structure (or {@code null} if there was no starting state or something else
   *                       went wrong)
   **/
  public UStructure synchronizedComposition(File newHeaderFile, File newBodyFile) {

      /* Setup */

    Stack<Long> stack = new Stack<Long>();
    HashSet<Long> valuesInStack = new HashSet<Long>();
    UStructure uStructure = new UStructure(newHeaderFile, newBodyFile, nControllers, true);

      /* Add initial state to the stack */

    { // The only reason this is inside a scope is so that variable names could be re-used more cleanly
      List<Long> listOfInitialIDs = new ArrayList<Long>();
      String combinedStateLabel = "";
      State startingState = getState(initialState);

      // Error checking
      if (startingState == null) {
        System.err.println("ERROR: No starting state.");
        return null;
      }

      // Create list of initial IDs and build the label
      for (int i = 0; i <= nControllers; i++) {
        listOfInitialIDs.add(initialState);
        combinedStateLabel += "_" + startingState.getLabel();
      }

      long combinedID = combineIDs(listOfInitialIDs, nStates);
      stack.push(combinedID);
      valuesInStack.add(combinedID);

      uStructure.addStateAt(combinedStateLabel.substring(1), false, new ArrayList<Transition>(), true, combinedID);

    }

      /* Continue until the stack is empty */

    while (stack.size() > 0) {
      
      long combinedID = stack.pop();
      valuesInStack.remove(combinedID);

      // Get list of IDs and states
      List<Long> listOfIDs = separateIDs(combinedID, nStates);
      List<State> listOfStates = new ArrayList<State>();
      for (long id : listOfIDs)
        listOfStates.add(getState(id));

      // For each transition in the system automaton
      outer: for (Transition t1 : listOfStates.get(0).getTransitions()) {

        Event e = t1.getEvent();

        List<Long> listOfTargetIDs = new ArrayList<Long>();
        listOfTargetIDs.add(t1.getTargetStateID());

        String combinedEventLabel = e.getLabel();
        String combinedStateLabel = getStateExcludingTransitions(t1.getTargetStateID()).getLabel();

        // If this is the system has a bad transition, then it is an unconditional violation by default until we've found a controller that prevents it
        boolean isBadTransition = isBadTransition(listOfStates.get(0).getID(), e.getID(), t1.getTargetStateID());
        boolean isUnconditionalViolation = isBadTransition;

        // It is not a disablement decision by default, until we find a controller that can disable it
        // NOTE: The system must also have a bad transition in order for it to be a disablement decision
        boolean isDisablementDecision = false;
        boolean[] disablementControllers = new boolean[nControllers];

        // A conditional violation can only occur when an event is controllable by at least 2 controllers, and the system must have a good transition
        int counter = 0;
        for (int i = 0; i < nControllers; i++)
          if (e.isControllable()[i])
            counter++;
        boolean isConditionalViolation = (counter >= 2 && !isBadTransition);

        // Determine observable and controllable properties for this event vector
        boolean[] observable   = new boolean[nControllers];
        boolean[] controllable = new boolean[nControllers];

        // For each controller
        for (int i = 0; i < nControllers; i++) {

          // Observable events by this controller
          if (e.isObservable()[i]) {

            observable[i] = true;

            // If the event is observable, but not possible at this current time, then we can skip this altogether
            long targetID = 0;
            String label = null;
            for (Transition t2 : listOfStates.get(i + 1).getTransitions())
              if (t2.getEvent().equals(e)) {
                targetID = t2.getTargetStateID();
                label = getStateExcludingTransitions(t2.getTargetStateID()).getLabel();
              }
            if (targetID == 0)
              continue outer;

            combinedEventLabel += "," + e.getLabel();
            combinedStateLabel += "_" + label;
            listOfTargetIDs.add(targetID);

            if (e.isControllable()[i]) {

              controllable[i] = true;

              TransitionData data = new TransitionData(listOfStates.get(i + 1).getID(), e.getID(), targetID);

              // Check to see if this controller can prevent an unconditional violation
              if (isUnconditionalViolation && badTransitions.contains(data))
                isUnconditionalViolation = false;

              // Check to see if this controller can prevent a conditional violation
              if (isConditionalViolation && !badTransitions.contains(data))
                isConditionalViolation = false;

              // Check to see if this controller causes a disablement decision
              if (isBadTransition && badTransitions.contains(data)) {
                isDisablementDecision = true;
                disablementControllers[i] = true;
              }

            }

          // Unobservable events by this controller
          } else {
            combinedEventLabel += ",*";
            combinedStateLabel += "_" + listOfStates.get(i + 1).getLabel();
            listOfTargetIDs.add(listOfIDs.get(i + 1));
          }

        } // for i

        combinedEventLabel = "<" + combinedEventLabel + ">";

        long combinedTargetID = combineIDs(listOfTargetIDs, nStates);

        // Add event
        uStructure.addEventIfNonExisting(combinedEventLabel, observable, controllable);

        // Add state if it doesn't already exist
        if (!uStructure.stateExists(combinedTargetID)) {

          // Add state
          if (!uStructure.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
            System.err.println("ERROR: Failed to add state. Synchronized composition aborted.");
            return null;
          }
          
          // Only add the ID if it's not already waiting to be processed
          if (!valuesInStack.contains(combinedTargetID)) {
              stack.push(combinedTargetID);
              valuesInStack.add(combinedTargetID);
          } else
            System.out.println("DEBUG: Prevented adding of state since it was already in the stack (NOTE: Does this ever get printed to the console? Intuitively it should, but I have never seen it before.).");
        }

        // Add transition
        int eventID = uStructure.addTransition(combinedID, combinedEventLabel, combinedTargetID);
        if (isUnconditionalViolation)
          uStructure.addUnconditionalViolation(combinedID, eventID, combinedTargetID);
        if (isConditionalViolation)
          uStructure.addConditionalViolation(combinedID, eventID, combinedTargetID);
        if (isDisablementDecision)
          uStructure.addDisablementDecision(combinedID, eventID, combinedTargetID, disablementControllers);

      } // for

      // For each unobservable transition in the each of the controllers of the automaton
      outer: for (int i = 0; i < nControllers; i++) {

        for (Transition t : listOfStates.get(i + 1).getTransitions()) {
          if (!t.getEvent().isObservable()[i]) {

            List<Long> listOfTargetIDs = new ArrayList<Long>();
            String combinedEventLabel = "";
            String combinedStateLabel = "";

            for (int j = 0; j <= nControllers; j++) {

              // The current controller
              if (j == i + 1) {
                listOfTargetIDs.add(t.getTargetStateID());
                combinedEventLabel += "," + t.getEvent().getLabel();
                combinedStateLabel += "_" + getStateExcludingTransitions(t.getTargetStateID()).getLabel();
              } else {
                listOfTargetIDs.add(listOfIDs.get(j));
                combinedEventLabel += ",*";
                combinedStateLabel += "_" + listOfStates.get(j).getLabel(); 
              }

            }

            combinedEventLabel = "<" + combinedEventLabel.substring(1) + ">";
            combinedStateLabel = combinedStateLabel.substring(1);
            long combinedTargetID = combineIDs(listOfTargetIDs, nStates);

            // Add event
            boolean[] observable = new boolean[nControllers];
            boolean[] controllable = new boolean[nControllers];
            controllable[i] = t.getEvent().isControllable()[i];
            uStructure.addEventIfNonExisting(combinedEventLabel, observable, controllable);

            // Add state if it doesn't already exist
            if (!uStructure.stateExists(combinedTargetID)) {

              // Add state
              if (!uStructure.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
                System.err.println("ERROR: Failed to add state. Synchronized composition aborted.");
                return null;
              }
            
              // Only add the ID if it's not already waiting to be processed
              if (!valuesInStack.contains(combinedTargetID)) {
                stack.push(combinedTargetID);
                valuesInStack.add(combinedTargetID);
              } else
                System.out.println("DEBUG: Prevented adding of state since it was already in the stack.");

            }

            // Add transition
            if (uStructure.addTransition(combinedID, combinedEventLabel, combinedTargetID) == 0)
              System.err.println("ERROR: Failed to add transition.");

          }
        }

      } // for


    } // while

      /* Re-number states (by removing empty ones) */

    uStructure.renumberStates();

      /* Ensure that the header file has been written to disk */

    uStructure.writeHeaderFile();

      /* Return produced U-Structure */

    return uStructure;

  }

  /**
   * Test to see if this system is observable.
   * @implNote This is an expensive test.
   * @return  Whether or not this system is observable
   **/
  public boolean testObservability() {

    Automaton centralizedAutomaton = new Automaton(
      null,
      null,
      getEvents().size(),
      getNumberOfStates(),
      getTransitionCapacity(),
      getLabelLength(),
      1,
      true
    );

    // Copy over modified events to the centralized automaton
    for (Event e : getEvents()) {

      // Find the union of the observability and controllability properties for each event
      boolean[] observableUnion = new boolean[1];
      boolean[] controllableUnion = new boolean[1];
      for (boolean b : e.isObservable())
        if (b)
          observableUnion[0] = true;
      for (boolean b : e.isControllable())
        if (b)
          controllableUnion[0] = true;

      // Add the event to the centralized automaton
      centralizedAutomaton.addEvent(e.getLabel(), observableUnion, controllableUnion);

    }

    // Copy over the states and transitions to the centralized automaton
    // NOTE: I attempted to do this by simply copying the .bdy file, because it would be more efficient,
    //       however there are many other things that need to be considered (capacities, initial state,
    //       re-opening the RandomAccessFile object for the body file, etc.)
    for (long s = 1; s <= getNumberOfStates(); s++) {
      State state = getState(s);

      // Add state
      centralizedAutomaton.addState(state.getLabel(), state.isMarked(), s == getInitialStateID());

      // Add transitions
      for (Transition t : state.getTransitions())
        centralizedAutomaton.addTransition(s, t.getEvent().getID(), t.getTargetStateID());

    }

    // Copy over all special transitions to the centralized automaton
    copyOverSpecialTransitions(centralizedAutomaton);

    // Take the U-Structure
    UStructure uStructure = centralizedAutomaton.synchronizedComposition(null, null);

    // The presence of violations indicate that the system is not observable
    return !uStructure.hasViolations();

  }

  /**
   * Test to see if this system is controllable.
   * @implNote This is a cheap test.
   * @return  Whether or not this system is controllable
   **/
  public boolean testControllability() {

    outer: for (TransitionData data : badTransitions) {
      
      Event event = getEvent(data.eventID);
      
      // Ensure that the event is controllable      
      for (boolean b : event.isControllable())
        if (b)
          continue outer;

      // Otherwise this system is not controllable
      return false;

    }

    return true;

  }

  /**
   * Generate the twin plant by combining this automaton w.r.t. G_{Sigma*}.
   * @implNote The technique used here is similar to how the complement works. This would not work
   *       in all cases, but G_{Sigma*} is a special case.
   * @param newHeaderFile The header file where the new automaton should be stored
   * @param newBodyFile   The body file where the new automaton should be stored
   * @return              The twin plant
   **/
  public final Automaton generateTwinPlant(File newHeaderFile, File newBodyFile) {

    Automaton automaton = new Automaton(
      newHeaderFile,
      newBodyFile,
      getEventCapacity(),
      getStateCapacity(),
      getTransitionCapacity(),
      getLabelLength(),
      getNumberOfControllers(),
      true
    );

      /* Add events */
    
    automaton.addAllEvents(getEvents());

      /* Build twin plant */

    long dumpStateID           = getNumberOfStates() + 1;
    boolean needToAddDumpState = false;

    // Add each state to the new automaton
    for (long s = 1; s <= getNumberOfStates(); s++) {

      State state = getState(s);

      long id = automaton.addState(state.getLabel(), !state.isMarked(), s == initialState);

      // Try to add transitions for each event
      for (Event e : events) {

        boolean foundMatch = false;

        // Search through each transition for the event
        for (Transition t : state.getTransitions())
          if (t.getEvent().equals(e)) {
            automaton.addTransition(id, e.getID(), t.getTargetStateID());
            foundMatch = true;
          }

        // Check to see if this event is controllable by at least one controller
        boolean controllable = false;
        for (boolean b : e.isControllable())
          if (b) {
            controllable = true;
            break;
          }

        // Add new transition leading to dump state if this event if undefined at this state and is controllable
        if (!foundMatch && controllable) {
          automaton.addTransition(id, e.getID(), dumpStateID);
          automaton.markTransitionAsBad(id, e.getID(), dumpStateID);
          needToAddDumpState = true;
        }

      }

    }

      /* Create dump state if it needs to be made */

    if (needToAddDumpState) {
    
      long id = automaton.addState(DUMP_STATE_LABEL, false, false);

      if (id != dumpStateID)
        System.err.println("ERROR: Dump state ID did not match expected ID.");
    
    }

      /* Add special transitions */

    copyOverSpecialTransitions(automaton);

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return generated automaton */

    return automaton;

  }


  /**
    * Generate the twin plant by combining this automaton w.r.t. G_{Sigma*}.
    * @implNote The technique used here is similar to how the complement works. This would not work
    *       in all cases, but G_{Sigma*} is a special case.
    * @param newHeaderFile The header file where the new automaton should be stored
    * @param newBodyFile   The body file where the new automaton should be stored
    * @return              The twin plant
    **/
  public final Automaton generateTwinPlant2(File newHeaderFile, File newBodyFile) {

    Automaton automaton = new Automaton(
      newHeaderFile,
      newBodyFile,
      getEventCapacity(),
      getStateCapacity(),
      getTransitionCapacity(),
      getLabelLength(),
      getNumberOfControllers(),
      true
    );

      /* Add events */
    
    automaton.addAllEvents(getEvents());

      /* Build twin plant */

    long dumpStateID           = getNumberOfStates() + 1;
    boolean needToAddDumpState = false;

    List<Event> activeEvents = getActiveEvents();

    // Add each state to the new automaton
    for (long s = 1; s <= getNumberOfStates(); s++) {

      State state = getState(s);

      long id = automaton.addState(state.getLabel(), !state.isMarked(), s == initialState);

      // Try to add transitions for each event
      for (Event e : events) {

        boolean foundMatch = false;

        // Search through each transition for the event
        for (Transition t : state.getTransitions())
          if (t.getEvent().equals(e)) {
            automaton.addTransition(id, e.getID(), t.getTargetStateID());
            foundMatch = true;
          }

        // Check to see if this event is controllable by at least one controller
        boolean controllable = false;
        for (boolean b : e.isControllable())
          if (b) {
            controllable = true;
            break;
          }

        // Add new transition leading to dump state if this event if undefined at this state and is controllable and active
        if (!foundMatch && controllable && activeEvents.contains(e)) {
          automaton.addTransition(id, e.getID(), dumpStateID);
          automaton.markTransitionAsBad(id, e.getID(), dumpStateID);
          needToAddDumpState = true;
        }

      }

    }

      /* Create dump state if it needs to be made */

    if (needToAddDumpState) {
    
      long id = automaton.addState(DUMP_STATE_LABEL, false, false);

      if (id != dumpStateID)
        System.err.println("ERROR: Dump state ID did not match expected ID.");
    
    }

      /* Add special transitions */

    copyOverSpecialTransitions(automaton);

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return generated automaton */

    return automaton;

  }

    /* AUTOMATA OPERATION HELPER METHODS */

  /**
   * Helper method to copy over all special transition data from this automaton to another.
   * @implNote The data is only copied over if both of the states involved in the transition actually exist.
   * @apiNote This method is intended to be overridden.
   * @param <T> type of automaton
   * @param automaton The automaton which is receiving the special transitions
   **/
  protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

    for (TransitionData data : badTransitions)
      if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
        automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);

  }

  /**
   * Looks for blank spots in the {@code .bdy} file (which indicates that no state exists there),
   * and re-numbers all of the states accordingly. This must be done after operations such as intersection or union.
   */
  /* To make this method more efficient we could make the buffer larger. */
  @SuppressWarnings("deprecation")
  protected final void renumberStates() {

    try {

      byte[] buffer = new byte[nBytesPerStateID];

        /* Create a file containing the mappings (where the new IDs can be indexed using the old IDs) */

      File mappingFile = IOUtility.getTemporaryFile();
      RandomAccessFile mappingRAFile = new RandomAccessFile(mappingFile, "rw");

      long newID = 1;
      for (long s = 1; s <= getStateCapacity(); s++)
        if (stateExists(s)) {
          
          Arrays.fill(buffer, (byte) 0);
          mappingRAFile.seek(nBytesPerStateID * s);
          ByteManipulator.writeLongAsBytes(buffer, 0, newID++, nBytesPerStateID);
          mappingRAFile.write(buffer);

        }

        /* Create new .bdy file with renumbered states */

      File newBodyFile = IOUtility.getTemporaryFile();
      RandomAccessFile newBodyRAFile = new RandomAccessFile(newBodyFile, "rw");

      for (long s = 1; s <= getStateCapacity(); s++) {

        State state = null;

        if ((state = getState(s)) != null) {
          
          // Get new ID of state
          Arrays.fill(buffer, (byte) 0);
          mappingRAFile.seek(nBytesPerStateID * s);
          mappingRAFile.read(buffer);
          long newStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

          // Update initial state ID (if applicable)
          // NOTE: This works since 'newStateID' will be <= 's'
          if (initialState == s)
            initialState = newStateID;

          // Update ID of state
          state.setID(newStateID);

          // Update IDs of the target state of each
          for (Transition t : state.getTransitions()) {

            // Get new ID of state
            Arrays.fill(buffer, (byte) 0);
            mappingRAFile.seek(nBytesPerStateID * t.getTargetStateID());
            mappingRAFile.read(buffer);
            long newTargetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

            if (newTargetStateID == 0) {
              System.out.println("ERROR: Target state does not exist, deleting transitions in an attempt to recover.");
              state.getTransitions().clear();
              break;
            } else 
              t.setTargetStateID(newTargetStateID);
          }

          // Write the updated state to the new file
          if (!StateIO.writeToFile(state, newBodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID))
            System.err.println("ERROR: Could not write state to file.");

        }

      }

        /* Update the special transitions in the header file */

      renumberStatesInAllTransitionData(mappingRAFile);

        /* Remove old body file and mappings file */

      try {
        newBodyRAFile.close();
        mappingRAFile.close();
        baf.copyFrom(newBodyFile);
      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

      /* Update header file (since we renumbered the information in the special transitions) */

    headerFileNeedsToBeWritten = true;

  }


  /**
   * Renumber the states in all applicable special transition data.
   * @apiNote This method is designed to be overridden when subclassing, in order to renumber the states in
   *          all applicable special transition data for this automaton type.
   * @param mappingRAFile The file containing the mapping information (old state IDs to new state IDs)
   * @throws IOException  If there are any problems read from or writing to file
   **/
  protected void renumberStatesInAllTransitionData(RandomAccessFile mappingRAFile) throws IOException {

    renumberStatesInTransitionData(mappingRAFile, badTransitions);

  }

  /**
   * Helper method to renumber states in the specified list of special transitions.
   * @param mappingRAFile The binary file containing the state ID mappings
   * @param list          The list of special transition data
   * @throws IOException  If there was problems reading from file
   **/
  protected final void renumberStatesInTransitionData(RandomAccessFile mappingRAFile, List<? extends TransitionData> list) throws IOException {

    byte[] buffer = new byte[nBytesPerStateID];

    for (TransitionData data : list) {

      // Update initialStateID
      mappingRAFile.seek(nBytesPerStateID * data.initialStateID);
      mappingRAFile.read(buffer);
      data.initialStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

      // Update targetStateID
      mappingRAFile.seek(nBytesPerStateID * data.targetStateID);
      mappingRAFile.read(buffer);
      data.targetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

    }

  }

  /**
   * Given two state IDs (the order matters) and their respective automatons, create a unique combined ID.
   * @implNote The reasoning behind this formula is analogous to the following: if you have a table with N rows and M columns,
   * every cell is guaranteed to have a different combination of row and column indexes.
   * @param id1     The state ID from the first automaton
   * @param first   The first automaton
   * @param id2     The state ID from the second automaton
   * @param second  The second automaton
   * @return        The combined ID
   * @throws ArithmeticException if the ID combination result overflows a {@code long}
   **/ 
  private static long combineTwoIDs(long id1, Automaton first, long id2, Automaton second) {

    return Math.addExact((id2 - 1) * first.getNumberOfStates(), id1);

  }

  /**
   * Given a list of IDs and a maximum possible ID, create a unique combined ID.
   * @implNote The order of the list matters. This method does not sort the list internally.
   * @param list  The list of IDs
   * @param maxID The largest possible value that could appear in the list (usually {@link #nStates})
   * @return      The unique combined ID
   * @throws ArithmeticException if the ID combination result overflows a {@code long}
   **/
  public static long combineIDs(List<Long> list, long maxID) {
    
    long combinedID = 0;

    for (long id : list) {
      
      combinedID *= maxID + 1;
      combinedID += id;

      // Check for overflow
      if (combinedID < 0)
        throw new ArithmeticException("Overflow in Automaton.combineIDs() method. Consider using Automaton.combineBigIDs() method instead.");

    }

    return combinedID;

  }

  /**
   * Given a list of IDs and the largest possible value that could appear in the list, create a unique
   * combined ID using a BigInteger.
   * @implNote The order of the list matters. This method does not sort the list interally.
   * @param list  The list of IDs
   * @param maxID The largest possible value that could appear in the list (usually nStates)
   * @return      The unique combined ID
   **/
  public static BigInteger combineBigIDs(List<Long> list, long maxID) {
    
    BigInteger bigMaxID = BigInteger.valueOf(maxID);
    BigInteger maxIDPlusOne = bigMaxID.add(BigInteger.ONE);

    BigInteger combinedID = BigInteger.ZERO;

    for (long id : list)
      combinedID = combinedID.multiply(maxIDPlusOne).add(BigInteger.valueOf(id));

    return combinedID;

  }

  /**
   * Given a combined ID, obtain the list of original IDs by reversing the process.
   * @param combinedID  The combined ID
   * @param maxID       The largest possible value to be used as an ID
   * @return            The original list of IDs
   **/
  public static List<Long> separateIDs(long combinedID, long maxID) {

    List<Long> list = new ArrayList<Long>();

    while (combinedID > 0) {

      list.add(0, combinedID % (maxID + 1));
      combinedID /= (maxID + 1);

    }

    return list;

  }

  /**
   * Check to see if this automaton accepts the specified counter-example.
   * @param sequences The list of sequences of event labels which represent the counter-example
   * @return          {@code -1} if the autmaton accepts the counter-example, or the number of steps it took to reject the counter-example
   **/
  public int acceptsCounterExample(List<List<String>> sequences) {

    int nSteps = 0;

    // Ensure that all sequences can be matched
    for (List<String> sequence : sequences) {

      State currentState = getState(getInitialStateID());

      outer: for (String label : sequence) {
        
        nSteps++;
        
        // Check all transitions for a match
        for (Transition t : currentState.getTransitions()) {
          if (t.getEvent().getLabel().equals(label)) {
            currentState = getState(t.getTargetStateID());
            continue outer;
          }
        }

        // If we got this far, then this automaton does not accept the counter-example
        return nSteps;

      }

    }

    return -1;

  }

    /* IMAGE GENERATION */

  /**
   * Output this automaton in a format that is readable by GraphViz, then export as requested.
   * @param outputFileName              The location to put the generated output
   * @return                            Whether or not the output was successfully generated
   * @throws MissingOrCorruptBodyFileException If any of the states are unable to be read from the body file
   * @throws IOException If I/O error occurs
   **/
  public boolean generateImage(String outputFileName) throws MissingOrCorruptBodyFileException, IOException {

      /* Setup */

    StringBuilder str = new StringBuilder();
    str.append("digraph Image {");
    str.append("bgcolor=\"transparent\";overlap=scale;");
    str.append("node [shape=circle, style=bold, constraint=false];");

    try {

        /* Mark special transitions */

      HashMap<String, String> additionalEdgeProperties = new HashMap<String, String>();
      addAdditionalEdgeProperties(additionalEdgeProperties);
      
        /* Draw all states and their transitions */

      for (long s = 1; s <= nStates; s++) {

        // Get state from file
        State state = getState(s);
        String stateLabel = formatStateLabel(state);

        // Draw state
        if (state.isMarked())
          str.append(String.format("\"_%s\" [peripheries=2,label=\"%s\"];", stateLabel, stateLabel));
        else
          str.append(String.format("\"_%s\" [peripheries=1,label=\"%s\"];", stateLabel, stateLabel));
        
        // Find and draw all of the special transitions 
        ArrayList<Transition> transitionsToSkip = new ArrayList<Transition>();
        for (Transition t : state.getTransitions()) {

          State targetState = getStateExcludingTransitions(t.getTargetStateID());

          // Check to see if this transition has additional properties (meaning it's a special transition)
          String key = "" + stateLabel + " " + t.getEvent().getID() + " " + formatStateLabel(targetState);
          String properties = additionalEdgeProperties.get(key);

          if (properties != null) {

            transitionsToSkip.add(t);

            String edge = "\"_" + stateLabel + "\" -> \"_" + formatStateLabel(targetState) + "\"";
            str.append(edge);
            str.append(" [label=\"" + t.getEvent().getLabel() + "\"");
            str.append(properties);
            str.append("];");
          
          }
        }

        // Draw all of the remaining (normal) transitions
        for (Transition t1 : state.getTransitions()) {

          // Skip it if this was already taken care of (grouped into another transition going to the same target state)
          if (transitionsToSkip.contains(t1))
            continue;

          // Start building the label
          String label = t1.getEvent().getLabel();
          transitionsToSkip.add(t1);

          // Look for all transitions that can be grouped with this one
          for (Transition t2 : state.getTransitions()) {

            // Skip it if this was already taken care of (grouped into another transition going to
            // the same target state)
            if (transitionsToSkip.contains(t2))
              continue;

            // Check to see if both transitions lead to the same event
            if (t1.getTargetStateID() == t2.getTargetStateID()) {
              label += "," + t2.getEvent().getLabel();
              transitionsToSkip.add(t2);
            }

          }

          // Add transition
          String edge = "\"_" + stateLabel + "\" -> \"_" + formatStateLabel(getStateExcludingTransitions(t1.getTargetStateID())) + "\"";
          str.append(edge);
          str.append(" [label=\"" + label + "\"]");

        }

      }

        /* Add arrow towards initial state */

      if (initialState > 0) {
        str.append("node [shape=plaintext];");
        str.append("\" \"-> \"_" + formatStateLabel(getStateExcludingTransitions(initialState)) + "\" [color=blue];");
      }

      str.append("}");

    } catch (NullPointerException e) {
      throw new MissingOrCorruptBodyFileException(e);
    }

      /* Generate image */

    MutableGraph g = new Parser().read(str.toString());
    Graphviz.fromGraph(g).render(Format.SVG_STANDALONE).toFile(new File(outputFileName + ".svg"));
    Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(outputFileName + ".png"));

    return true;

  }

  /**
   * This helper method is used to get a state's label, breaking vectors into multiple lines.
   * @param state The state in which the label is being taken from
   * @return      The formatted state label
   **/
  private String formatStateLabel(State state) {

    String label = state.getLabel();
    LabelVector labelVector = new LabelVector(label);
    int size = labelVector.getSize();

    if (size == -1)
      return label;

    StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < size; i++)
      stringBuilder.append(labelVector.getLabelAtIndex(i) + "\\n");

    return stringBuilder.toString();

  }

  /**
   * Add any additional edge properties applicable to this automaton type, which is used in the DOT output.
   * EXAMPLE: This is used to color potential communications blue.
   * @param map The mapping from edges to additional properties
   **/
  protected void addAdditionalEdgeProperties(Map<String, String> map) {

    for (TransitionData data : badTransitions)
      appendValueToMap(map, createKey(data), ",style=dotted");
    
  }

  /**
   * Helper method used to create a key for the additional edge properties map.
   * @param data  The relevant transition data
   * @return      A string used to identify this particular transition
   **/
  protected String createKey(TransitionData data) {
    return "" + formatStateLabel(getState(data.initialStateID)) + " "
              + data.eventID + " "
              + formatStateLabel(getStateExcludingTransitions(data.targetStateID));
  }

  /**
   * Helper method used to append a value to the pre-existing value of a particular key in a map.
   * If the key was not previously in the map, then the value is simply added.
   * @param map   The relevant map
   * @param key   The key which is mapped to a value that is being appending to
   * @param value The value to be appended
   **/
  protected void appendValueToMap(Map<String, String> map, String key, String value) {
    if (map.containsKey(key))
      map.put(key, map.get(key) + value);
    else
      map.put(key, value); 
  }

    /* GUI INPUT CODE GENERATION */

  /**
   * Generates all GUI input code (which is useful when loading automaton from file in the GUI).
   * @apiNote Further calls to {@link #getEventInput()}, {@link #getStateInput()}, and/or {@link #getTransitionInput()}
   *          are needed to actually get the generated input code.
   **/
  public void generateInputForGUI() {

    generateEventInputForGUI();
    generateStateAndTransitionInputForGUI();

  }

  /**
   * Generates the GUI input code for the events.
   **/
  private void generateEventInputForGUI() {

    eventInputBuilder = new StringBuilder();

    int counter = 0;

    for (Event e : events) {

      // Label
      eventInputBuilder.append(e.getLabel());

      // Observability properties
      eventInputBuilder.append(",");
      for (int i = 0; i < nControllers; i++)
        eventInputBuilder.append((e.isObservable()[i] ? "T" : "F"));

      // Controllability properties
      eventInputBuilder.append(",");
      for (int i = 0; i < nControllers; i++)
        eventInputBuilder.append((e.isControllable()[i] ? "T" : "F"));

      // End of line character
      if (++counter < events.size())
        eventInputBuilder.append("\n");

    }

  }

  /**
   * Generates the GUI input code for the events.
   **/
  private void generateStateAndTransitionInputForGUI() {

    stateInputBuilder = new StringBuilder();
    transitionInputBuilder = new StringBuilder();
    
    boolean firstTransitionInStringBuilder = true;

    for (long s = 1; s <= nStates; s++) {

      State state = getState(s);

      if (state == null) {
        System.err.println("ERROR: State could not be loaded.");
        continue;
      }

      // Place '@' before label if this is the initial state
      if (s == initialState)
        stateInputBuilder.append("@");

      // Append label and properties
      stateInputBuilder.append(state.getLabel());
      if (type == Type.AUTOMATON)
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
            + "," + getStateExcludingTransitions(t.getTargetStateID()).getLabel()
          );

          /* Append special transition information */

        TransitionData transitionData = new TransitionData(s, t.getEvent().getID(), t.getTargetStateID());
        String specialTransitionInfo = getInputCodeForSpecialTransitions(transitionData);
        
        if (!specialTransitionInfo.equals(""))
          transitionInputBuilder.append(":" + specialTransitionInfo.substring(1));

      }

    }

  }

  /**
   * Get the GUI input code correlating with the special transition data for the specified transition.
   * @param data transition data
   * @apiNote This method is intended to be overridden when subclassing **/
  protected String getInputCodeForSpecialTransitions(TransitionData data) {

    return (badTransitions.contains(data)) ? ",BAD" : "";

  }

  /**
   * Get the event GUI input code.
   * @apiNote Must call {@link #generateInputForGUI()} prior to use.
   * @return  GUI input code in the form of a string
   **/
  public final String getEventInput() {

    if (eventInputBuilder == null)
      return null;

    return eventInputBuilder.toString();

  }

  /**
   * Get the state GUI input code.
   * @apiNote Must call {@link #generateInputForGUI()} prior to use.
   * @return  GUI input code in the form of a string
   **/
  public final String getStateInput() {

    if (stateInputBuilder == null)
      return null;

    return stateInputBuilder.toString();

  }

  /**
   * Get the transition GUI input code.
   * @apiNote Must call {@link #generateInputForGUI()} prior to use.
   * @return  GUI input code in the form of a String
   **/
  public final String getTransitionInput() {

    if (transitionInputBuilder == null)
      return null;

    return transitionInputBuilder.toString();

  }

    /* WORKING WITH FILES */

  /**
   * Duplicate this automaton, storing them in temporary files.
   * @apiNote This method is intended to be overridden.
   **/
  public Automaton duplicate() {
    return duplicate(IOUtility.getTemporaryFile(), IOUtility.getTemporaryFile());
  }

  /**
   * Duplicate this automaton and store it in a different set of files.
   * @apiNote This method is intended to be overridden.
   * @param newHeaderFile The new header file where the automaton is being copied to (cannot be null)
   * @param newBodyFile   The new body file where the automaton is being copied to (cannot be null)
   * @return              The duplicated automaton
   **/
  public Automaton duplicate(File newHeaderFile, File newBodyFile) {

    if (!duplicateHelper(newHeaderFile, newBodyFile))
      return null;

    return new Automaton(newHeaderFile, newBodyFile, false);

  }

  /**
   * A helper method used to duplicate this automaton, simply by making a copy of the .bdy and .hdr files (which is clearly the most efficient approach).
   * @param newHeaderFile The new header file where the automaton is being copied to
   * @param newBodyFile   The new body file where the automaton is being copied to
   * @return              Whether or not the duplication was successful
   **/
  protected final boolean duplicateHelper(File newHeaderFile, File newBodyFile) {

    // Ensure that the header file is up-to-date
    writeHeaderFile();

    // Copy the header and body files
    try {
    
      if (haf.exists())
        haf.copyTo(newHeaderFile);
      if (baf.exists())
        baf.copyTo(newBodyFile);
    
    // Handle errors
    } catch (IOException e) {

      e.printStackTrace();
      return false;

    }

    return true;

  }

  /**
   * Files need to be closed on the Windows operating system because there are problems trying to delete files if they are in use.
   * @implNote Do not attempt to use this automaton instance again afterwards.
   **/
  public void closeFiles() {

      try {

        writeHeaderFile();

        haf.close();
        baf.close();

      } catch (IOException e) {
        e.printStackTrace();
      }

  }

  @Override
  public void close() {
    closeFiles();
  } 

  /**
   * Delete the current header and body files.
   **/
  private void clearFiles() {

    try {

      if (!haf.clearFile())
        System.err.println("ERROR: Could not delete header file.");
      
      if (!baf.clearFile())
        System.err.println("ERROR: Could not delete body file.");

    } catch (SecurityException e) {
      e.printStackTrace();
    }

  }

  /**
   * Write all of the header information to file.
   **/
  public final void writeHeaderFile() {

    // Do not write the header file unless we need to
    if (!headerFileNeedsToBeWritten)
      return;

      /* Write the header of the .hdr file */
    
    byte[] buffer = new byte[HeaderAccessFile.HEADER_SIZE];

    ByteManipulator.writeLongAsBytes(buffer, 0,  type.getNumericValue(), 1);
    ByteManipulator.writeLongAsBytes(buffer, 1,  nStates, 8);
    ByteManipulator.writeLongAsBytes(buffer, 9,  eventCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 13, stateCapacity, 8);
    ByteManipulator.writeLongAsBytes(buffer, 21, transitionCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 25, labelLength, 4);
    ByteManipulator.writeLongAsBytes(buffer, 29, initialState, 8);
    ByteManipulator.writeLongAsBytes(buffer, 37, nControllers, 4);
    ByteManipulator.writeLongAsBytes(buffer, 41, events.size(), 4);

    try {

      haf.seek(0);
      haf.write(buffer);

        /* Write the events to the .hdr file */

      for (Event e : events) {
      
        // Fill the buffer
        buffer = new byte[ (2 * nControllers) + 4 + e.getLabel().length()];

        // Read event properties (NOTE: If we ever need to condense the space required to hold an event
        // in a file, we can place a property in each bit instead of each byte)
        int index = 0;
        for (int i = 0; i < nControllers; i++) {
          buffer[index]     = (byte) (e.isObservable()[i]   ? 1 : 0);
          buffer[index + 1] = (byte) (e.isControllable()[i] ? 1 : 0);
          index += 2;
        }

        // Write the length of the label
        ByteManipulator.writeLongAsBytes(buffer, index, e.getLabel().length(), 4);
        index += 4;

        // Write each character of the label
        for (int i = 0; i < e.getLabel().length(); i++)
          buffer[index++] = (byte) e.getLabel().charAt(i);

        haf.write(buffer);

      }

        /* This is where the .hdr content corresponding to the relevant automaton type is written */

      writeSpecialTransitionsToHeader();     

        /* Indicate that the header file no longer need to be written */

      headerFileNeedsToBeWritten = false;

        /* Trim the file so that there is no garbage at the end (removing events, for example, shortens the .hdr file) */

      haf.trim();

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * Write all of the special transitions to the header, which is relevant to this particular automaton type.
   * @apiNote This method is intended to be overridden when sub-classing.
   * @throws IOException  If there were any problems writing to file
   **/
  protected void writeSpecialTransitionsToHeader() throws IOException {

      /* Write a number which indicates how many special transitions are in the file */

    byte[] buffer = new byte[4];
    ByteManipulator.writeLongAsBytes(buffer, 0, badTransitions.size(), 4);
    haf.write(buffer);

      /* Write special transitions to the .hdr file */

    writeTransitionDataToHeader(badTransitions);

  }

  /**
   * A helper method to write a list of special transitions to the header file.
   * @param list          The list of transition data
   * @throws IOException  If there were any problems writing to file
   **/
  protected void writeTransitionDataToHeader(List<TransitionData> list) throws IOException {

      /* Setup */

    byte[] buffer = new byte[list.size() * 20];
    int index = 0;

      /* Write each piece of transition data into the buffer */

    for (TransitionData data : list) {

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

    }

      /* Write the buffer to file */

    haf.write(buffer);

  }

  /**
   * Read all of the header information from file.
   **/
  protected final void readHeaderFile() {

    try {

        /* Do not try to load an empty file */

      if (haf.isEmpty())
        return;

        /* Go to the proper position and read in the bytes */
      haf.seek(0);
      byte[] buffer = haf.readHeaderBytes(HeaderAccessFile.HEADER_SIZE);

        /* Calculate the values stored in these bytes */

      type = Type.getType((byte) ByteManipulator.readBytesAsLong(buffer, 0,  1));
      nStates            =       ByteManipulator.readBytesAsLong(buffer, 1,  8);
      eventCapacity      =       ByteManipulator.readBytesAsInt(buffer, 9,  4);
      stateCapacity      =       ByteManipulator.readBytesAsLong(buffer, 13, 8);
      transitionCapacity =       ByteManipulator.readBytesAsInt(buffer, 21, 4);
      labelLength        =       ByteManipulator.readBytesAsInt(buffer, 25, 4);
      initialState       =       ByteManipulator.readBytesAsLong(buffer, 29, 8);
      nControllers       =       ByteManipulator.readBytesAsInt(buffer, 37, 4);
      int nEvents        =       ByteManipulator.readBytesAsInt(buffer, 41, 4);

      // None of the folowing things can exist if there are no events
      if (nEvents == 0)
        return;

        /* Read in the events */

      for (int e = 1; e <= nEvents; e++) {

        // Read properties
        buffer = haf.readHeaderBytes(nControllers * 2);
        boolean[] observable = new boolean[nControllers];
        boolean[] controllable = new boolean[nControllers];
        for (int i = 0; i < nControllers; i++) {
          observable[i] = (buffer[2 * i] == 1);
          controllable[i] = (buffer[(2 * i) + 1] == 1);
        }

        // Read the number of characters in the label
        buffer = haf.readHeaderBytes(4);
        int eventLabelLength = ByteManipulator.readBytesAsInt(buffer, 0, 4);

        // Read each character of the label, building an array of characters
        buffer = haf.readHeaderBytes(eventLabelLength);
        char[] arr = new char[eventLabelLength];
        for (int i = 0; i < arr.length; i++)
          arr[i] = (char) buffer[i];

        // Create the event and add it to the list
        addEvent(new String(arr), observable, controllable);

      }

        /* This is where the .hdr content corresponding to the relevant automaton type is read */

      readSpecialTransitionsFromHeader();

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * Read all of the special transitions from the header, which is relevant to this particular automaton type.
   * @apiNote This method is intended to be overridden when sub-classing.
   * @throws IOException  If there were any problems reading from file
   **/
  protected void readSpecialTransitionsFromHeader() throws IOException {

      /* Read the number which indicates how many special transitions are in the file */

    byte[] buffer = haf.readHeaderBytes(4);
    int nBadTransitions = ByteManipulator.readBytesAsInt(buffer, 0, 4);

      /* Read in special transitions from the .hdr file */
    
    if (nBadTransitions > 0)
      readTransitionDataFromHeader(nBadTransitions, badTransitions);

  }

  /**
   * A helper method to read a list of special transitions from the header file.
   * @param nTransitions  The number of transitions that need to be read
   * @param list          The list of transition data
   * @throws IOException  If there was problems reading from file
   **/
  protected void readTransitionDataFromHeader(int nTransitions, List<TransitionData> list) throws IOException {

      /* Read from file */

    byte[] buffer = haf.readHeaderBytes(nTransitions * 20);
    int index = 0;

      /* Add transitions to the list */

    for (int i = 0; i < nTransitions; i++) {
      
      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = ByteManipulator.readBytesAsInt(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      list.add(new TransitionData(initialStateID, eventID, targetStateID));
    
    }

  }

  /**
   * Re-create the body file to accommodate some increase in capacity.
   * @implNote This operation can clearly be expensive for large automata, so we need to try to reduce the number of times this method is called.
   * @param newEventCapacity      The number of events that the automaton will be able to hold
   * @param newStateCapacity      The number of states that the automaton will be able to hold
   * @param newTransitionCapacity The number of transitions that each state will be able to hold
   * @param newLabelLength        The maximum number of characters that each state label will be allowed
   * @param newNBytesPerStateID   The number of bytes that are now required to represent each state's ID
   * @param newNBytesPerEventID   The number of bytes that are now required to represent each event's ID
   **/
  @SuppressWarnings("deprecation")
  private void recreateBodyFile(int newEventCapacity, long newStateCapacity, int newTransitionCapacity, int newLabelLength, int newNBytesPerEventID, int newNBytesPerStateID) {

    long newNBytesPerState = calculateNumberOfBytesPerState(newNBytesPerEventID, newNBytesPerStateID, newTransitionCapacity, newLabelLength);

      /* Setup files */

    File newBodyFile = IOUtility.getTemporaryFile();

    // Ensure that this temporary file does not already exist
    if (newBodyFile.exists())
      if (!newBodyFile.delete())
        System.err.println("ERROR: Could not delete previously existing temporary file.");

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

        // Pad with zeros, which will indicate a non-existent state
        try {
          newBodyRAFile.write(buffer);
        } catch (IOException e) {
          e.printStackTrace();
        }

        counter++;
        
        continue; 
      }

      // Try writing to file
      if (!StateIO.writeToFile(state, newBodyRAFile, newNBytesPerState, newLabelLength, newNBytesPerEventID, newNBytesPerStateID)) {
        System.err.println("ERROR: Could not write copy over state to file. Aborting re-creation of .bdy file.");
        return;
      }

    } // for

      /* Remove old file */

    try {

      newBodyRAFile.close();
      baf.copyFrom(newBodyFile);

    } catch (SecurityException | IOException e) {

      e.printStackTrace();

    }

      /* Update variables */

    eventCapacity      = newEventCapacity;
    stateCapacity      = newStateCapacity;
    transitionCapacity = newTransitionCapacity;
    labelLength        = newLabelLength;
    nBytesPerEventID   = newNBytesPerEventID;
    nBytesPerStateID   = newNBytesPerStateID;
    nBytesPerState     = newNBytesPerState;

  }

    /* MISCELLANEOUS */

  /**
   * Calculate the amount of space required to store a state, given the specified conditions.
   * @param newNBytesPerEventID   The number of bytes per event ID
   * @param newNBytesPerStateID   The number of bytes per state ID
   * @param newTransitionCapacity The transition capacity
   * @param newLabelLength        The maximum label length
   * @return                      The number of bytes needed to store a state
   **/
  private long calculateNumberOfBytesPerState(int newNBytesPerEventID, long newNBytesPerStateID, int newTransitionCapacity, int newLabelLength) {
    return
      1 // To hold up to 8 boolean values (such as 'Marked' and 'Exists' status)
      + (long) newLabelLength // The state's labels
      + (long) newTransitionCapacity * (long) (newNBytesPerEventID + newNBytesPerStateID); // All of the state's transitions
  }

  /**
   * Initialize the variables, ensuring that they all lay within the proper ranges.
   **/
  private void initializeVariables() {

      /* The automaton should have room for at least 1 transition per state (otherwise our automaton will be pretty boring) */

    if (transitionCapacity < 1)
      transitionCapacity = 1;

      /* The requested length of the state labels should not exceed the limit, nor should it be non-positive */

    if (labelLength < 1)
      labelLength = 1;
    if (labelLength > MAX_LABEL_LENGTH)
      labelLength = MAX_LABEL_LENGTH;

      /* The number of controllers should be greater than 0, but it should not exceed the maximum */

    if (nControllers < 1)
      nControllers = 1;
    if (nControllers > MAX_NUMBER_OF_CONTROLLERS)
      nControllers = MAX_NUMBER_OF_CONTROLLERS;

      /* Calculate the amount of space needed to store each state ID */

    // Special case if the state capacity is not positive
    nBytesPerStateID = stateCapacity < 1 ? 1 : 0;

    long temp = stateCapacity;
    
    while (temp > 0) {
      nBytesPerStateID++;
      temp >>= 8;
    }

      /* Calculate the maximum number of states that we can have before we have to allocate more space for each state's ID */

    stateCapacity = 1;

    for (int i = 0; i < nBytesPerStateID; i++)
      stateCapacity <<= 8;

      /* Special case when the user gives a value between 2^56 - 1 and 2^64 (exclusive) */

    if (stateCapacity == 0)
      stateCapacity = MAX_STATE_CAPACITY;
    else
      stateCapacity--;

      /* Cap the state capacity */

    if (stateCapacity > MAX_STATE_CAPACITY)
      stateCapacity = MAX_STATE_CAPACITY;

      /* Calculate the amount of space needed to store each event ID */

    // Special case if the event capacity is not positive
    nBytesPerEventID = eventCapacity < 1 ? 1 : 0;

    temp = eventCapacity;
    
    while (temp > 0) {
      nBytesPerEventID++;
      temp >>= 8;
    }

      /* Calculate the maximum number of events that we can have before we have to allocate more space for each event's ID */

    eventCapacity = 1;

    for (int i = 0; i < nBytesPerEventID; i++)
      eventCapacity <<= 8;

      /* Special case when the user gives a value between 2^24 - 1 and 2^32 (exclusive) */

    if (eventCapacity == 0)
      eventCapacity = MAX_EVENT_CAPACITY;
    else
      eventCapacity--;

      /* Cap the event capacity */

    if (eventCapacity > MAX_EVENT_CAPACITY)
      eventCapacity = MAX_EVENT_CAPACITY;

  }

    /* OVERRIDDEN METHOD */

  @Override public String toString() {
    return getHeaderFile().getName() + "/" + getBodyFile().getName();
  }
    /* MUTATOR METHODS */

  /**
   * Adds a transition based the label of the event (instead the ID).
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventLabel      The label of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   * @return                The ID of the event label (returns 0 if the addition was unsuccessful)
   **/
  public int addTransition(long startingStateID, String eventLabel, long targetStateID) {

    for (Event e : events)
      if (eventLabel.equals(e.getLabel())) {
        if (!addTransition(startingStateID, e.getID(), targetStateID))
          return 0;
        else
          return e.getID();
      }

    return 0;

  }

  /**
   * Adds a transition based on the specified IDs (which means that the states and event must already exist).
   * @implNote This method could be made more efficient since the entire state is written to file instead of only writing the new transition to file.
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventID         The ID of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   * @return                Whether or not the addition was successful
   **/
  public boolean addTransition(long startingStateID, int eventID, long targetStateID) {

      /* Get starting state from ID */

    State startingState  = getState(startingStateID);

    if (startingState == null) {
      System.err.println("ERROR: Could not add transition to file (starting state does not exist).");
      return false;
    }

      /* Increase the maximum allowed transitions per state */

    if (startingState.getNumberOfTransitions() == transitionCapacity) {

      // If we cannot increase the capacity, return false (NOTE: This will likely never happen)
      if (transitionCapacity == MAX_TRANSITION_CAPACITY) {
        System.err.println("ERROR: Could not add transition to file (reached maximum transition capacity).");
        return false;
      }

      // Update body file
      recreateBodyFile(
          eventCapacity,
          stateCapacity,
          transitionCapacity + 1,
          labelLength,
          nBytesPerEventID,
          nBytesPerStateID
        );

      // Update header file
      headerFileNeedsToBeWritten = true;

    }

      /* Increase the number of allowed states */

    if (targetStateID > getStateCapacity()) {

      if (targetStateID > MAX_STATE_CAPACITY) {
        System.err.println("ERROR: Could not add transition (reached maximum state capacity).");
        return false;
      }

      // Determine how much stateCapacity and nBytesPerStateID need to be increased by
      long newStateCapacity = stateCapacity;
      int newNBytesPerStateID = nBytesPerStateID;
      while (newStateCapacity < targetStateID) {
        newStateCapacity = ((newStateCapacity + 1) << 8) - 1;
        newNBytesPerStateID++;
      }

      // Re-create binary file
      recreateBodyFile(
        eventCapacity,
        newStateCapacity,
        transitionCapacity,
        labelLength,
        nBytesPerEventID,
        newNBytesPerStateID
      );

    }

      /* Add transition and update the file */

    Event event = getEvent(eventID);
    startingState.addTransition(new Transition(event, targetStateID));
    if (!StateIO.writeToFile(startingState, baf, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
      System.err.println("ERROR: Could not add transition to file.");
      return false;
    }

    return true;

  }

  /**
   * Removes the specified transition.
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventID         The ID of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   * @return                Whether or not the removal was successful
   **/
  public boolean removeTransition(long startingStateID, int eventID, long targetStateID) {

      /* Get starting state from ID */

    State startingState  = getState(startingStateID);

    if (startingState == null) {
      System.err.println("ERROR: Could not remove transition from file (starting state does not exist).");
      return false;
    }

      /* Remove transition and update the file */

    Event event = getEvent(eventID);
    startingState.removeTransition(new Transition(event, targetStateID));
    if (!StateIO.writeToFile(startingState, baf, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
      System.err.println("ERROR: Could not remove transition from file.");
      return false;
    }

      /* Remove transition from list of special transitions (if it appears anywhere in them) */

    removeTransitionData(new TransitionData(startingStateID, eventID, targetStateID));

    headerFileNeedsToBeWritten = true;

    return true;

  }

  /**
   * Remove any special transition information attached to a particular transition.
   * @apiNote This method is intended to be overridden.
   * @param data  The transition data associated with the special transitions to be removed
   **/
  protected void removeTransitionData(TransitionData data) {

    badTransitions.remove(data);

  }

  /**
   * Add the specified state to the automaton with an empty transition list.
   * @param label           The "name" of the new state
   * @param marked          Whether or not the states is marked
   * @param isInitialState  Whether or not this is the initial state
   * @return                The ID of the added state; or {@code 0} if the addition was unsuccessful
   **/
  public long addState(String label, boolean marked, boolean isInitialState) {
    return addState(label, marked, null, isInitialState);
  }

  /**
   * Add the specified state to the automaton.
   * @param label           The "name" of the new state
   * @param marked          Whether or not the states is marked
   * @param transitions     The list of transitions (if null, then an empty list is made)
   * @param isInitialState  Whether or not this is the initial state
   * @return                The ID of the added state; or {@code 0} if the addition was unsuccessful
   **/
  public long addState(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState) {

    if (transitions == null)
      transitions = new ArrayList<Transition>();

      /* Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs) */

    if (nStates == MAX_STATE_CAPACITY) {
      System.err.println("ERROR: Could not write state to file (reached maximum state capacity).");
      return 0;
    }

      /* Increase the maximum allowed characters per state label */

    if (label.length() > labelLength) {

      // If we cannot increase the capacity, indicate a failure
      if (label.length() > MAX_LABEL_LENGTH) {
        System.err.println("ERROR: Could not write state to file (reached maximum label length).");
        return 0;
      }

      // Re-create binary file
      recreateBodyFile(
        eventCapacity,
        stateCapacity,
        transitionCapacity,
        label.length(),
        nBytesPerEventID,
        nBytesPerStateID
      );

    }

      /* Increase the maximum allowed transitions per state */
    
    if (transitions.size() > transitionCapacity) {

      // If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
      if (transitions.size() > MAX_TRANSITION_CAPACITY) {
        System.err.println("ERROR: Could not write state to file (reached maximum transition capacity).");
        return 0;
      }

      // Re-create binary file
      recreateBodyFile(
        eventCapacity,
        stateCapacity,
        transitions.size(),
        labelLength,
        nBytesPerEventID,
        nBytesPerStateID
      );

    }

      /* Increase the number of allowed states */
    
    if (nStates == stateCapacity) {

      // Re-create binary file
      recreateBodyFile(
        eventCapacity,
        ((stateCapacity + 1) << 8) - 1,
        transitionCapacity,
        labelLength,
        nBytesPerEventID,
        nBytesPerStateID + 1
      );

    }

    long id = ++nStates;

      /* Write new state to file */
    
    State state = new State(label, id, marked, transitions);
    if (!StateIO.writeToFile(state, baf, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
      System.err.println("ERROR: Could not write state to file.");
      return 0;
    }

      /* Change initial state */
    
    if (isInitialState)
      initialState = id;

      /* Update header file */
    
    headerFileNeedsToBeWritten = true;

    return id;
  }

  /**
   * Add the specified state to the automaton.
   * @implNote This method assumes that no state already exists with the specified ID.
   * @implNote The method {@link #renumberStates()} must be called some time after using this method has been called since it can create empty
   * spots in the {@code .bdy} file where states don't actually exist (this happens during automata operations such as intersection).
   * @param label           The "name" of the new state
   * @param marked          Whether or not the states is marked
   * @param transitions     The list of transitions (if null, then a new list is made)
   * @param isInitialState  Whether or not this is the initial state
   * @param id              The index where the state should be added at
   * @return                Whether or not the addition was successful (returns {@code false} if a state already existed there)
   **/
  public boolean addStateAt(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState, long id) {

    if (transitions == null)
      transitions = new ArrayList<Transition>();

      /* Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs) */
    
    if (id > MAX_STATE_CAPACITY) {
      System.err.println("ERROR: Could not write state to file (exceeded maximum state capacity).");
      return false;
    }

      /* Increase the maximum allowed characters per state label */
    
    if (label.length() > labelLength) {

      // If we cannot increase the capacity, indicate a failure
      if (label.length() > MAX_LABEL_LENGTH) {
        System.err.println("ERROR: Could not write state to file (exceeded maximum label length).");
        return false;
      }

      recreateBodyFile(
        eventCapacity,
        stateCapacity,
        transitionCapacity,
        label.length(),
        nBytesPerEventID,
        nBytesPerStateID
      );

    }

      /* Increase the maximum allowed transitions per state */

    if (transitions.size() > transitionCapacity) {

      // If we cannot increase the capacity, indicate a failure (NOTE: This will likely never happen)
      if (transitions.size() > MAX_TRANSITION_CAPACITY) {
        System.err.println("ERROR: Could not write state to file (exceeded maximum transition capacity).");
        return false;
      }

      recreateBodyFile(
        eventCapacity,
        stateCapacity,
        transitions.size(),
        labelLength,
        nBytesPerEventID,
        nBytesPerStateID
      );

    }

      /* Increase the number of allowed states */

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
        eventCapacity,
        newStateCapacity,
        transitionCapacity,
        labelLength,
        nBytesPerEventID,
        newNBytesPerStateID
      );

    }

      /* Write new state to file */
    
    State state = new State(label, id, marked, transitions);
    
    if (!StateIO.writeToFile(state, baf, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
      System.err.println("ERROR: Could not write state to file.");
      return false;
    }

    nStates++;

      /* Update initial state */
    
    if (isInitialState)
      initialState = id;

      /* Update header file */
    
    headerFileNeedsToBeWritten = true;

    return true;
  }

  /**
   * Add the specified event to the set.
   * @implNote It is assumed that the new event is not already a member of the set (it is not checked for here for efficiency purposes).
   * @param label         The "name" of the new event
   * @param observable    Whether or not the event is observable
   * @param controllable  Whether or not the event is controllable
   * @return              The ID of the added event (0 indicates failure)
   **/
  public int addEvent(String label, boolean[] observable, boolean[] controllable) {

      /* Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs) */

    if (events.size() == MAX_EVENT_CAPACITY) {
      System.err.println("ERROR: Could not add event (reached maximum event capacity).");
      return 0;
    }

      /* Check to see if we need to re-write the entire binary file */
    
    if (events.size() == eventCapacity) {

      // Re-create binary file
      recreateBodyFile(
        ((eventCapacity + 1) << 8) - 1,
        stateCapacity,
        transitionCapacity,
        labelLength,
        nBytesPerEventID + 1,
        nBytesPerStateID
      );

    }

      /* Instantiate the event */

    int id = events.size() + 1;
    Event event = new Event(label, id, observable, controllable);

      /* Add the event */

    if (!events.add(event) ) {
      System.err.println("ERROR: Could not add event to list.");
      return 0;
    }

    eventsMap.put(label, event);

      /* Update the header file */

    headerFileNeedsToBeWritten = true;

    return id;

  }

  /**
   * Add the specified event to the set if it does not already exist.
   * @param label         The "name" of the new event
   * @param observable    Whether or not the event is observable
   * @param controllable  Whether or not the event is controllable
   * @return              The ID of the added event (negative ID indicates that the event already existed)
   *                      or {@code 0}, which indicates failure (occurring when maximum number of events has been reached)
   **/
  public int addEventIfNonExisting(String label, boolean[] observable, boolean[] controllable) {
    
    Event event = getEvent(label);

    if (event == null)
      return addEvent(label, observable, controllable);
    else
      return -event.getID();

  }

  /**
   * Add the entire list of events to `the automaton.
   * @param newEvents The list of events to add
   **/
  protected void addAllEvents(List<Event> newEvents) {

    for (Event e : newEvents)
      addEvent(e.getLabel(), (boolean[]) e.isObservable().clone(), (boolean[]) e.isControllable().clone());

  }

  /**
   * Add the entire list of events to the automaton (ensuring that no duplicates are added).
   * @param newEvents                       The list of events to add
   * @throws IncompatibleAutomataException  If one of the events to be added is incompatible with an existing event
   **/
  protected void addEventsWithErrorChecking(List<Event> newEvents) throws IncompatibleAutomataException {

    for (Event event1 : newEvents) {

      Event event2 = getEvent(event1.getLabel());

      if (event2 == null)
        addEvent(event1.getLabel(), (boolean[]) event1.isObservable().clone(), (boolean[]) event1.isControllable().clone());
      else if (!Arrays.equals(event1.isObservable(), event2.isObservable()) || !Arrays.equals(event1.isControllable(), event2.isControllable()))
        throw new IncompatibleAutomataException();

    }

  }

  /**
   * Mark the specified as being "bad", which is used in synchronized composition.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void markTransitionAsBad(long initialStateID, int eventID, long targetStateID) {

    badTransitions.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Set the initial state to the state with the specified ID.
   * @param id  The ID of the new initial state
   **/
  public void setInitialStateID(long id) {
    initialState = id;
  }

    /* ACCESSOR METHODS */

  /**
   * Check to see if a transition exists.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @param targetStateID   The target state
   * @return                Whether or not the transition exists
   **/
  public boolean transitionExists(long initialStateID, int eventID, long targetStateID) {

    Transition transition = new Transition(getEvent(eventID), targetStateID);
    State s = getState(initialStateID);

    for (Transition t : s.getTransitions())
      if (t.equals(transition))
        return true;

    return false;

  }

  /**
   * Check to see if the specified state already has a transition with a particular event.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @return                Whether or not the transition exists
   **/
  public boolean transitionExistsWithEvent(long initialStateID, int eventID) {

    for (Transition t : getState(initialStateID).getTransitions())
      if (t.getEvent().getID() == eventID)
        return true;

    return false;

  }

  /**
   * Check to see if a transition is bad.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   * @return                 Whether or not the transition is bad
   **/
  public boolean isBadTransition(long initialStateID, int eventID, long targetStateID) {
    
    TransitionData transitionData = new TransitionData(initialStateID, eventID, targetStateID);

    for (TransitionData t : badTransitions)
      if (t.equals(transitionData))
        return true;

    return false;

  }

  /**
   * Check to see if a state exists.
   * @implNote This is a light-weight method which can be used instead of calling "{@code getState(id) != null}").
   * It does not load all of the state information, but only checks the first byte to see if it exists or not.
   * @param id  The unique identifier corresponding to the state we are looking for
   **/
  public boolean stateExists(long id) {
    return StateIO.stateExists(this, baf, id);
  }

  /**
   * Given the ID number of a state, get the state information.
   * @param id  The unique identifier corresponding to the requested state
   * @return    The requested state
   **/
  public State getState(long id) {
    return StateIO.readFromFile(this, baf, id);
  }

  /**
   * Given the label of a state, get the ID of the state.
   * @implNote This method is extremely expensive. It should only be used when abesolutely necessary.
   * @param label The unique label corresponding to the requested state
   * @return      The corresponding state ID (or null, if it was not found)
   **/
  public Long getStateID(String label) {
  
    for (long s = 1; s <= getNumberOfStates(); s++) {
      State state = StateIO.readFromFileExcludingTransitions(this, baf, s);
      if (state.getLabel().equals(label))
        return s;
    }

    return null;
  }

  /**
   * Given the ID number of a state, get the state information (excluding transitions).
   * @implNote This is a light-weight method which is used when accessing or modifying the transitions is not needed.
   * @param id  The unique identifier corresponding to the requested state
   * @return    The requested state
   **/
  public State getStateExcludingTransitions(long id) {
    return StateIO.readFromFileExcludingTransitions(this, baf, id);
  }

  /**
   * Check to see whether or not this automaton has any unmarked states.
   * @implNote This can be an expensive method.
   * @return  Whether or not this automaton has at least one unmarked state
   **/
  public boolean hasUnmarkedState() {

    for (long s = 1; s <= getNumberOfStates(); s++)
      if (!getStateExcludingTransitions(s).isMarked())
        return true;

    return false;

  }

  /**
   * Given the ID number of an event, get the event information.
   * @implNote Using this method to check for a non-existent event is inefficient, since it checks each
   *           event one by one if it wasn't able to locate the event directly. This behaviour is required
   *           since there are cases where the event list is incomplete (for example,
   *           in {@link PrunedUStructure#removeInactiveEvents()}).
   * @param id  The unique identifier corresponding to the requested event
   * @return    The requested event (or null if it does not exist)
   **/
  public Event getEvent(int id) {

    // Try to get the event by directly indexing it, and return it if it exists
    try {
      Event event = getEvents().get(id - 1);
      if (event.getID() == id)
        return event;  
    } catch (IndexOutOfBoundsException e) { }

    // Search each event one by one, looking for it
    // NOTE: This is necessary in removeInactiveEvents(), since events are in the process of being re-numbered
    for (Event event : getEvents())
      if (event.getID() == id)
        return event;

    // Return null if it did not exist
    return null;

  }

  /**
   * Given the label of an event, get the event information.
   * @param label  The unique label corresponding to the requested event
   * @return       The requested event (or null if it does not exist)
   **/
  public Event getEvent(String label) {
    return eventsMap.get(label);
  }

  /**
   * Return the list of all events (ordered by ID, where event with an ID of 1 is in position 0).
   * @return  The list of all events
   **/
  public List<Event> getEvents() {
    return events;
  }

  /**
   * Get the number of events that can be held in this automaton.
   * @return  Current event capacity
   **/
  public int getEventCapacity() {
    return eventCapacity;
  }

  /**
   * Get the number of events that are currently in this automaton.
   * @return  Number of events
   **/
  public int getNumberOfEvents() {
    return getEvents().size();
  }

  /**
   * Return the list of all inactive events.
   * @implNote This is an expensive operation.
   * @return  The list of all inactive events
   **/
  public List<Event> getInactiveEvents() {

    List<Event> inactiveEvents = new ArrayList<Event>(events);

    for (long s = 1; s <= getNumberOfStates(); s++)
      for (Transition t : getState(s).getTransitions())
        inactiveEvents.remove(t.getEvent());

    return inactiveEvents;
  }

  /**
   * Return the list of all active events.
   * @implNote This is an expensive operation.
   * @return  The list of all active events
   **/
  public List<Event> getActiveEvents() {

    List<Event> activeEvents = new ArrayList<Event>(events);

    for (Event e : getInactiveEvents())
      activeEvents.remove(e);

    return activeEvents;
  }

  /**
   * Get the number of states that are currently in this automaton.
   * @return  Number of states
   **/
  public long getNumberOfStates() {
    return nStates;
  }

  /**
   * Get the number of states that can be held in this automaton.
   * @return  Current state capacity
   **/
  public long getStateCapacity() {
    return stateCapacity;
  }

  /**
   * Get the number of transitions that are currently in this automaton.
   * @implNote This is an expensive method.
   * @return  Number of transitions
   **/
  public long getNumberOfTransitions() {

    long nTransitions = 0;

    for (long s = 1; s <= getNumberOfStates(); s++)
      nTransitions += getState(s).getNumberOfTransitions();
  
    return nTransitions;

  }

  /**
   * Get the number of transitions that can be attached to each state.
   * @return  Current transition capacity
   **/
  public int getTransitionCapacity() {
    return transitionCapacity;
  }

  /**`
   * Get the number of characters that can be used for a state's label.
   * @return  Current maximum label length
   **/
  public int getLabelLength() {
    return labelLength;
  }

  /**
   * Get the amount of space needed to store an event ID.
   * @return  Number of bytes required to store each event ID
   **/
  public int getSizeOfEventID() {
    return nBytesPerEventID;
  }

  /**
   * Get the amount of space needed to store a state ID.
   * @return  Number of bytes required to store each state ID
   **/
  public int getSizeOfStateID() {
    return nBytesPerStateID;
  }

  /**
   * Get the amount of space needed to store a state.
   * @return  Number of bytes required to store each state
   **/ 
  public long getSizeOfState() {
    return nBytesPerState;
  }

  /**
   * Get the ID of the state where the automaton begins (the entry point).
   * @return  The ID of the initial state (0 indicates that no initial state was specified)
   **/
  public long getInitialStateID() {
    return initialState;
  }

  /**
   * Get the number of controllers in the automaton (>1 indicates decentralized control).
   * @return  The number of controllers
   **/
  public int getNumberOfControllers() {
    return nControllers;
  }

  /**
   * Get the header file where this automaton is being stored.
   * @return  The header file
   **/
  public final File getHeaderFile() {
    return haf.getHeaderFile();
  }

  /**
   * Get the body file where this automaton is being stored.
   * @return  The body file
   **/
  public final File getBodyFile() {
    return baf.getBodyFile();
  }

  /**
   * Get the enum value associated with this automaton type.
   * @return  The automaton type
   **/
  public final Type getType() {
    return type;
  }

  /**
   * Get the list of bad transitions.
   * @return  The bad transitions
   **/
  public List<TransitionData> getBadTransitions() {
    return badTransitions;
  }

  /**
   * Check to see if the specified list contains at least 1 transition that is a self-loop.
   * @param list  The list of transitions
   * @return      Whether or not the list contains a self-loop
   **/
  public boolean hasSelfLoop(List<? extends TransitionData> list) {

    for (TransitionData data : list)
      if (data.initialStateID == data.targetStateID)
        return true;

    return false;

  }

  /**
   * Check to see if this automaton is deterministic.
   * @implNote This method has been added purely as a testing mechanism.
   * @return  Whether or not this automaton is deterministic
   **/
  public boolean isDeterministic() {

    for (long s = 1; s <= getNumberOfStates(); s++) {
      
      List<Transition> transitions = getState(s).getTransitions();
      Set<Integer> eventIDs = new HashSet<Integer>();

      for (Transition t : transitions)
        eventIDs.add(t.getEvent().getID());

      if (eventIDs.size() != transitions.size())
        return false;

    }

    return true;

  }

  /**
   * Check to see if all states and transtions in this automaton actually exist.
   * @implNote Does not check special transition data.
   * @implNote This method has been added purely as a testing mechanism.
   * @return  Whether or not this automaton is deterministic
   **/
  public boolean isValid() {

    for (long s = 1; s <= getNumberOfStates(); s++) {
      
      State state = getState(s);

      if (state == null)
        return false;

      for (Transition t : state.getTransitions()) {
        if (getEvent(t.getEvent().getID()) == null)
          return false;
        if (!stateExists(t.getTargetStateID()))
          return false;
      }

    }

    return true;

  }

  /**
   * Mutate this automaton by adding self-loops to all states for all inactive events.
   * @implNote This is being used for Liu's thesis implementation.
   **/
  public void addSelfLoopsForInactiveEvents() {

    List<Event> inactiveEvents = getInactiveEvents();

    for (long s = 1; s <= getNumberOfStates(); s++)
      for (Event e : inactiveEvents)
        addTransition(s, e.getID(), s);

  }

}