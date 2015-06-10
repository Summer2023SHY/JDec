/**
 * Automaton - This extensive class is able to fully represent an automaton. The usage of .hdr and .bdy files
 *             gives the potential to work with very large automata, since the entire automaton does not need
 *             to be stored in memory.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Public Class Constants
 *  -Private Class Constants
 *  -Output Mode Enum
 *  -Private Instance Variables
 *  -Constructors
 *  -Automata Operations
 *  -Image Generation
 *  -GUI Input Code Generation
 *  -Working with Files
 *  -Miscellaneous
 *  -Mutator Methods
 *  -Accessor Methods
 **/

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;
import javax.swing.*;

public class Automaton {

    /** PUBLIC CLASS CONSTANTS **/

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

  /** The maximum number of characters that each state label in an automaton can hold. */
  public static final int MAX_LABEL_LENGTH = 100;

  /** The default number of controllers in an automaton. */
  public static final int DEFAULT_NUMBER_OF_CONTROLLERS = 1;

  /** The maximum number of controllers in an automaton. */
  public static final int MAX_NUMBER_OF_CONTROLLERS = 10;

    /** PRIVATE CLASS CONSTANTS **/

  private static final int HEADER_SIZE = 60; // This is the fixed amount of space needed to hold the main variables in the .hdr file

  private static final String DEFAULT_HEADER_FILE_NAME = "temp.hdr";
  private static final String DEFAULT_BODY_FILE_NAME   = "temp.bdy";
  private static final File DEFAULT_HEADER_FILE        = new File(DEFAULT_HEADER_FILE_NAME);
  private static final File DEFAULT_BODY_FILE          = new File(DEFAULT_BODY_FILE_NAME);

    /** OUTPUT MODE ENUM **/

  /** Image of automaton can be formatted as either .png or .svg. */
  public static enum OutputMode {

    /** Output the image of the graph as .png. NOTE: The image size is limited since this image is intended for the GUI. */
    PNG,

    /** Output the image of the graph as .svg, which is an XML-based format. The image is blown up so that no nodes overlap. */
    SVG

  }

    /** PRIVATE INSTANCE VARIABLES **/

  // Events
  private Set<Event> events = new TreeSet<Event>(); // Due to Event's compareTo and equals implementations, a TreeSet cannot not guarantee that it is actually a set (considering changing this to an ArrayList)
  private Set<Event> activeEvents = new HashSet<Event>();

  // Basic properties of the automaton
  private long nStates      = 0;
  private long initialState = 0;
  private int nControllers;
  
  // Variables which determine how large the .bdy file will become
  private int eventCapacity;
  private long stateCapacity;
  private int transitionCapacity = 2;
  private int labelLength;

  // Initialized based on the above capacities
  private int nBytesPerEventID;
  private int nBytesPerStateID;
  private long nBytesPerState;

  // Special transitions (used by U-Structure)
  private List<TransitionData> badTransitions             = new ArrayList<TransitionData>();
  private List<TransitionData> unconditionalViolations    = new ArrayList<TransitionData>();
  private List<TransitionData> conditionalViolations      = new ArrayList<TransitionData>();
  private List<CommunicationData> potentialCommunications = new ArrayList<CommunicationData>();

  // File variables
  private String headerFileName = DEFAULT_HEADER_FILE_NAME;
  private String bodyFileName = DEFAULT_BODY_FILE_NAME;
  private File headerFile;
  private File bodyFile;
  private RandomAccessFile headerRAFile; // Contains basic information about automaton, needed in order to read the bodyFile, as well as the events
  private RandomAccessFile bodyRAFile; // List each state in the automaton, with the transitions
  private boolean headerFileNeedsToBeWritten;

  // GUI input
  private StringBuilder eventInputBuilder;
  private StringBuilder stateInputBuilder;
  private StringBuilder transitionInputBuilder;

    /** CONSTRUCTORS **/

  /**
   * Default constructor: create empty automaton with default capacity, wiping any previous data existing in the files.
   **/
  public Automaton() {
    this(DEFAULT_HEADER_FILE,
      DEFAULT_BODY_FILE,
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      DEFAULT_NUMBER_OF_CONTROLLERS,
      true
    );
  }

  /**
   * Implicit constructor: create an automaton with a specified number of controllers.
   * @param headerFile    The file where the header should be stored
   * @param nControllers  The number of controllers that this automaton has
   **/
  public Automaton(File headerFile, int nControllers) {
    this(
      (headerFile == null) ? DEFAULT_HEADER_FILE : headerFile,
      (headerFile == null) ? DEFAULT_BODY_FILE : new File(headerFile.getName().substring(0, headerFile.getName().length() - 4) + ".bdy"),
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      nControllers,
      true
    );
  }

  /**
   * Implicit constructor: load automaton from file.
   * @param headerFile  The file where the header should be stored
   * @param clearFiles  Whether or not the header and body files should be wiped before use
   **/
  public Automaton(File headerFile, boolean clearFiles) {
    this(
      (headerFile == null) ? DEFAULT_HEADER_FILE : headerFile,
      (headerFile == null) ? DEFAULT_BODY_FILE   : new File(headerFile.getName().substring(0, headerFile.getName().length() - 4) + ".bdy"),
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      DEFAULT_NUMBER_OF_CONTROLLERS,
      clearFiles
    );
  }

  /**
   * Implicit constructor: create automaton with specified initial capacities.
   * NOTE: Choosing larger values increases the amount of space needed to store the binary file.
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
    this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllers, clearFiles);
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

    this.headerFile     = headerFile;
    this.bodyFile       = bodyFile;
    this.headerFileName = headerFile.getName();
    this.bodyFileName   = bodyFile.getName();

      /* These variables will be overridden if we are loading information from file */

    this.eventCapacity      = eventCapacity;
    this.stateCapacity      = stateCapacity;
    this.transitionCapacity = transitionCapacity;
    this.labelLength        = labelLength;
    this.nControllers       = nControllers;

      /* Clear files */

    if (clearFiles)
      deleteFiles();
    
      /* Open files and try to load data from header */

    openFiles();

      /* Finish setting up */

    initializeVariables();
    nBytesPerState = calculateNumberOfBytesPerState(nBytesPerEventID, nBytesPerStateID, this.transitionCapacity, this.labelLength);

      /* Update header file */

