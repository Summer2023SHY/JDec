/**
 * Automaton -  This extensive class is able to fully represent an automaton. The usage of .hdr and .bdy files
 *              gives the potential to work with very large automata, since the entire automaton does not need
 *              to be stored in memory.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Class Constants
 *  -Enum
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
import java.awt.image.*;
import java.net.*;
import javax.imageio.*;

public class Automaton {

    /** CLASS CONSTANTS **/

  private static final int HEADER_SIZE = 48; // This is the fixed amount of space needed to hold the main variables in the .hdr file

  public static final long DEFAULT_STATE_CAPACITY = 255;
  public static final long MAX_STATE_CAPACITY = Long.MAX_VALUE;
  public static final int DEFAULT_TRANSITION_CAPACITY = 1;
  public static final int MAX_TRANSITION_CAPACITY = Integer.MAX_VALUE;
  public static final int DEFAULT_LABEL_LENGTH = 1;
  public static final int MAX_LABEL_LENGTH = 100;
  public static final int DEFAULT_NUMBER_OF_CONTROLLERS = 1;
  public static final int MAX_NUMBER_OF_CONTROLLERS = 10;

  private static final String DEFAULT_HEADER_FILE_NAME = "temp.hdr",
                DEFAULT_BODY_FILE_NAME = "temp.bdy";
  private static final File   DEFAULT_HEADER_FILE = new File(DEFAULT_HEADER_FILE_NAME),
                DEFAULT_BODY_FILE = new File(DEFAULT_BODY_FILE_NAME);

    /** ENUM **/

  public static enum OutputMode {
    PNG,
    SVG
  }

    /** PRIVATE INSTANCE VARIABLES **/

  // Events
  private Set<Event>  events = new TreeSet<Event>(),
            activeEvents = new HashSet<Event>();

  // Basic properties of the automaton
  private long nStates = 0;
  private long initialState = 0;
  private int nControllers;
  
  // Variables which determine how large the .bdy file will become
  private long stateCapacity;
  private int transitionCapacity = 2;
  private int labelLength;

  // Initialized based on the above capacities
  private int nBytesPerStateID;
  private long nBytesPerState;

  // Special transitions (used by synchronized composition)
  private List<TransitionData> badTransitions = new ArrayList<TransitionData>();
  private List<TransitionData> unconditionalViolations = new ArrayList<TransitionData>();
  private List<TransitionData> conditionalViolations = new ArrayList<TransitionData>();

  
  // File variables
  private String  headerFileName = DEFAULT_HEADER_FILE_NAME,
          bodyFileName = DEFAULT_BODY_FILE_NAME;
  private File  headerFile,
          bodyFile;
  private RandomAccessFile  headerRAFile, // Contains basic information about automaton, needed in order to read the bodyFile, as well as the events
                bodyRAFile; // List each state in the automaton, with the transitions

  // GUI input
  private StringBuilder   eventInputBuilder,
              stateInputBuilder,
              transitionInputBuilder;

    /** CONSTRUCTORS **/

  /**
   * Default constructor: create empty automaton with default capacity, wiping any previous data existing in the files.
   **/
  public Automaton() {
    this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, DEFAULT_STATE_CAPACITY, DEFAULT_TRANSITION_CAPACITY, DEFAULT_LABEL_LENGTH, DEFAULT_NUMBER_OF_CONTROLLERS, true);
  }

  /**
   * Implicit constructor: create an automaton with a specified number of controllers.
   * @param headerFile  The file where the header should be stored
   * @param nControllers  The number of controllers that this automaton has
   **/
  public Automaton(File headerFile, int nControllers) {
    this(
        (headerFile == null) ? DEFAULT_HEADER_FILE : headerFile,
        (headerFile == null) ? DEFAULT_BODY_FILE : new File(headerFile.getName().substring(0, headerFile.getName().length() - 4) + ".bdy"),
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
        (headerFile == null) ? DEFAULT_BODY_FILE : new File(headerFile.getName().substring(0, headerFile.getName().length() - 4) + ".bdy"),
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
   * @param stateCapacity        The initial state capacity (increases by a factor of 256 when it is exceeded)
   *                             (NOTE: the initial state capacity may be higher than the value you give it, since it has to be in the form 256^x)
   * @param transitionCapacity   The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength          The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllers         The number of controllers that the automaton has (1 implies centralized control, >1 implies decentralized control)
   * @param clearFiles           Whether or not the header and body files should be cleared prior to use
   **/
  public Automaton(long stateCapacity, int transitionCapacity, int labelLength, int nControllers, boolean clearFiles) {
    this(DEFAULT_HEADER_FILE, DEFAULT_BODY_FILE, stateCapacity, transitionCapacity, labelLength, nControllers, clearFiles);
  }

  /**
   * Main constructor.
   * @param headerFile         The binary file to load the header information of the automaton from (information about events, etc.)
   * @param bodyFile           The binary file to load the body information of the automaton from (states and transitions)
   * @param stateCapacity      The initial state capacity (increases by a factor of 256 when it is exceeded)
   * @param transitionCapacity The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength        The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllers       The number of controllers that the automaton has (1 implies centralized control, >1 implies decentralized control)
   * @param clearFiles         Whether or not the header and body files should be cleared prior to use
   **/
  public Automaton(File headerFile, File bodyFile, long stateCapacity, int transitionCapacity, int labelLength, int nControllers, boolean clearFiles) {

    this.headerFile = headerFile;
    this.bodyFile = bodyFile;
    this.headerFileName = headerFile.getName();
    this.bodyFileName = bodyFile.getName();

      /* These variables will be overridden if we are loading information from file */

    this.stateCapacity = stateCapacity;
    this.transitionCapacity = transitionCapacity;
    this.labelLength = labelLength;
    this.nControllers = nControllers;

      /* Clear files */

    if (clearFiles)
      deleteFiles();
    
      /* Open files and try to load data from header */

    openFiles();

        /* Finish setting up */

      initializeVariables();
      nBytesPerState = calculateNumberOfBytesPerState(nBytesPerStateID, this.transitionCapacity, this.labelLength);

        /* Update header file */

    writeHeaderFile();

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

      /* Add special transitions if they still appear in the accessible part */

    copyOverSpecialTransitions(automaton);

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

      /* Return accessible automaton */

    return automaton;
  }

  /**
   * Create a new copy of this automaton that has all states removed which are unable to reach a marked state.
   * @return the co-accessible automaton
   **/
  public Automaton coaccessible() {

      /* Create a new automaton that has each of the transitions going the opposite direction */

    Automaton invertedAutomaton = new Automaton(stateCapacity, transitionCapacity, labelLength, nControllers, true);

    // Add events
    invertedAutomaton.addAllEvents(events);

    // Add states
    for (long s = 1; s <= nStates; s++) {

      State state = getStateExcludingTransitions(s);
      invertedAutomaton.addState(state.getLabel(), state.isMarked(), s == initialState);

    }

    // Add transitions
    for (long s = 1; s <= nStates; s++) {

      State state = getState(s);

      for (Transition t : state.getTransitions())
        invertedAutomaton.addTransition(t.getTargetStateID(), t.getEvent().getID(), s);

    }

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

      /* Return co-accessible automaton */

    return automaton;
  }

  /**
   * Helper method to copy over all special transition data from this automaton to another.
   * NOTE: The data is only copied over if both of the states involved in the transition actually exist
   * @param automaton The automaton in which the special transitions are being added
   **/
  private void copyOverSpecialTransitions(Automaton automaton) {

    for (TransitionData transitionData : badTransitions)
      if (automaton.stateExists(transitionData.initialStateID) && automaton.stateExists(transitionData.targetStateID))
        automaton.markTransitionAsBad(transitionData.initialStateID, transitionData.eventID, transitionData.targetStateID);

    for (TransitionData transitionData : unconditionalViolations)
      if (automaton.stateExists(transitionData.initialStateID) && automaton.stateExists(transitionData.targetStateID))
        automaton.addUnconditionalViolation(transitionData.initialStateID, transitionData.eventID, transitionData.targetStateID);

    for (TransitionData transitionData : conditionalViolations)
      if (automaton.stateExists(transitionData.initialStateID) && automaton.stateExists(transitionData.targetStateID))
        automaton.addConditionalViolation(transitionData.initialStateID, transitionData.eventID, transitionData.targetStateID);

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
        System.out.println("ERROR: Bad state ID.");
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
            long targetID = combineTwoIDs(t1.getTargetStateID(), first, t2.getTargetStateID(), second);
            automaton.addTransition(newStateID, t1.getEvent().getID(), targetID);

          }

    }

      /* Re-number states (by removing empty ones) */

    automaton.renumberStates();

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
    automaton.addAllEvents(second.getEvents());

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
      long newStateID = combineTwoIDs(id1, first, id2, second);

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
        System.out.println("ERROR: No starting state.");
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
          automaton.addEvent(combinedEventLabel, new boolean[] {true}, new boolean[] {true} );

          // Add state
          if (!automaton.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
            System.out.println("ERROR: Failed to add state. Synchronized composition aborted.");
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
              automaton.addEvent(combinedEventLabel, new boolean[] {true}, new boolean[] {true} );

              // Add state
              if (!automaton.addStateAt(combinedStateLabel, false, new ArrayList<Transition>(), false, combinedTargetID)) {
                System.out.println("ERROR: Failed to add state. Synchronized composition aborted.");
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

      /* Return produced automaton */

    // TEMPORARY
    automaton.generateLeastUpperBounds();
    automaton.generatePotentialCommunications();

    return automaton;

  }

  /**
   * TEMPORARY METHOD
   **/
  public void generateLeastUpperBounds() {

    Set<String> leastUpperBounds = new HashSet<String>();

    for (Event e1 : events)
      for (Event e2 : events) {

          /* Error checking */

        if (e1.getVectorSize() == -1 || e2.getVectorSize() == -1 || e1.getVectorSize() != e2.getVectorSize()) {
          System.out.println("ERROR: Bad event vectors. Least upper bounds generation aborted.");
          return;
        }

          /* Build least upper bound */

        boolean valid = true;
        String leastUpperBound = "";
        for (int i = 0; i < e1.getVectorSize(); i++) {

          String  label1 = e1.getLabelFromVector(i),
                  label2 = e2.getLabelFromVector(i);

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

          /* Add to the set */

        if (valid)
          leastUpperBounds.add("<" + leastUpperBound.substring(1) + ">");

      }

      System.out.println(leastUpperBounds);

  }

  /**
   * TEMPORARY METHOD
   **/
  public void generatePotentialCommunications() {

    List<Event> observableLabels = new ArrayList<Event>(),
                unobservableLabels = new ArrayList<Event>();
    for (Event e : events) {
      if (e.getLabelFromVector(0).equals("*"))
        unobservableLabels.add(e);
      else
        observableLabels.add(e);
    }

    Set<String> potentialCommunications = new HashSet<String>();

    for (Event e1 : observableLabels)
      for (Event e2 : unobservableLabels) {

          /* Error checking */

        if (e1.getVectorSize() == -1 || e2.getVectorSize() == -1 || e1.getVectorSize() != e2.getVectorSize()) {
          System.out.println("ERROR: Bad event vectors. Least upper bounds generation aborted.");
          return;
        }

          /* Build least upper bound */

        boolean valid = true;
        String potentialCommunication = "";
        for (int i = 0; i < e1.getVectorSize(); i++) {

          String  label1 = e1.getLabelFromVector(i),
                  label2 = e2.getLabelFromVector(i);

          // Check for incompatibility
          if (!label1.equals("*") && !label2.equals("*") && !label1.equals(label2)) {
            valid = false;
            break;
          }

          // Append vector element
          if (label1.equals("*"))
            potentialCommunication += "_" + label2;
          else
            potentialCommunication += "_" + label1;

        }

          /* Add to the set */

        if (valid)
          potentialCommunications.add("<" + potentialCommunication.substring(1) + ">");

      }

      System.out.println(potentialCommunications);

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
          if (!state.writeToFile(newBodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity))
            System.out.println("ERROR: Could not write state to file.");

        }

      }

        /* Update the special transitions in the header file */

      byte[] buffer = new byte[nBytesPerStateID];

      // Bad transitions
      for (TransitionData t : badTransitions) {

        // Update initialStateID
        mappingRAFile.seek(nBytesPerStateID * t.initialStateID);
        mappingRAFile.read(buffer);
        t.initialStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

        // Update targetStateID
        mappingRAFile.seek(nBytesPerStateID * t.targetStateID);
        mappingRAFile.read(buffer);
        t.targetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

      }

      // Unconditional violations
      for (TransitionData t : unconditionalViolations) {

        // Update initialStateID
        mappingRAFile.seek(nBytesPerStateID * t.initialStateID);
        mappingRAFile.read(buffer);
        t.initialStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

        // Update targetStateID
        mappingRAFile.seek(nBytesPerStateID * t.targetStateID);
        mappingRAFile.read(buffer);
        t.targetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

      }

      // Conditional violations
      for (TransitionData t : conditionalViolations) {

        // Update initialStateID
        mappingRAFile.seek(nBytesPerStateID * t.initialStateID);
        mappingRAFile.read(buffer);
        t.initialStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

        // Update targetStateID
        mappingRAFile.seek(nBytesPerStateID * t.targetStateID);
        mappingRAFile.read(buffer);
        t.targetStateID = ByteManipulator.readBytesAsLong(buffer, 0, nBytesPerStateID);

      }

        /* Remove old body file and mappings file */

      try {

        bodyRAFile.close();

          if (!bodyFile.delete())
                    System.out.println("ERROR: Could not delete old body file.");
                
                if (!mappingFile.delete())
                    System.out.println("ERROR: Could not delete mapping file.");

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

  /**
   * Given two state IDs and their respective automatons, create a unique combined ID.
   * NOTE: The reasoning behind this formula is analogous to the following: if you have a table with N rows and M columns,
   * every cell is guaranteed to have a different combination of row and column indexes.
   * @param id1   The state ID from the first automaton
   * @param first   The first automaton
   * @param id2   The state ID from the second automaton
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
   * @param maxID     The largest possible value to be used as an ID
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
   * @param size        The requested width and height in pixels
   * @param mode        The output type
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
        System.out.println("ERROR: GraphViz failed to generate image of graph.");
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

      State state = getState(s);

      if (state == null) {
        System.out.println("ERROR: State could not be loaded..");
        continue;
      }

      // Place '@' before label only if this is the initial state
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

        // Append special transition information
        String specialTransition = "";
        TransitionData transitionData = new TransitionData(s, t.getEvent().getID(), t.getTargetStateID());
        if (badTransitions.contains(transitionData))
          specialTransition += ",BAD";
        if (unconditionalViolations.contains(transitionData))
          specialTransition += ",UNCONDITIONAL_VIOLATION";
        if (conditionalViolations.contains(transitionData))
          specialTransition += ",CONDITIONAL_VIOLATION";
        if (!specialTransition.equals(""))
          transitionInputBuilder.append(":" + specialTransition.substring(1));
      
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
   * This is needed on Windows operating system because there are problems trying to delete files if they are in use.
   * NOTE: Do not attempt to use the automaton again unless the files are re-opened using openFiles().
   **/
  public void closeFiles() {

      try {

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
                System.out.println("ERROR: Could not delete header file.");
        
            if (!bodyFile.delete() && headerFile.exists())
                System.out.println("ERROR: Could not delete body file.");

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
  private void writeHeaderFile() {

      /* Write the header of the .hdr file */
    
    byte[] buffer = new byte[HEADER_SIZE];

    ByteManipulator.writeLongAsBytes(buffer, 0, nStates, 8);
    ByteManipulator.writeLongAsBytes(buffer, 8, stateCapacity, 8);
    ByteManipulator.writeLongAsBytes(buffer, 12, transitionCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 16, labelLength, 4);
    ByteManipulator.writeLongAsBytes(buffer, 20, initialState, 8);
    ByteManipulator.writeLongAsBytes(buffer, 28, nControllers, 4);
    ByteManipulator.writeLongAsBytes(buffer, 32, events.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 36, badTransitions.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 40, unconditionalViolations.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 44, conditionalViolations.size(), 4);

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

      writeSpecialTransitionsToHeader(badTransitions);
      writeSpecialTransitionsToHeader(unconditionalViolations);
      writeSpecialTransitionsToHeader(conditionalViolations);

    } catch (IOException e) {
            e.printStackTrace();
      } 

  }

  /**
   * A helper method to write a list of special transitions to the header file.
   * @param list  The list of transition data
   **/
  private void writeSpecialTransitionsToHeader(List<TransitionData> list) throws IOException {

    byte[] buffer = new byte[list.size() * 20];
    int index = 0;

    for (TransitionData t : list) {
      ByteManipulator.writeLongAsBytes(buffer, index, t.initialStateID, 8);
      ByteManipulator.writeLongAsBytes(buffer, index + 8, t.eventID, 4);
      ByteManipulator.writeLongAsBytes(buffer, index + 12, t.targetStateID, 8);
      index += 20;
    }

    headerRAFile.write(buffer);

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

      nStates = ByteManipulator.readBytesAsLong(buffer, 0, 8);
      stateCapacity = ByteManipulator.readBytesAsLong(buffer, 8, 8);
      transitionCapacity = (int) ByteManipulator.readBytesAsLong(buffer, 12, 4);
      labelLength = (int) ByteManipulator.readBytesAsLong(buffer, 16, 4);
      initialState = ByteManipulator.readBytesAsLong(buffer, 20, 8);
      nControllers = (int) ByteManipulator.readBytesAsLong(buffer, 28, 4);
      int nEvents = (int) ByteManipulator.readBytesAsLong(buffer, 32, 4);
      int nBadTransitions = (int) ByteManipulator.readBytesAsLong(buffer, 36, 4);
      int nUnconditionalViolations = (int) ByteManipulator.readBytesAsLong(buffer, 40, 4);
      int nConditionalViolations = (int) ByteManipulator.readBytesAsLong(buffer, 44, 4);

       /* Read in the events */

      for (int e = 1; e <= nEvents; e++) {

        // Read properties
        buffer = new byte[nControllers * 2];
        headerRAFile.read(buffer);
        boolean[]   observable = new boolean[nControllers],
              controllable = new boolean[nControllers];
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

      readSpecialTransitionsFromHeader(nBadTransitions, badTransitions);
      readSpecialTransitionsFromHeader(nUnconditionalViolations, unconditionalViolations);
      readSpecialTransitionsFromHeader(nConditionalViolations, conditionalViolations);

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * A helper method to read a list of special transitions from the header file.
   * @param nTransitions  The number of transitions that need to be read
   * @param list          The list of transition data
   **/
  private void readSpecialTransitionsFromHeader(int nTransitions, List<TransitionData> list) throws IOException {

    byte[] buffer = new byte[nTransitions * 20];
    headerRAFile.read(buffer);
    int index = 0;

    for (int t = 0; t < nTransitions; t++) {
      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index + 8, 4);
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index + 12, 8);
      list.add(new TransitionData(initialStateID, eventID, targetStateID));
      index += 20;
    }

  }

  /**
   * Re-create the body file to accommodate some increase in capacity.
   * NOTE: This operation can clearly be expensive for large automata, so we need to try to reduce the number of times this method is called.
   * @param newStateCapacity    The number of states that the automaton will be able to hold
   * @param newTransitionCapacity The number of transitions that each state will be able to hold
   * @param newLabelLength    The maximum number of characters that each state label will be allowed
   * @param newNBytesPerStateID The number of bytes that are now required to represent each state ID
   **/
  private void recreateBodyFile(long newStateCapacity, int newTransitionCapacity, int newLabelLength, int newNBytesPerStateID) {

    long newNBytesPerState = calculateNumberOfBytesPerState(newNBytesPerStateID, newTransitionCapacity, newLabelLength);

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
   * @param newNBytesPerStateID The number of bytes per state ID
   * @param newTransitionCapacity The transition capacity
   * @param newLabelLength    The maximum label length
   * @return the number of bytes needed to store a state
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

      /* Calculate the maximum number of states that we can have before we have to allocate more space for each state ID */

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

  }

    /** MUTATOR METHODS **/

  /**
   * Adds a transition based th label of the event (instead the ID).
   * @param startingStateID The ID of the state where the transition originates from
   * @param eventLabel    The label of the event that triggers the transition
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
   * @param eventID     The ID of the event that triggers the transition
   * @param targetStateID   The ID of the state where the transition leads to
   **/
  public boolean addTransition(long startingStateID, int eventID, long targetStateID) {

      /* Create starting state from ID */

    State startingState  = getState(startingStateID);

    if (startingState == null) {
      System.out.println("ERROR: Could not add transition to file (starting state does not exist).");
      return false;
    }

      /* Increase the maximum allowed transitions per state */

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
          nBytesPerStateID
        );

      // Update header file
      writeHeaderFile();

    }

      /* Add transition and update the file */

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
   * Add the specified state to the automaton with an empty transition list.
   * @param label       The "name" of the new state
   * @param marked      Whether or not the states is marked
   * @param isInitialState  Whether or not this is the initial state
   * @return the ID of the added state (0 indicates the addition was unsuccessful)
   **/
  public long addState(String label, boolean marked, boolean isInitialState) {
    return addState(label, marked, new ArrayList<Transition>(), isInitialState);
  }

  /**
   * Add the specified state to the automaton.
   * @param label       The "name" of the new state
   * @param marked      Whether or not the states is marked
   * @param transitions   The list of transitions
   * @param isInitialState  Whether or not this is the initial state
   * @return the ID of the added state (0 indicates the addition was unsuccessful)
   **/
  public long addState(String label, boolean marked, ArrayList<Transition> transitions, boolean isInitialState) {

      /* Ensure that we haven't already reached the limit (NOTE: This will likely never be the case since we are using longs) */

    if (nStates == MAX_STATE_CAPACITY) {
      System.out.println("ERROR: Could not write state to file.");
      return 0;
    }

      /* Increase the maximum allowed characters per state label */

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
          nBytesPerStateID
        );

    }

      /* Increase the maximum allowed transitions per state */
    
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
          nBytesPerStateID
        );

    }

      /* Check to see if we need to re-write the entire binary file */
    
    if (nStates == stateCapacity) {

      // Re-create binary file
      recreateBodyFile(
          ((stateCapacity + 1) << 8) - 1,
          transitionCapacity,
          labelLength,
          nBytesPerStateID + 1
        );

    }

    long id = ++nStates;

      /* Write new state to file */
    
    State state = new State(label, id, marked, transitions);
    if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity)) {
      System.out.println("ERROR: Could not write state to file.");
      return 0;
    }

      /* Change initial state */
    
    if (isInitialState)
      initialState = id;

      /* Update header file */
    
    writeHeaderFile();

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
      System.out.println("ERROR: Could not write state to file.");
      return false;
    }

      /* Increase the maximum allowed characters per state label */
    
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
          nBytesPerStateID
        );

    }

      /* Increase the maximum allowed transitions per state */

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
          newStateCapacity,
          transitionCapacity,
          labelLength,
          newNBytesPerStateID
        );

    }

      /* Write new state to file */
    
    State state = new State(label, id, marked, transitions);
    
    if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerStateID, transitionCapacity)) {
      System.out.println("ERROR: Could not write state to file.");
      return false;
    }

    nStates++;

      /* Update initial state */
    
    if (isInitialState)
      initialState = id;

      /* Update header file */
    
    writeHeaderFile();

    return true;
  }

  /**
   * Add the specified event to the set.
   * @param label     The "name" of the new event
   * @param observable  Whether or not the event is observable
   * @param controllable  Whether or not the event is controllable
   * @return the ID of the added event (0 indicates the addition was unsuccessful, which implies that the set did not change in size)
   **/
  public int addEvent(String label, boolean[] observable, boolean[] controllable) {

    // Ensure that no other event already exists with this label (this is necessary because of the strange comparison criteria in Event.compareTo())
    for (Event e : events)
      if (e.getLabel().equals(label))
        return 0; 

    // Create and add the event
    int id = events.size() + 1;
    Event event = new Event(label, id, observable, controllable);
    events.add(event);

    // Update header file
    writeHeaderFile();

    return id;

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
   * Mark the specified as being "bad", which is used in synchronized composition.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void markTransitionAsBad(long initialStateID, int eventID, long targetStateID) {

    badTransitions.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    writeHeaderFile();

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
    writeHeaderFile();

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
    writeHeaderFile();

  }

    /** ACCESSOR METHODS **/

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

  /**
   * Get the the number of controllers in the automaton (>1 indicates decentralized control).
   * @return number of controllers
   **/
  public int getNumberOfControllers() {
    return nControllers;
  }

  /**
   * Private class to hold all 3 pieces of information needed to identify a transition.
   **/
  private class TransitionData {

    public long initialStateID, targetStateID;
    public int eventID;

    public TransitionData(long initialStateID, int eventID, long targetStateID) {
        this.initialStateID = initialStateID;
        this.eventID = eventID;
        this.targetStateID = targetStateID;
    }

    /**
     * Check for equality by comparing properties.
     * @param obj - The object to compare this one to
     * @return whether or not the transitions are equal
     **/
    @Override public boolean equals(Object obj) {

      TransitionData other = (TransitionData) obj;

      return initialStateID == other.initialStateID
        && eventID == other.eventID
        && targetStateID == other.targetStateID;

    }

  } // TransitionData

}