    headerFileNeedsToBeWritten = true;

  }

    /** AUTOMATA OPERATIONS **/

  /**
   * Create a new copy of this automaton that has all unreachable states and transitions removed.
   * @return the accessible automaton
   **/
  public Automaton accessible() {

      /* Setup */

    Automaton automaton = new Automaton(new File("accessible.hdr"), nControllers);

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
        stack.add(t.getTargetStateID());

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
   * @return the co-accessible automaton
   **/
  public Automaton coaccessible() {

      Automaton invertedAutomaton = invert(this);

      /* Build co-accessible automaton by seeing which states are accessible from the marked states in the inverted automaton */

    Automaton automaton = new Automaton(new File("coaccessible.hdr"), nControllers);

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
   * @return the complement automaton
   **/
  public Automaton complement() {

      /* Setup */

    Automaton automaton = new Automaton(
      new File("complement.hdr"),
      new File("complement.bdy"),
      eventCapacity,
      stateCapacity,
      events.size(), // This is the new number of transitions that will be required for each state
      labelLength,
      nControllers,
      true
    );

    // Add events
    automaton.addAllEvents(events);

    // If there is no initial state, return null, so that the GUI knows to alert the user
    if (initialState == 0)
      return null;

      /* Build complement of this automaton */

    long dumpStateID = -1;

    // Add each state to the new automaton
    for (long s = 1; s <= nStates; s++) {

      State state = getState(s);

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

          // Create dump state if it has not already been made
          if (dumpStateID == -1)
            dumpStateID = automaton.addState("Dump State", false, false);

          automaton.addTransition(id, e.getID(), dumpStateID);

        }

      }

    }

      /* Add special transitions */

    copyOverSpecialTransitions(automaton);

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

      /* Return complement automaton */

    return automaton;
  }

  /**
   * Create a new version of the specified automaton which has all of the transitions going the opposite direction.
   * NOTE: This is just a shallow copy of the automaton (no special transition data is retained), which makes it slightly more efficient.
   * @param automaton The automaton to invert
   * @return the inverted automaton
   **/
  public static Automaton invert(Automaton automaton) {

      /* Create a new automaton that has each of the transitions going the opposite direction */

    Automaton invertedAutomaton = new Automaton(automaton.getEventCapacity(), automaton.getStateCapacity(), automaton.getTransitionCapacity(), automaton.getLabelLength(), automaton.getNumberOfControllers(), true);

    // Add events
    invertedAutomaton.addAllEvents(automaton.getEvents());

    // Add states
    for (long s = 1; s <= automaton.getNumberOfStates(); s++) {

      State state = automaton.getStateExcludingTransitions(s);
      invertedAutomaton.addState(state.getLabel(), state.isMarked(), s == automaton.getInitialStateID());

    }

    // Add transitions
    for (long s = 1; s <= automaton.getNumberOfStates(); s++) {

      State state = automaton.getState(s);

      for (Transition t : state.getTransitions())
        invertedAutomaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), s);

    }

    return invertedAutomaton;

  }

  /**
   * Helper method to copy over all special transition data from this automaton to another.
   * NOTE: The data is only copied over if both of the states involved in the transition actually exist
   * @param automaton The automaton in which the special transitions are being added
   **/
  private void copyOverSpecialTransitions(Automaton automaton) {

    for (TransitionData data : badTransitions)
      if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
        automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);

    for (TransitionData data : unconditionalViolations)
      if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
        automaton.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

    for (TransitionData data : conditionalViolations)
      if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
        automaton.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

    for (CommunicationData data : potentialCommunications)
      if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
        automaton.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, (CommunicationRole[]) data.roles.clone());

  }

  /**
   * Create a new copy of this automaton that is trim (both accessible and co-accessible).
   * NOTE: I am taking the accessible part of the automaton before the co-accessible part of the automaton
   * because the accessible() method has less overhead than the coaccessible() method.
   * @return the trim automaton, or null if there was no initial state specified
   **/
  public Automaton trim() {

    if (initialState == 0)
      return null;

    return accessible().coaccessible();

  }

    // WIP
  //   public Automaton observer() {

  //      /* Setup */

  //    Automaton automaton = new Automaton(new File("observer.hdr"), true);
  //    Stack<Set<Long>> stackOfConnectedIDs = new Stack<Set<Long>>();

  //    // Find all connecting states
    // Set<Long> statesConnectingToInitial = new TreeSet<Long>();
    // findConnectingStates(statesConnectingToInitial, initialState);

    // // Push initial list to the stack
  //    stackOfConnectedIDs.push(statesConnectingToInitial);
  //    boolean isInitialState = true;

  //      /* Build observer */

  //    while (stackOfConnectedIDs.size() > 0) {

  //      // Get set from stack and generate unique ID
  //      Set<Long> setOfIDs =  stackOfConnectedIDs.pop();
  //      long combinedID = createCombinedIDWithOrderedSet(setOfIDs);

  //      // Skip if this state already exists
  //      if (automaton.stateExists(combinedID))
  //        continue;

  //      // Get the states and add them to a list
  //      List<State> listOfStates = new ArrayList<State>();
  //      for (long id : setOfIDs)
  //        listOfStates.add(getState(id));

  //      // Create a label for this state, and determine whether or not this state should be marked
  //      String label = "";
  //      boolean marked = false;
  //      for (State s : listOfStates) {
  //        label += s.getLabel();
  //        if (s.isMarked())
  //          marked = true;
  //      }
  //      label = label.substring(1);

  //      // Add new state
  //      automaton.addStateAt(
  //          label,
  //          marked,
  //          new ArrayList<Transition>(),
  //          isInitialState,
  //          combinedID
  //        );

  //      isInitialState = false;

  //      // Loop through event event
  //      for (Event e : events) {

  //        // Generate list of the IDs of all reachable states from the current event
  //        Set<Long> reachableStates = new HashSet<Long>();
  //        for (State s : listOfStates)
  //          for (Transition t : s.getTransitions())
  //            if (t.getEvent().equals(e))
  //              reachableStates.add(t.getTargetStateID());

  //        if (reachableStates.size() > 0) {
  //          // automaton.addTransition();
  //        }

  //      }

  //    }

  //      /* Re-number states (by removing empty ones) */

  //    automaton.renumberStates();

     // Ensure that the header file has been written to disk 
      
    // automaton.writeHeaderFile();

  //      /* Return observer automaton */

  //    return automaton;

  //   }

  //   // UNTESTED
  //   private void findConnectingStates(Set<Long> set, long id) {

  //    // Base case
  //    if (set.contains(id))
  //      return;

  //    set.add(id);

  //    // Find all unobservable events leading from this state, and add the target states to the set
  //    for (Transition t : getState(id).getTransitions())
  //      if (!t.getEvent().isObservable())
  //        findConnectingStates(set, t.getTargetStateID());

  //   }

  /**
   * Generate the intersection of the two specified automata.
   * @param first   The first automaton
   * @param second  The second automaton
   * @return the intersection, or null if the number of controllers do not match
   **/
  public static Automaton intersection(Automaton first, Automaton second) {

      /* Error checking */

    if (first.getNumberOfControllers() != second.getNumberOfControllers())
      return null;

      /* Setup */

    Automaton automaton = new Automaton(new File("intersection.hdr"), first.getNumberOfControllers());

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

      // Find every pair of transitions that have the same events
      for (Transition t1 : transitions1)
        for (Transition t2 : transitions2)
          if (t1.getEvent().equals(t2.getEvent())) {

            // Add this pair to the stack
            stack1.add(t1.getTargetStateID());
            stack2.add(t2.getTargetStateID());

            // Add transition to the new automaton
            long targetID = combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
            automaton.addTransition(newStateID, t1.getEvent().getID(), targetID);

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
   * @param first   The first automaton
   * @param second  The second automaton
   * @return the union, or null if the number of controllers do not match
   **/
  public static Automaton union(Automaton first, Automaton second) {

      /* Error checking */

    if (first.getNumberOfControllers() != second.getNumberOfControllers())
      return null;

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
    Set<Event> privateEvents1 = new HashSet<Event>();
    privateEvents1.addAll(first.getEvents());
    for (Event e : second.getEvents())
      privateEvents1.remove(e);
    Set<Event> privateEvents2 = new HashSet<Event>();
    privateEvents2.addAll(second.getEvents());
    for (Event e : first.getEvents())
      privateEvents2.remove(e);

    // Create event set (union of both event sets)
    automaton.addAllEvents(first.getEvents());
    automaton.addEventsIfNonExisting(second.getEvents());

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
            automaton.addTransition(newStateID, t1.getEvent().getLabel(), targetID);

          }

      // Take care of the first automaton's private events
      for (Transition t : transitions1)
        if (privateEvents1.contains(t.getEvent())) {
        
        // Add the pair of states to the stack
        stack1.add(t.getTargetStateID());
        stack2.add(id2);

        // Add transition to the new automaton
        long targetID = combineTwoIDs(t.getTargetStateID(), first, id2, second);
        automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

        }

      // Take care of the second automaton's private events
      for (Transition t : transitions2)
        if (privateEvents2.contains(t.getEvent())) {
        
        // Add the pair of states to the stack
        stack1.add(id1);
        stack2.add(t.getTargetStateID());

        // Add transition to the new automaton
        long targetID = combineTwoIDs(id1, first, t.getTargetStateID(), second);
        automaton.addTransition(newStateID, t.getEvent().getLabel(), targetID);

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
   * @return the U-Structure
   **/
  public Automaton synchronizedComposition() {

      /* Setup */

    Stack<Long> stack = new Stack<Long>();
    HashSet<Long> valuesInStack = new HashSet<Long>();
    Automaton automaton = new Automaton(new File("synchronizedComposition.hdr"), true);

      /* Add initial state to the stack */

    {
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

      automaton.addStateAt(combinedStateLabel.substring(1), false, new ArrayList<Transition>(), true, combinedID);

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
        boolean isBadTransition = badTransitions.contains(new TransitionData(listOfStates.get(0).getID(), e.getID(), t1.getTargetStateID()));
        boolean isUnconditionalViolation = isBadTransition;

        // A conditional violation can only occur when an event is controllable by at least 2 controllers, and the system must have a good transition
        int counter = 0;
        for (int i = 0; i < nControllers; i++)
          if (e.isControllable()[i])
            counter++;
        boolean isConditionalViolation = (counter >= 2 && !isBadTransition);

        // For each controller
        for (int i = 0; i < nControllers; i++) {

          // Observable events by this controller
          if (e.isObservable()[i]) {

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

            combinedEventLabel += "_" + e.getLabel();
            combinedStateLabel += "_" + label;
            listOfTargetIDs.add(targetID);

            // Check to see if this controller can prevent an unconditional violation
            if (isUnconditionalViolation && e.isControllable()[i])
              if (badTransitions.contains(new TransitionData(listOfStates.get(i + 1).getID(), e.getID(), targetID)))
                isUnconditionalViolation = false;

            // Check to see if this controller can prevent a conditional violation
            if (isConditionalViolation && e.isControllable()[i])
              if (!badTransitions.contains(new TransitionData(listOfStates.get(i + 1).getID(), e.getID(), targetID)))
                isConditionalViolation = false;

          // Unobservable events by this controller
          } else {
            combinedEventLabel += "_*";
            combinedStateLabel += "_" + listOfStates.get(i + 1).getLabel();
            listOfTargetIDs.add(listOfIDs.get(i + 1));
          }

        }

        combinedEventLabel = "<" + combinedEventLabel + ">";

        long combinedTargetID = combineIDs(listOfTargetIDs, nStates);

        // Add state if it doesn't already exist
        if (!automaton.stateExists(combinedTargetID)) {

          // Add event
          automaton.addEventIfNonExisting(combinedEventLabel, new boolean[] {true}, new boolean[] {true} );

          // Add state
          if (!automaton.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
            System.err.println("ERROR: Failed to add state. Synchronized composition aborted.");
            return null;
          }
          
          // Only add the ID if it's not already waiting to be processed
          if (!valuesInStack.contains(combinedTargetID)) {
              stack.push(combinedTargetID);
              valuesInStack.add(combinedTargetID);
          } else {
            System.out.println("DEBUG: Prevented adding of state since it was already in the stack.");
          }
        }

        // Add transition
        int eventID = automaton.addTransition(combinedID, combinedEventLabel, combinedTargetID);
        if (isUnconditionalViolation)
          automaton.addUnconditionalViolation(combinedID, eventID, combinedTargetID);
        if (isConditionalViolation)
          automaton.addConditionalViolation(combinedID, eventID, combinedTargetID);

      } // for

      // For each unobservable transition in the each controller automata
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
                combinedEventLabel += "_" + t.getEvent().getLabel();
                combinedStateLabel += "_" + getStateExcludingTransitions(t.getTargetStateID()).getLabel();
              } else {
                listOfTargetIDs.add(listOfIDs.get(j));
                combinedEventLabel += "_*";
                combinedStateLabel += "_" + listOfStates.get(j).getLabel(); 
              }


            }

            combinedEventLabel = "<" + combinedEventLabel.substring(1) + ">";
            combinedStateLabel = combinedStateLabel.substring(1);
            long combinedTargetID = combineIDs(listOfTargetIDs, nStates);

            // Add state if it doesn't already exist
            if (!automaton.stateExists(combinedTargetID)) {

              // Add event
              automaton.addEventIfNonExisting(combinedEventLabel, new boolean[] {true}, new boolean[] {true} );

              // Add state
              if (!automaton.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
                System.err.println("ERROR: Failed to add state. Synchronized composition aborted.");
                return null;
              }
            
              // Only add the ID if it's not already waiting to be processed
              if (!valuesInStack.contains(combinedTargetID)) {
                  stack.push(combinedTargetID);
                  valuesInStack.add(combinedTargetID);
              } else {
                System.out.println("DEBUG: Prevented adding of state since it was already in the stack.");
              }

            }

            // Add transition
            automaton.addTransition(combinedID, combinedEventLabel, combinedTargetID);

          }
        }

      } // for


    } // while

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Ensure that the header file has been written to disk */

    automaton.writeHeaderFile();

      /* Return produced automaton */

    return automaton;

  }

  /**
   * Generate a new automaton, with all communications added (potential communications are marked).
   * @return the automaton with the added transitions
   **/
  public Automaton addCommunications() {

    // I'm not sure how we're supposed to handle automata with more than 1 controller (since our U-Structure has just one controller)
    if (nControllers != 1)
      return null;
    
      /* Setup */

    List<LabelVector> leastUpperBounds = new ArrayList<LabelVector>(generateLeastUpperBounds());
    List<CommunicationLabelVector> potentialCommunications = new ArrayList<CommunicationLabelVector>(findPotentialCommunicationLabels(leastUpperBounds));
    Automaton automaton = duplicate("addCommunications");

      /* Add communications (marking the potential communications) */

    for (long s = 1; s < automaton.getNumberOfStates(); s++) {

      State startingState = automaton.getState(s);

      // Try each least upper bound
      for (LabelVector vector : leastUpperBounds) {
        
        boolean[] vectorElementsFound = new boolean[vector.getSize()];
        State destinationState = findWhereCommunicationLeads(automaton, vector, vectorElementsFound, startingState);
        
        if (destinationState != null) {

          // Add event if it doesn't already exist
          int id;
          Event event = automaton.getEvent(vector.toString());
          if (event == null)
            id = automaton.addEvent(vector.toString(), new boolean[] {true}, new boolean[] {true});
          else
            id = event.getID();

          // Add the transition (if it doesn't already exist)
          if (!automaton.transitionExists(startingState.getID(), id, destinationState.getID())) {

            // Add transition
            automaton.addTransition(startingState.getID(), id, destinationState.getID());

            // There could be more than one potential communication, so we need to mark them all
            for (CommunicationLabelVector data : potentialCommunications)
              if (vector.equals((LabelVector) data))
                automaton.addPotentialCommunication(startingState.getID(), id, destinationState.getID(), data.roles);
    
          }
         
        }

      }

    }

    // Ensure that the header file has been written to disk
    automaton.writeHeaderFile();

    return automaton;
    
  }

  /**
   * Using recursion, starting at a given state, determine which state the specified communication leads to (if it exists).
   * @param automaton           The automaton we are working with
   * @param communication       The event vector representing the communication
   * @param vectorElementsFound Indicates which elements of the vector have been found
   * @param currentState        The state that we are currently on
   * @return the destination state (or null if the communication does not lead to a state)
   **/
  private static State findWhereCommunicationLeads(Automaton automaton, LabelVector communication, boolean[] vectorElementsFound, State currentState) {

      /* Base case */

    // We have found the destination if all vector elements have been found
    boolean finished = true;
    for (int i = 0; i < communication.getSize(); i++)
      if (!communication.getLabelAtIndex(i).equals("*") && !vectorElementsFound[i]) {
        finished = false;
        break;
      }

    if (finished)
      return currentState;

      /* Recursive case */

    // Try all transitions leading from this state
    outer: for (Transition t : currentState.getTransitions()) {

      boolean[] copy = (boolean[]) vectorElementsFound.clone();

      // Check to see if the event vector of this transition is compatible with what we've found so far
      for (int i = 0; i < t.getEvent().vector.getSize(); i++) {

        String element = t.getEvent().vector.getLabelAtIndex(i);

        if (!element.equals("*")) {

          // Conflict since we have already found an element for this index (so they aren't compatible)
          if (copy[i])
            continue outer;

          // Is compatible
          else if (element.equals(communication.getLabelAtIndex(i)))
            copy[i] = true;

          // Conflict since the elements do not match (meaning they aren't compatible)
          else
            continue outer;
        }

      }

      // Recursive call to the state where this transition leads
      State destinationState = findWhereCommunicationLeads(automaton, communication, copy, automaton.getState(t.getTargetStateID()));
      
      // Return destination if it is found (there will only ever be one destination for a given communication from a given state, so we can stop as soon as we find it the first time)
      if (destinationState != null)
        return destinationState;

    }

    return null;

  }

  /**
   * Given the complete set of least upper bounds (LUBs), return the subset of LUBs which are the event vectors for potential communications.
   * @param leastUpperBounds  The set of LUBs
   * @return the set of potential communications, including communication roles
   **/
  private Set<CommunicationLabelVector> findPotentialCommunicationLabels(List<LabelVector> leastUpperBounds) {

      /* Separate observable and unobservable labels */

    Set<LabelVector> observableLabels = new HashSet<LabelVector>();
    Set<LabelVector> unobservableLabels = new HashSet<LabelVector>();

    for (LabelVector v : leastUpperBounds)
      if (v.getLabelAtIndex(0).equals("*"))
        unobservableLabels.add(v);
      else
        observableLabels.add(v);

      /* Find potential communications */

    Set<CommunicationLabelVector> potentialCommunications = new HashSet<CommunicationLabelVector>();
    
    for (LabelVector v1 : observableLabels) {
      for (LabelVector v2 : unobservableLabels) {

          /* Error checking */

        if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
          System.err.println("ERROR: Bad event vectors. Least upper bounds generation aborted.");
          return null;
        }

          /* Setup */

        CommunicationRole[] roles = new CommunicationRole[v1.getSize() - 1];

          /* Build least upper bound */

        boolean valid = true;
        String potentialCommunication = "";
        String eventLabel = null;

        for (int i = 0; i < v1.getSize(); i++) {

          String label1 = v1.getLabelAtIndex(i);
          String label2 = v2.getLabelAtIndex(i);

          // Check to see if they are incompatible or if this potential communication has already been taken care of
          if (!label1.equals("*") && !label2.equals("*")) {
            valid = false;
            break;
          }

          // Append vector element
          String newEventLabel = null;
          if (!label1.equals("*")) {
            potentialCommunication += "_" + label1;
            newEventLabel = label1;
            if (i > 0)
              roles[i - 1] = CommunicationRole.SENDER;
          } else if (!label2.equals("*")) {
            potentialCommunication += "_" + label2;
            newEventLabel = label2;
            if (i > 0)
              roles[i - 1] = CommunicationRole.RECIEVER;
          } else {
            potentialCommunication += "_*";
            if (i > 0)
              roles[i - 1] = CommunicationRole.NONE;
          }

          // Make sure that the senders and recievers all are working with the same event
          if (eventLabel != null && newEventLabel != null && !newEventLabel.equals(eventLabel)) {
            valid = false;
            break;
          }

          if (eventLabel == null)
            eventLabel = newEventLabel;

        }

          /* Add it to the set */

        if (valid) {

          // Add all potential communications (1 for each sender)
          for (int i = 0; i < roles.length; i++) {

            if (roles[i] == CommunicationRole.SENDER) {

              CommunicationRole[] copy = (CommunicationRole[]) roles.clone();
              
              // Remove all other senders
              for (int j = 0; j < copy.length; j++)
                if (j != i && copy[j] == CommunicationRole.SENDER)
                  copy[j] = CommunicationRole.NONE;
              
              // Add potential communication
              potentialCommunications.add(new CommunicationLabelVector("<" + potentialCommunication.substring(1) + ">", copy));

            }

          }

        }

      } // for
    } // for

    return potentialCommunications;

  }

  /**
   * Generate all possible least upper bounds (LUBs) of the event vectors (after synchronized composition).
   * @return the set of all LUBs in the form of event vectors
   **/
  private Set<LabelVector> generateLeastUpperBounds() {

      /* Setup */

    Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
    for (Event e : events)
      leastUpperBounds.add(new LabelVector(e.getLabel()));

      /* Continue to find LUBs using pairs of event vectors until there are no new ones left to find */

    boolean foundNew = true;
    while (foundNew) {

      List<LabelVector> temporaryList = new ArrayList<LabelVector>();
      
      // Try all pairs
      for (LabelVector v1 : leastUpperBounds) {
        for (LabelVector v2 : leastUpperBounds) {

            /* Error checking */

          if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
            System.err.println("ERROR: Bad event vectors. Least upper bounds generation aborted.");
            return null;
          }

            /* Build least upper bound */

          boolean valid = true;
          String leastUpperBound = "";
          for (int i = 0; i < v1.getSize(); i++) {

            String label1 = v1.getLabelAtIndex(i);
            String label2 = v2.getLabelAtIndex(i);

            // Check for incompatibility
            if (!label1.equals("*") && !label2.equals("*") && !label1.equals(label2)) {
              valid = false;
              break;
            }

            // Append vector element
            if (label1.equals("*"))
              leastUpperBound += "_" + label2;
            else
              leastUpperBound += "_" + label1;

          }

            /* Add to the temporary list */

          if (valid)
            temporaryList.add(new LabelVector("<" + leastUpperBound.substring(1) + ">"));

        } // for
      } // for

      // Add all of the vectors from the temporary list
      foundNew = false;
      for (LabelVector v : temporaryList)
        if (leastUpperBounds.add(v))
          foundNew = true;

    }

    return leastUpperBounds;

  }

  /**
   * Check feasibility for all possible communication protocols, printing out the results.
   * @param communications  The communications to be considered (which should be a subset of the potentialCommunications list)
   **/
  public void printFeasibleProtocols(List<CommunicationData> communications) {

    // Generate powerset of communication protocols
    System.out.println("set size: " + communications.size());
    List<Set<CommunicationData>> protocols = new ArrayList<Set<CommunicationData>>();
    powerSet(protocols, communications, new HashSet<CommunicationData>(), 0);

    System.out.println("powerset size:" + protocols.size());

    // Create inverted automaton, so that we can explore the automaton by crossing transitions from either direction
    Automaton invertedAutomaton = invert(this);

    for (Set<CommunicationData> protocol : protocols) {

      System.out.println("here: " + protocol);

      // Ignore the protocol with no communications (doesn't make sense in our context)
      if (protocol.size() == 0)
        continue;

      if (isFeasibleProtocol(protocol, invertedAutomaton)) {
        
        System.out.println("FEASIBLE PROTOCOL:");
        for (CommunicationData data : protocol)
          System.out.println(data.toString(this));

      }

    }

  }

  /**
   * Make the specified protocol feasible (returning it as a new set).
   * @param protocol            The protocol that is being made feasible
   * @param invertedAutomaton   An automaton identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @return the feasible protocol
   **/
  public Set<CommunicationData> makeProtocolFeasible(Set<CommunicationData> protocol, Automaton invertedAutomaton) {

    Set<CommunicationData> feasibleProtocol = new HashSet<CommunicationData>();

    // Start at each communication in the protocol
    outer: for (CommunicationData data : protocol) {

      feasibleProtocol.add(data);

      // Find reachable states
      Set<Long> reachableStates = new HashSet<Long>();
      findReachableStates(this, invertedAutomaton, reachableStates, data.initialStateID, data.getIndexOfSender() + 1);

      // Check for an indistinguishable state outside the protocol
      for (Long id : reachableStates)

        // Check if this state is indistinguishable
        for (Transition t : getState(id).getTransitions()) {

          if (t.getEvent().getID() == data.eventID) {

            // Check if this communication is outside of the protocol
            boolean found = false;
            for (CommunicationData data2 : protocol)
              if (data2.initialStateID == id && data2.eventID == t.getEvent().getID() && data2.targetStateID == t.getTargetStateID()) {
                found = true;
                break;
              }

            // If this is not in the protocol, then add it to the protocol to maintain feasibility
            if (!found) {
              for (CommunicationData data2 : potentialCommunications)
                if (data2.initialStateID == id && data2.eventID == t.getEvent().getID() && data2.targetStateID == t.getTargetStateID()) {
                  feasibleProtocol.add(data2);
                  break;
                }
            }

          }
        }
    }

    System.out.println("FEASIBLE PROTOCOL:");
    for (CommunicationData data : protocol)
      System.out.println(data.toString(this));

    return feasibleProtocol;

  }


  /**
   * Check to see if the specified protocol is feasible.
   * @param protocol            The protocol that is being checked for feasibility
   * @param invertedAutomaton   An automaton identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @return whether or not the protocol is feasible
   **/
  private boolean isFeasibleProtocol(Set<CommunicationData> protocol, Automaton invertedAutomaton) {

    // Start at each communication in the protocol
    outer: for (CommunicationData data : protocol) {

      // Find reachable states
      Set<Long> reachableStates = new HashSet<Long>();
      findReachableStates(this, invertedAutomaton, reachableStates, data.initialStateID, data.getIndexOfSender() + 1);

      // Check for an indistinguishable state outside the protocol
      for (Long id : reachableStates)

        // Check if this state is indistinguishable
        for (Transition t : getState(id).getTransitions()) {
          
          if (t.getEvent().getID() == data.eventID) {

            // Check if this communication is outside of the protocol
            boolean found = false;
            for (CommunicationData data2 : protocol)
              if (data2.initialStateID == id && data2.eventID == t.getEvent().getID() && data2.targetStateID == t.getTargetStateID()) {
                found = true;
                break;
              }

            // If this is not in the protocol, then it is not feasible
            if (!found)
              return false;

          }
        }
    }
    
    return true;

  }

  /**
   * Using recursion, determine which states are reachable through transitions which are unobservable to the sender.
   * @param automaton           The relevant automaton
   * @param invertedAutomaton   An automaton identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @param reachableStates     The set of reachable states that are being built during this recursive process
   * @param currentStateID      The current state
   * @param vectorIndexOfSender The index in the event vector which corresponds to the sending controller
   **/
  private static void findReachableStates(Automaton automaton, Automaton invertedAutomaton, Set<Long> reachableStates, long currentStateID, int vectorIndexOfSender) {

    reachableStates.add(currentStateID);

    for (Transition t : automaton.getState(currentStateID).getTransitions()) {

      LabelVector vector = t.getEvent().vector;
      boolean unobservableToSender = (vector.getLabelAtIndex(0).equals("*") || vector.getLabelAtIndex(vectorIndexOfSender).equals("*"));

      if (unobservableToSender && !reachableStates.contains(t.getTargetStateID()))
        findReachableStates(automaton, invertedAutomaton, reachableStates, t.getTargetStateID(), vectorIndexOfSender);

    }

    for (Transition t : invertedAutomaton.getState(currentStateID).getTransitions()) {

      LabelVector vector = t.getEvent().vector;
      boolean unobservableToSender = (vector.getLabelAtIndex(0).equals("*") || vector.getLabelAtIndex(vectorIndexOfSender).equals("*"));

      if (unobservableToSender && !reachableStates.contains(t.getTargetStateID()))
        findReachableStates(automaton, invertedAutomaton, reachableStates, t.getTargetStateID(), vectorIndexOfSender);

    }

  }

  /**
   * A generic method to generate the powerset of the given list, which are stored in the list of sets that you give it.
   * @param results         This is a list of sets where all of the sets in the powerset will be stored
   * @param masterList      This is the original list of elements in the set
   * @param elementsChosen  This maintains the elements chosen so far (when you call this method you should give an empty set)
   * @param index           The current index in the master list (when you call this method, this parameter should be 0)
   **/
  private static <T> void powerSet(List<Set<T>> results, List<T> masterList, Set<T> elementsChosen, int index) {

      /* Base case */

    if (index == masterList.size()) {
      results.add(elementsChosen);
      return;
    }

      /* Recursive case */

    Set<T>  includingElement = new HashSet<T>(),
            notIncludingElement = new HashSet<T>();
    
    for (T e : elementsChosen) {
      includingElement.add(e);
      notIncludingElement.add(e);
    }

    includingElement.add(masterList.get(index));

    // Recursive calls
    powerSet(results, masterList, includingElement, index + 1);
    powerSet(results, masterList, notIncludingElement, index + 1);

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

          // Update initial state ID (if applicable)
          if (initialState == s)
            initialState = newStateID;

          // Update ID of state
          state.setID(newStateID);

          // Update IDs of the target state of each
          for (Transition t : state.getTransitions()) {

            // Get new ID of state
            buffer = new byte[nBytesPerStateID];
            mappingRAFile.seek(nBytesPerStateID * t.getTargetStateID());
            mappingRAFile.read(buffer);
            long newTargetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

            t.setTargetStateID(newTargetStateID);
          }

          // Write the updated state to the new file
          if (!state.writeToFile(newBodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID))
            System.err.println("ERROR: Could not write state to file.");

        }

      }

        /* Update the special transitions in the header file */

      renumberTransitionData(mappingRAFile, badTransitions);
      renumberTransitionData(mappingRAFile, unconditionalViolations);
      renumberTransitionData(mappingRAFile, conditionalViolations);
      renumberTransitionData(mappingRAFile, potentialCommunications);

        /* Remove old body file and mappings file */

      try {

        bodyRAFile.close();

        if (!bodyFile.delete())
          System.err.println("ERROR: Could not delete old body file.");
              
        if (!mappingFile.delete())
          System.err.println("ERROR: Could not delete mapping file.");

      } catch (SecurityException e) {
        e.printStackTrace();
      }
          /* Rename new body file */

      newBodyFile.renameTo(new File(bodyFileName));
      bodyRAFile = newBodyRAFile;

    } catch (IOException e) {
      e.printStackTrace();
    }

      /* Update header file (since we re-numbered the information in the special transitions) */

    headerFileNeedsToBeWritten = true;

  }

  /**
   * Helper method to re-number states in the specified list of special transitions.
   * @param mappingRAFile The binary file containing the state ID mappings
   * @param list          The list of special transition data
   **/
  private void renumberTransitionData(RandomAccessFile mappingRAFile, List<? extends TransitionData> list) throws IOException {

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
   * Given two state IDs and their respective automatons, create a unique combined ID.
   * NOTE: The reasoning behind this formula is analogous to the following: if you have a table with N rows and M columns,
   * every cell is guaranteed to have a different combination of row and column indexes.
   * @param id1     The state ID from the first automaton
   * @param first   The first automaton
   * @param id2     The state ID from the second automaton
   * @param second  The second automaton
   * @return the combined ID
   **/ 
  private static long combineTwoIDs(long id1, Automaton first, long id2, Automaton second) {

    return ((id2 - 1) * first.getNumberOfStates() + id1);

  }

  /**
   * Given a list of IDs and a maximum possible ID, create a unique combined ID.
   * @param list  The list of IDs
   * @param maxID The largest possible value to be used as an ID
   * @return the combined ID
   **/
  public static long combineIDs(List<Long> list, long maxID) {

    long combinedID = 0;

    for (Long id : list) {
      combinedID *= maxID + 1;
      combinedID += id;
    }

    return combinedID;

  }

  /**
   * Given a combined ID, obtain the list of original IDs by reversing the process.
   * @param combinedID  The combined ID
   * @param maxID       The largest possible value to be used as an ID
   * @return the original list of IDs
   **/
  public static List<Long> separateIDs(long combinedID, long maxID) {

    List<Long> list = new ArrayList<Long>();

    while (combinedID > 0) {

      list.add(0, combinedID % (maxID + 1));
      combinedID /= (maxID + 1);

    }

    return list;

  }

    /** IMAGE GENERATION **/

  /**
   * Output this automaton in a format that is readable by GraphViz, then export as requested.
   * @param size            The requested width and height in pixels
   * @param mode            The output type
   * @param outputFileName  The location to put the generated output
   * @return whether or not the output was successfully generated
   **/
  public boolean generateImage(int size, OutputMode mode, String outputFileName) {

      /* Setup */

    StringBuilder str = new StringBuilder();
    str.append("digraph G {");
    str.append("node [shape=circle, style=bold, constraint=false];");

      /* Constrain the size of the image if it's a PNG (since we will be displaying it on the GUI) */

    if (mode == OutputMode.PNG) {
      double inches = ((double) size) / 96.0; // Assuming DPI is 96
      str.append("size=\"" + inches + "," + inches + "\";");
      str.append("ratio=fill;");
    }

      /* Mark special transitions */

    HashMap<String, String> additionalEdgeProperties = new HashMap<String, String>();
    for (TransitionData t : unconditionalViolations) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (additionalEdgeProperties.containsKey(edge))
        additionalEdgeProperties.put(edge, additionalEdgeProperties.get(edge) + ",color=red");
      else
        additionalEdgeProperties.put(edge, ",color=red"); 
    }
    for (TransitionData t : conditionalViolations) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (additionalEdgeProperties.containsKey(edge))
        additionalEdgeProperties.put(edge, additionalEdgeProperties.get(edge) + ",color=green3");
      else
        additionalEdgeProperties.put(edge, ",color=green3"); 
    }
    for (TransitionData t : badTransitions) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (additionalEdgeProperties.containsKey(edge))
        additionalEdgeProperties.put(edge, additionalEdgeProperties.get(edge) + ",style=dotted");
      else
        additionalEdgeProperties.put(edge, ",style=dotted"); 
    }
    for (TransitionData t : potentialCommunications) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (additionalEdgeProperties.containsKey(edge))
        additionalEdgeProperties.put(edge, additionalEdgeProperties.get(edge) + ",color=blue,fontcolor=blue");
      else
        additionalEdgeProperties.put(edge, ",color=blue,fontcolor=blue"); 
    }
    
      /* Draw all states and their transitions */

    for (long s = 1; s <= nStates; s++) {

      // Get state from file
      State state = getState(s);

      // Draw state
      if (state.isMarked())
        str.append(String.format("\"_%s\" [peripheries=2,label=\"%s\"];", state.getLabel(), state.getLabel()));
      else
        str.append(String.format("\"_%s\" [peripheries=1,label=\"%s\"];", state.getLabel(), state.getLabel()));

      // Draw each of its transitions
      ArrayList<Transition> transitionsToSkip = new ArrayList<Transition>();
      for (Transition t1 : state.getTransitions()) {

        // Skip it if this was already taken care of (grouped into another transition going to the same target state)
        if (transitionsToSkip.contains(t1))
          continue;

        String label = "";

        // Look for all transitions that can be group with this one (for simplicity of code, this will also include 't1')
        for (Transition t2 : state.getTransitions()) {

          // Skip it if this was already taken care of (grouped into another transition going to the same target state)
          if (transitionsToSkip.contains(t2))
            continue;

          // Check to see if both transitions lead to the same event
          if (t1.getTargetStateID() == t2.getTargetStateID()) {
            label += "," + t2.getEvent().getLabel();
            transitionsToSkip.add(t2);
          }

        }

        // Add transition
        String edge = "\"_" + state.getLabel() + "\" -> \"_" + getStateExcludingTransitions(t1.getTargetStateID()).getLabel() + "\"";
        str.append(edge);
        str.append(" [label=\"" + label.substring(1) + "\"");

        // Add additional properties (if applicable)
        String properties = additionalEdgeProperties.get(edge);
        if (edge != null)
          str.append(properties);
        
        // if (nControllers == 1) {
        //  if (!t1.getEvent().isObservable()[0])
        //    str.append(",style=dotted");

        //  if (!t1.getEvent().isControllable()[0])
        //    str.append(",color=red");
        // }

        str.append("];");
      }

    }

      /* Add arrow towards initial state */

    if (initialState > 0) {
      str.append("node [shape=plaintext];");
      str.append("\" \"-> \"_" + getStateExcludingTransitions(initialState).getLabel() + "\" [color=blue];");
    }

    str.append("}");

      /* Generate image */

    try {

      // Write DOT language to file
      PrintStream out = new PrintStream(new FileOutputStream("out.tmp"));
      out.print(str.toString());

      // Produce PNG from DOT language
      Process process = null;

      if (mode == OutputMode.SVG) 
        process = new ProcessBuilder(
          (nStates > 100) ? "neato": "dot",
          "-Goverlap=scale",
          "-Tsvg",
          "out.tmp",
          "-o",
          outputFileName
        ).start();
      else if (mode == OutputMode.PNG)
        process = new ProcessBuilder(
          (nStates > 100) ? "neato": "dot",
          "-Tpng",
          "out.tmp",
          "-o",
          outputFileName
        ).start();

      // Wait for it to finish
      if (process.waitFor() != 0) {
        System.err.println("ERROR: GraphViz failed to generate image of graph.");
        return false;
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      return false;
    }
    
    return true;

  }

  /**
   * Load the generated graph image from file.
   * @param fileName  The name of the image to be loaded
   * @return image, or null if it could not be loaded
   **/
  public BufferedImage loadImageFromFile(String fileName) {

    try {
      return ImageIO.read(new File(fileName));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

  }

    /** GUI INPUT CODE GENERATION **/

  /**
   * Generates GUI input code from this automaton (which is useful when loading automaton from file in the GUI).
   * NOTE: Further calls to getEventInput(), getStateInput(), and/or getTransitionInput() are needed to actually
   * get the generated input code.
   **/
  public void generateInputForGUI() {

    eventInputBuilder = new StringBuilder();
    stateInputBuilder = new StringBuilder();
    transitionInputBuilder = new StringBuilder();

      /* Generate event input */

    int counter = 0;

    for (Event e : events) {

      // Label
      eventInputBuilder.append(e.getLabel() + ",");

      // Observability properties
      for (int i = 0; i < nControllers; i++)
        eventInputBuilder.append((e.isObservable()[i] ? "T" : "F"));

      eventInputBuilder.append(",");

      // Controllability properties
      for (int i = 0; i < nControllers; i++)
        eventInputBuilder.append((e.isControllable()[i] ? "T" : "F"));
      
      // End of line character
      if (++counter < events.size())
        eventInputBuilder.append("\n");

    }

      /* Generate state and transition input */

    boolean firstTransitionInStringBuilder = true;

    for (long s = 1; s <= nStates; s++) {

      try {

        State state = getState(s);

        if (state == null) {
          System.err.println("ERROR: State could not be loaded. id=" + s);
          continue;
        }

        // Place '@' before label if this is the initial state
        if (s == initialState)
          stateInputBuilder.append("@");

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
              + "," + getStateExcludingTransitions(t.getTargetStateID()).getLabel()
            );

            /* Append special transition information */

          String specialTransition = "";
          TransitionData transitionData = new TransitionData(s, t.getEvent().getID(), t.getTargetStateID());
          
          if (badTransitions.contains(transitionData))
            specialTransition += ",BAD";
          
          if (unconditionalViolations.contains(transitionData))
            specialTransition += ",UNCONDITIONAL_VIOLATION";
          
          if (conditionalViolations.contains(transitionData))
            specialTransition += ",CONDITIONAL_VIOLATION";
          
          // Search entire list since there may be more than one potential communication
          for (CommunicationData data : potentialCommunications) {
            if (data.equals(transitionData)) {
              specialTransition += ",POTENTIAL_COMMUNICATION-";
              for (CommunicationRole role : data.roles)
                specialTransition += role.getCharacter();
            }
          }
          
          if (!specialTransition.equals(""))
            transitionInputBuilder.append(":" + specialTransition.substring(1));
        
        }

      } catch (NullPointerException e) {
        System.out.println("NULL POINTER EXCEPTION at state " + s + ", so it was skipped..");
      }
    }

  }

  /**
   * Get the event input code.
   * NOTE: Must call generateInputForGUI() prior to use.
   * @return input code in the form of a String
   **/
  public String getEventInput() {

    if (eventInputBuilder == null)
      return null;

    return eventInputBuilder.toString();

  }

  /**
   * Get the state input code.
   * NOTE: Must call generateInputForGUI() prior to use.
   * @return input code in the form of a String
   **/
  public String getStateInput() {

    if (stateInputBuilder == null)
      return null;

    return stateInputBuilder.toString();

  }

  /**
   * Get the transition input code.
   * NOTE: Must call generateInputForGUI() prior to use.
   * @return input code in the form of a String
   **/
  public String getTransitionInput() {

    if (transitionInputBuilder == null)
      return null;

    return transitionInputBuilder.toString();

  }

    /** WORKING WITH FILES **/

  /**
   * Duplicate this automaton and store it in a different set of files.
   * @param fileName  The name of the new files, excluding the extension (ex: "file" will store the automaton in "file.hdr" and "file.bdy")
   * @return the duplicated automaton
   **/
  public Automaton duplicate(String fileName) {

    File newHeaderFile = new File(fileName + ".hdr");
    File newBodyFile = new File(fileName + ".bdy");

    try {
    
      Files.copy(headerFile.toPath(), newHeaderFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      Files.copy(bodyFile.toPath(), newBodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return new Automaton(newHeaderFile, false);

  }

  /**
   * Open the header and body files, and read in the header file.
   **/
  private void openFiles() {

    try {

      headerRAFile = new RandomAccessFile(headerFile, "rw");
      bodyRAFile = new RandomAccessFile(bodyFile, "rw");

      readHeaderFile();

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * Files need to be closed on tge Windows operating system because there are problems trying to delete files if they are in use.
   * NOTE: Do not attempt to use the automaton again unless the files are re-opened using openFiles().
   **/
  public void closeFiles() {

      try {

        if (headerFileNeedsToBeWritten)
          writeHeaderFile();

        headerRAFile.close();
        bodyRAFile.close();

      } catch (IOException e) {
        e.printStackTrace();
      }

  }

  /**
   * Delete the current header and body files.
   **/
  private void deleteFiles() {

    try {

      if (!headerFile.delete() && headerFile.exists())
        System.err.println("ERROR: Could not delete header file.");
      
      if (!bodyFile.delete() && headerFile.exists())
        System.err.println("ERROR: Could not delete body file.");

    } catch (SecurityException e) {
      e.printStackTrace();
    }

  }

  /**
   * Delete the temporary header and body files (if they exist).
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
   * Write all of the header information to file.
   **/
  public void writeHeaderFile() {

    // Do not write the header file unless we need to
    if (!headerFileNeedsToBeWritten)
      return;

      /* Write the header of the .hdr file */
    
    byte[] buffer = new byte[HEADER_SIZE];

    ByteManipulator.writeLongAsBytes(buffer, 0, nStates, 8);
    ByteManipulator.writeLongAsBytes(buffer, 8, eventCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 12, stateCapacity, 8);
    ByteManipulator.writeLongAsBytes(buffer, 20, transitionCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 24, labelLength, 4);
    ByteManipulator.writeLongAsBytes(buffer, 28, initialState, 8);
    ByteManipulator.writeLongAsBytes(buffer, 36, nControllers, 4);
    ByteManipulator.writeLongAsBytes(buffer, 40, events.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 44, badTransitions.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 48, unconditionalViolations.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 52, conditionalViolations.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 56, potentialCommunications.size(), 4);

    try {

      headerRAFile.seek(0);
      headerRAFile.write(buffer);

        /* Write the events to the .hdr file */

      for (Event e : events) {
      
        // Fill the buffer
        buffer = new byte[ (2 * nControllers) + 4 + e.getLabel().length()];

        // Read event properties (NOTE: If we ever need to condense the space required to hold an event in a file, we can place a property in each bit instead of each byte)
        int index = 0;
        for (int i = 0; i < nControllers; i++) {
          buffer[index] = (byte) (e.isObservable()[i] ? 1 : 0);
          buffer[index + 1] = (byte) (e.isControllable()[i] ? 1 : 0);
          index += 2;
        }

        // Write the length of the label
        ByteManipulator.writeLongAsBytes(buffer, index, e.getLabel().length(), 4);
        index += 4;

        // Write each character of the label
        for (int i = 0; i < e.getLabel().length(); i++)
          buffer[index++] = (byte) e.getLabel().charAt(i);

        headerRAFile.write(buffer);

      }

        /* Write special transitions to the .hdr file */

      writeTransitionDataToHeader(badTransitions);
      writeTransitionDataToHeader(unconditionalViolations);
      writeTransitionDataToHeader(conditionalViolations);
      writeCommunicationDataToHeader(potentialCommunications);

        /* Indicate that the header file no longer need to be written */

      headerFileNeedsToBeWritten = false;

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * A helper method to write a list of special transitions to the header file.
   * @param list  The list of transition data
   **/
  private void writeTransitionDataToHeader(List<TransitionData> list) throws IOException {

    byte[] buffer = new byte[list.size() * 20];
    int index = 0;

    for (TransitionData data : list) {

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

    }

    headerRAFile.write(buffer);

  }

  /**
   * A helper method to write a list of special transitions to the header file.
   * NOTE: This could be made more efficient by using one buffer for all communication data. This
   * is only possible because data.roles.length is supposed to be the same for all data in the list.
   * @param list  The list of transition data
   **/
  private void writeCommunicationDataToHeader(List<CommunicationData> list) throws IOException {

    for (CommunicationData data : list) {

      byte[] buffer = new byte[20 + data.roles.length];
      int index = 0;

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

      for (CommunicationRole role : data.roles)
        buffer[index++] = role.getNumericValue();
      
      headerRAFile.write(buffer);

    }

  }

  /**
   * Read all of the header information from file.
   **/
  private void readHeaderFile() {

    byte[] buffer = new byte[HEADER_SIZE];

    try {

       /* Do not try to load an empty file */

      if (headerRAFile.length() == 0)
        return;

       /* Go to the proper position and read in the bytes */

      headerRAFile.seek(0);
      headerRAFile.read(buffer);

       /* Calculate the values stored in these bytes */

      nStates            = ByteManipulator.readBytesAsLong(buffer, 0, 8);
      eventCapacity      = (int) ByteManipulator.readBytesAsLong(buffer, 8, 4);
      stateCapacity      = ByteManipulator.readBytesAsLong(buffer, 12, 8);
      transitionCapacity = (int) ByteManipulator.readBytesAsLong(buffer, 20, 4);
      labelLength        = (int) ByteManipulator.readBytesAsLong(buffer, 24, 4);
      initialState       = ByteManipulator.readBytesAsLong(buffer, 28, 8);
      nControllers       = (int) ByteManipulator.readBytesAsLong(buffer, 36, 4);
      int nEvents        = (int) ByteManipulator.readBytesAsLong(buffer, 40, 4);

      // None of the folowing things can exist if there are no events
      if (nEvents == 0)
        return;

      int nBadTransitions          = (int) ByteManipulator.readBytesAsLong(buffer, 44, 4);
      int nUnconditionalViolations = (int) ByteManipulator.readBytesAsLong(buffer, 48, 4);
      int nConditionalViolations   = (int) ByteManipulator.readBytesAsLong(buffer, 52, 4);
      int nPotentialCommunications = (int) ByteManipulator.readBytesAsLong(buffer, 56, 4);

        /* Read in the events */

      for (int e = 1; e <= nEvents; e++) {

        // Read properties
        buffer = new byte[nControllers * 2];
        headerRAFile.read(buffer);
        boolean[] observable = new boolean[nControllers];
        boolean[] controllable = new boolean[nControllers];
        for (int i = 0; i < nControllers; i++) {
          observable[i] = (buffer[2 * i] == 1);
          controllable[i] = (buffer[(2 * i) + 1] == 1);
        }

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

        // Create the event and add it to the list
        addEvent(new String(arr), observable, controllable);

      }

        /* Read in special transitions */

      if (nBadTransitions > 0)
        readTransitionDataFromHeader(nBadTransitions, badTransitions);
      
      if (nUnconditionalViolations > 0)
        readTransitionDataFromHeader(nUnconditionalViolations, unconditionalViolations);
      
      if (nConditionalViolations > 0)
        readTransitionDataFromHeader(nConditionalViolations, conditionalViolations);
      
      if (nPotentialCommunications > 0) {
        int nControllersBeforeUStructure = calculateNumberOfControllersBeforeUStructure();
        readCommunicationDataFromHeader(nPotentialCommunications, potentialCommunications, nControllersBeforeUStructure);
      }

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * A helper method to read a list of special transitions from the header file.
   * @param nTransitions  The number of transitions that need to be read
   * @param list          The list of transition data
   **/
  private void readTransitionDataFromHeader(int nTransitions, List<TransitionData> list) throws IOException {

    byte[] buffer = new byte[nTransitions * 20];
    headerRAFile.read(buffer);
    int index = 0;

    for (int i = 0; i < nTransitions; i++) {
      
      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      list.add(new TransitionData(initialStateID, eventID, targetStateID));
    
    }

  }

  /**
   * A helper method to read a list of potential communication transitions from the header file.
   * @param nCommunications The number of communications that need to be read
   * @param list            The list of communication data
   **/
  private void readCommunicationDataFromHeader(int nCommunications, List<CommunicationData> list, int nControllersBeforeUStructure) throws IOException {

    byte[] buffer = new byte[nCommunications * (20 + nControllersBeforeUStructure)];
    headerRAFile.read(buffer);
    int index = 0;

    for (int i = 0; i < nCommunications; i++) {

      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      CommunicationRole[] roles = new CommunicationRole[nControllersBeforeUStructure];
      for (int j = 0; j < roles.length; j++)
        roles[j] = CommunicationRole.getRole(buffer[index++]);
      
      list.add(new CommunicationData(initialStateID, eventID, targetStateID, roles));
    
    }

  }

  /**
   * Re-create the body file to accommodate some increase in capacity.
   * NOTE: This operation can clearly be expensive for large automata, so we need to try to reduce the number of times this method is called.
   * @param newEventCapacity      The number of events that the automaton will be able to hold
   * @param newStateCapacity      The number of states that the automaton will be able to hold
   * @param newTransitionCapacity The number of transitions that each state will be able to hold
   * @param newLabelLength        The maximum number of characters that each state label will be allowed
   * @param newNBytesPerStateID   The number of bytes that are now required to represent each state's ID
   * @param newNBytesPerEventID   The number of bytes that are now required to represent each event's ID
   **/
  private void recreateBodyFile(int newEventCapacity, long newStateCapacity, int newTransitionCapacity, int newLabelLength, int newNBytesPerEventID, int newNBytesPerStateID) {

    long newNBytesPerState = calculateNumberOfBytesPerState(newNBytesPerEventID, newNBytesPerStateID, newTransitionCapacity, newLabelLength);

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
      if (!state.writeToFile(newBodyRAFile, newNBytesPerState, newLabelLength, newNBytesPerEventID, newNBytesPerStateID)) {
        System.err.println("ERROR: Could not write copy over state to file. Aborting re-creation of .bdy file.");
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

    if (!newBodyFile.renameTo(new File(bodyFileName))) {
      System.out.println("CRUCIAL ERROR: Could not rename .bdy file during re-creation. Aborting program...");
      System.exit(-1);
    }
      /* Update variables */

    eventCapacity = newEventCapacity;
    stateCapacity = newStateCapacity;
    transitionCapacity = newTransitionCapacity;
    labelLength = newLabelLength;
    nBytesPerEventID = newNBytesPerEventID;
    nBytesPerStateID = newNBytesPerStateID;
    nBytesPerState = newNBytesPerState;

    bodyRAFile = newBodyRAFile;

  }

    /** MISCELLANEOUS **/

  /**
   * Calculate the amount of space required to store a state, given the specified conditions.
   * @param newNBytesPerEventID   The number of bytes per event ID
   * @param newNBytesPerStateID   The number of bytes per state ID
   * @param newTransitionCapacity The transition capacity
   * @param newLabelLength        The maximum label length
   * @return the number of bytes needed to store a state
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

  /**
   * Calculate and return the number of controllers before synchronized composition (which is related to the vector size of an event).
   * @return number of controllers prior to the creation of the U-Structure (or -1 if the events are not vectors)
   **/
  public int calculateNumberOfControllersBeforeUStructure() {

    // Grab a random event and get the vector size
    if (events.size() > 0)
      return events.iterator().next().vector.getSize() - 1; 
    
    return -1;

  }

    /** MUTATOR METHODS **/

  /**
   * Adds a transition based the label of the event (instead the ID).
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventLabel      The label of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   * @return the ID of the event label (returns 0 if the addition was unsuccessful)
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
   * NOTE: This method could be made more efficient since the entire state is written to file instead of only
   * writing the new transition to file.
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventID         The ID of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   **/
  public boolean addTransition(long startingStateID, int eventID, long targetStateID) {

      /* Create starting state from ID */

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

      /* Add transition and update the file */

    Event event = getEvent(eventID);
    startingState.addTransition(new Transition(event, targetStateID));
    if (!startingState.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
      System.err.println("ERROR: Could not add transition to file.");
      return false;
    }
    activeEvents.add(event);

    return true;

  }

  /**
   * Add the specified state to the automaton with an empty transition list.
   * @param label           The "name" of the new state
   * @param marked          Whether or not the states is marked
   * @param isInitialState  Whether or not this is the initial state
   * @return the ID of the added state (0 indicates the addition was unsuccessful)
   **/
  public long addState(String label, boolean marked, boolean isInitialState) {
    return addState(label, marked, new ArrayList<Transition>(), isInitialState);
  }

  /**
   * Add the specified state to the automaton.
   * @param label           The "name" of the new state
   * @param marked          Whether or not the states is marked
   * @param transitions     The list of transitions
   * @param isInitialState  Whether or not this is the initial state
   * @return the ID of the added state (0 indicates the addition was unsuccessful)
   **/
  public long addState(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState) {

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

      /* Check to see if we need to re-write the entire binary file */
    
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
    if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
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
   * Add the specified state to the automaton. NOTE: This method assumes that no state already exists with the specified ID.
   * The method renumberStates() must be called some time after using this method has been called since it can create empty
   * spots in the .bdy file where states don't actually exist (this happens during automata operations such as intersection).
   * @param label       The "name" of the new state
   * @param marked      Whether or not the states is marked
   * @param transitions   The list of transitions
   * @param isInitialState  Whether or not this is the initial state
   * @param id        The index where the state should be added at
   * @return whether or not the addition was successful (returns false if a state already existed there)
   **/
  public boolean addStateAt(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState, long id) {

      /* Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs) */
    
    if (id > MAX_STATE_CAPACITY) {
      System.err.println("ERROR: Could not write state to file.");
      return false;
    }

      /* Increase the maximum allowed characters per state label */
    
    if (label.length() > labelLength) {

      // If we cannot increase the capacity, indicate a failure
      if (label.length() > MAX_LABEL_LENGTH) {
        System.err.println("ERROR: Could not write state to file.");
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
        System.err.println("ERROR: Could not write state to file.");
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

      /* Check to see if we need to re-write the entire binary file */

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
    
    if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID)) {
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
   * NOTE: It is assumed that the new event is not already a member of the set (it is not checked for here for efficiency purposes).
   * @param label         The "name" of the new event
   * @param observable    Whether or not the event is observable
   * @param controllable  Whether or not the event is controllable
   * @return the ID of the added event (0 indicates failure)
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

      /* Ensure that no other event already exists with this label (if so, return the negative version of the ID) */

    // NOTE: This linear search is horribly inefficient. We could use a HashSet to hold the events, but many operations depend on it being ordered by ID (using TreeSet).
    // for (Event e : events)
    //   if (e.getLabel().equals(label))
    //     return -e.getID();

      /* Add the event */

    events.add(event);

      /* Update the header file */

    headerFileNeedsToBeWritten = true;

    return id;

  }

  /**
   * Add the specified event to the set if it does not already exist.
   * @param label         The "name" of the new event
   * @param observable    Whether or not the event is observable
   * @param controllable  Whether or not the event is controllable
   * @return the ID of the added event, negative ID indicates that the event already existed, and 0 indicates failure (due to reaching the maximum number of events)
   **/
  public int addEventIfNonExisting(String label, boolean[] observable, boolean[] controllable) {
    
    Event event = getEvent(label);

    if (event == null)
      return addEvent(label, observable, controllable);
    else
      return -event.getID();

  }


  /**
   * Add the entire set of events to the automaton.
   * @param label The set of events to add
   **/
  private void addAllEvents(Set<Event> newEvents) {

    for (Event e : newEvents)
      addEvent(e.getLabel(), e.isObservable(), e.isControllable());

  }

  /**
   * Add the entire set of events to the automaton (ensuring that no duplicates are added).
   * @param label The set of events to add
   **/
  private void addEventsIfNonExisting(Set<Event> newEvents) {

    for (Event e : newEvents)
      addEventIfNonExisting(e.getLabel(), e.isObservable(), e.isControllable());

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
   * Add an unconditional violation.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addUnconditionalViolation(long initialStateID, int eventID, long targetStateID) {

    unconditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Add a conditional violation.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addConditionalViolation(long initialStateID, int eventID, long targetStateID) {

    conditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Add a potential communication.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addPotentialCommunication(long initialStateID, int eventID, long targetStateID, CommunicationRole[] communicationRoles) {

    potentialCommunications.add(new CommunicationData(initialStateID, eventID, targetStateID, communicationRoles));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

    /** ACCESSOR METHODS **/

  /**
   * Check to see if a transition exists.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   * @return whether or not the transition exists
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
   * Check to see if a transition is bad.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   * @return whether or not the transition is bad
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
   * NOTE: This is a light-weight method which can be used instead of calling "getState(id) != null").
   * It does not load all of the state information, but only checks the first byte to see if it exists or not.
   * @param id  The unique identifier corresponding to the state we are looking for
   **/
  public boolean stateExists(long id) {
    return State.stateExists(this, bodyRAFile, id);
  }

  /**
   * Given the ID number of a state, get the state information.
   * @param id  The unique identifier corresponding to the requested state
   * @return the requested state
   **/
  public State getState(long id) {
    return State.readFromFile(this, bodyRAFile, id);
  }

  /**
   * Given the ID number of a state, get the state information (excluding transitions).
   * NOTE: This is a light-weight method which is used when accessing or modifying the transitions is not needed.
   * @param id  The unique identifier corresponding to the requested state
   * @return the requested state
   **/
  public State getStateExcludingTransitions(long id) {
    return State.readFromFileExcludingTransitions(this, bodyRAFile, id);
  }

  /**
   * Given the ID number of an event, get the event information.
   * @param id  The unique identifier corresponding to the requested event
   * @return the requested event (or null if it does not exist)
   **/
  public Event getEvent(int id) {

    for (Event e : events)
      if (e.getID() == id)
        return e;

    return null;

  }

  /**
   * Given the label of an event, get the event information.
   * @param label  The unique label corresponding to the requested event
   * @return the requested event (or null if it does not exist)
   **/
  public Event getEvent(String label) {

    for (Event e : events)
      if (e.getLabel().equals(label))
        return e;

    return null;

  }

  /**
   * Return the set of all events (in order by ID).
   * @return the set of all events
   **/
  public Set<Event> getEvents() {
    return events;
  }

  /**
   * Return the set of all active events.
   * @return the set of all active events
   **/
  public Set<Event> getActiveEvents() {
    return activeEvents;
  }

  /**
   * Get the number of events that can be held in this automaton.
   * @return current event capacity
   **/
  public int getEventCapacity() {
    return eventCapacity;
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
   * Get the amount of space needed to store an event ID.
   * @return number of bytes per event ID
   **/
  public int getSizeOfEventID() {
    return nBytesPerEventID;
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

  /**
   * Get the number of controllers in the automaton (>1 indicates decentralized control).
   * @return number of controllers
   **/
  public int getNumberOfControllers() {
    return nControllers;
  }

  /**
   * Get the list of potential communications.
   * @return potential communications
   **/
  public List<CommunicationData> getPotentialCommunications() {
    return potentialCommunications;
  }

}