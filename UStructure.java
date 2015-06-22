import java.util.*;
import java.io.*;
import java.nio.file.*;

public class UStructure extends Automaton {

  private static final int HEADER_SIZE = 72; // This is the fixed amount of space needed to hold the main variables in the .hdr file

  // Special transitions
  private List<TransitionData> unconditionalViolations;
  private List<TransitionData> conditionalViolations;
  private List<CommunicationData> potentialCommunications;
  private List<TransitionData> nonPotentialCommunications;

  private int nControllersBeforeUStructure;

  /**
   * Implicit constructor: used to load automaton from file.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   **/
  public UStructure(File headerFile, File bodyFile) {
    this(
      headerFile,
      bodyFile,
      -1, // This value will be overwritten when the header file is read
      false
    );
  }

  /**
   * Implicit constructor: used when creating a new U-Structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   **/
  public UStructure(File headerFile, File bodyFile, int nControllersBeforeUStructure) {
    this(
      headerFile,
      bodyFile,
      nControllersBeforeUStructure,
      true
    );
  }

  /**
   * Implicit constructor: used to load automaton from file or when creating a new U-Structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   * @param clearFiles                    Whether or not the header and body files should be wiped before use
   **/
  public UStructure(File headerFile, File bodyFile, int nControllersBeforeUStructure, boolean clearFiles) {
    this(
      headerFile,
      bodyFile,
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      nControllersBeforeUStructure,
      clearFiles
    );
  }
	
	/**
   * Main constructor.
   * @param headerFile                    The binary file to load the header information of the automaton from (information about events, etc.)
   * @param bodyFile                      The binary file to load the body information of the automaton from (states and transitions)
   * @param eventCapacity                 The initial event capacity (increases by a factor of 256 when it is exceeded)
   * @param stateCapacity                 The initial state capacity (increases by a factor of 256 when it is exceeded)
   * @param transitionCapacity            The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength                   The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   * @param clearFiles                    Whether or not the header and body files should be cleared prior to use
   **/
  public UStructure(File headerFile, File bodyFile, int eventCapacity, long stateCapacity, int transitionCapacity, int labelLength, int nControllersBeforeUStructure, boolean clearFiles) {
    
    super(headerFile, bodyFile, eventCapacity, stateCapacity, transitionCapacity, labelLength, 1, clearFiles);
    
    // This variable is only reset if this is not being read from file
    if (nControllersBeforeUStructure != -1)
      this.nControllersBeforeUStructure = nControllersBeforeUStructure;
    
    automatonType = 1;
    headerFileNeedsToBeWritten = true;

	}

  @Override public UStructure accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new UStructure(newHeaderFile, newBodyFile, nControllersBeforeUStructure));
  }

  @Override public UStructure coaccessible(File newHeaderFile, File newBodyFile) {
    return coaccessibleHelper(new UStructure(newHeaderFile, newBodyFile, nControllersBeforeUStructure), invert());
  }

  @Override public UStructure complement(File newHeaderFile, File newBodyFile) {

    UStructure uStructure = new UStructure(
      newHeaderFile,
      newBodyFile,
      eventCapacity,
      stateCapacity,
      events.size(), // This is the new number of transitions that will be required for each state
      labelLength,
      nControllersBeforeUStructure,
      true
    );

    return complementHelper(uStructure);

  }

  @Override protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

    super.copyOverSpecialTransitions(automaton);

    UStructure uStructure = (UStructure) automaton;

    if (unconditionalViolations != null)
      for (TransitionData data : unconditionalViolations)
        if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
          uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
    
    if (conditionalViolations != null)  
      for (TransitionData data : conditionalViolations)
        if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
          uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

    if (potentialCommunications != null)  
      for (CommunicationData data : potentialCommunications)
        if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
          uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, (CommunicationRole[]) data.roles.clone());

    if (nonPotentialCommunications != null)  
      for (TransitionData data : nonPotentialCommunications)
        if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
          uStructure.addNonPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID);

  }

  @Override protected UStructure invert() {
    return invertHelper(new UStructure(null, null, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllersBeforeUStructure, true));
  }

  /**
   * Generate a new automaton, with all communications added (potential communications are marked).
   * @param newHeaderFile          The header file where the new automaton should be stored
   * @param newBodyFile            The body file where the new automaton should be stored
   * @return                       The automaton with the added transitions
   * @throws NoUStructureException If the automaton is not a U-Structure (which is required for this operation)
   **/
  public UStructure addCommunications(File newHeaderFile, File newBodyFile) throws NoUStructureException {
    
      /* Setup */

    // Generate all potential
    Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
    for (Event e : events)
      leastUpperBounds.add(new LabelVector(e.getLabel()));
    Set<CommunicationLabelVector> potentialCommunications = findPotentialCommunicationLabels(leastUpperBounds);
    
    // Generate all least upper bounds
    generateLeastUpperBounds(leastUpperBounds);
    
    UStructure automaton = duplicate(newHeaderFile, newBodyFile);

      /* Add communications (marking the potential communications) */

    for (long s = 1; s < automaton.getNumberOfStates(); s++) {

      State startingState = automaton.getState(s);

      // Try each least upper bound
      for (LabelVector vector : leastUpperBounds) {
        
        boolean[] vectorElementsFound = new boolean[vector.getSize()];
        State destinationState = automaton.findWhereCommunicationLeads(vector, vectorElementsFound, startingState);
        
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
            boolean found = false;
            for (CommunicationLabelVector data : potentialCommunications)
              if (vector.equals((LabelVector) data)) {
                automaton.addPotentialCommunication(startingState.getID(), id, destinationState.getID(), data.roles);
                found = true;
              }

            // If there were no potential communications, then it must be a non-potential communication
            if (!found)
              automaton.addNonPotentialCommunication(startingState.getID(), id, destinationState.getID());
    
          }
         
        }

      }

    }

      /* Copy over all of the special transitions */

    copyOverSpecialTransitions(automaton);

      /* Ensure that the header file has been written to disk */

    automaton.writeHeaderFile();

    return automaton;
    
  }

  /**
   * Using recursion, starting at a given state, determine which state the specified communication leads to (if it exists).
   * @param communication       The event vector representing the communication
   * @param vectorElementsFound Indicates which elements of the vector have been found
   * @param currentState        The state that we are currently on
   * @return                    The destination state (or null if the communication does not lead to a state)
   **/
  private State findWhereCommunicationLeads(LabelVector communication, boolean[] vectorElementsFound, State currentState) {

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
      State destinationState = findWhereCommunicationLeads(communication, copy, getState(t.getTargetStateID()));
      
      // Return destination if it is found (there will only ever be one destination for a given communication from a given state, so we can stop as soon as we find it the first time)
      if (destinationState != null)
        return destinationState;

    }

    return null;

  }

  /**
   * Given the complete set of least upper bounds (LUBs), return the subset of LUBs which are the event vectors for potential communications.
   * @param leastUpperBounds        The set of LUBs
   * @return                        The set of potential communications, including communication roles
   * @throws NoUStructureException  If the automaton is not a U-Structure (which is required for this operation)
   **/
  private Set<CommunicationLabelVector> findPotentialCommunicationLabels(Set<LabelVector> leastUpperBounds) throws NoUStructureException {

      /* Separate observable and unobservable labels */

    Set<LabelVector> observableLabels = new HashSet<LabelVector>();
    Set<LabelVector> unobservableLabels = new HashSet<LabelVector>();

    for (LabelVector v : leastUpperBounds) {
      if (v.getSize() == -1)
        throw new NoUStructureException();
      if (v.getLabelAtIndex(0).equals("*"))
        unobservableLabels.add(v);
      else
        observableLabels.add(v);
    }

    // Find all LUB's of the unobservable labels (which will add communications where there is more than one reciever)
    generateLeastUpperBounds(unobservableLabels);

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
   * Expand the specified set of event vectors to include all possible least upper bounds (LUBs).
   * @param leastUpperBounds  The set of all LUBs in the form of event vectors 
   **/
  private void generateLeastUpperBounds(Set<LabelVector> leastUpperBounds) {

      /* Continue to find LUBs using pairs of event vectors until there are no new ones left to find */

    boolean foundNew = true;
    while (foundNew) {

      List<LabelVector> temporaryList = new ArrayList<LabelVector>();
      
      // Try all pairs
      for (LabelVector v1 : leastUpperBounds) {
        for (LabelVector v2 : leastUpperBounds) {

            /* Error checking */

          if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
            System.err.println("ERROR: Bad event vectors. Pair of label vectors skipped.");
            continue;
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

  }

  /**
   * Checking the feasibility for all possible communication protocols, generate a list of the feasible protocols.
   * @param communications  The communications to be considered (which should be a subset of the potentialCommunications list of this automaton)
   * @return                The feasible protocols, which are sorted by the number of communications that each protocol has (smallest to largest)
   **/
  public List<Set<CommunicationData>> generateAllFeasibleProtocols(List<CommunicationData> communications) {

      /* Generate powerset of communication protocols */

    List<Set<CommunicationData>> protocols = new ArrayList<Set<CommunicationData>>();
    powerSet(protocols, communications, new HashSet<CommunicationData>(), 0);

      /* Create inverted automaton, so that we can explore the automaton by crossing transitions from either direction */

    Automaton invertedAutomaton = invert();

      /* Generate list of feasible protocols */

    List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
    for (Set<CommunicationData> protocol : protocols) {

      // Ignore the protocol with no communications (doesn't make sense in our context)
      if (protocol.size() == 0)
        continue;

      if (isFeasibleProtocol(protocol, invertedAutomaton))
        feasibleProtocols.add(protocol);

    }

      /* Sort sets by size (so that protocols with fewer communications appear first) */

    Collections.sort(feasibleProtocols, new Comparator<Set<?>>() {
        @Override public int compare(Set<?> set1, Set<?> set2) {
          return Integer.valueOf(set1.size()).compareTo(set2.size());
        }
      }
    );

    return feasibleProtocols;

  }

  /**
   * Generate a list of the smallest possible feasible protocols (in terms of the number of communications).
   * @param communications  The communications to be considered (which should be a subset of the potentialCommunications list of this automaton)
   * @return                The feasible protocols
   **/
  public List<Set<CommunicationData>> generateSmallestFeasibleProtocols(List<CommunicationData> communications) {

      /* Create inverted automaton, so that we can explore the automaton by crossing transitions from either direction */

    Automaton invertedAutomaton = invert();

      /* Generate list of feasible protocols */

    List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
    int sizeOfSmallestProtocol = -1;

    // Each communication only needs to appear once in order to generate the smallest feasible protocols
    Set<CommunicationData> communicationsToSkip = new HashSet<CommunicationData>();
    
    for (CommunicationData data : communications) {

      // Skip if this communication has already been seen before (prevents the generation of duplicate protocols)
      if (communicationsToSkip.contains(data))
        continue;

      // Create a protocol uisng only this communication
      Set<CommunicationData> protocol = new HashSet<CommunicationData>();
      protocol.add(data);

      // Make this protocol feasible
      Set<CommunicationData> feasibleProtocol = makeProtocolFeasible(protocol, invertedAutomaton);

      // Add each communication in the feasible protocol to list of communications to skip
      communicationsToSkip.addAll(feasibleProtocol);

      // Add it to the list if it is tied as the smallest feasible protocol so far
      if (sizeOfSmallestProtocol == feasibleProtocol.size())
        feasibleProtocols.add(feasibleProtocol);

      // Clear the list if this is the new smallest feasible protocol
      else if (sizeOfSmallestProtocol == -1 || feasibleProtocol.size() < sizeOfSmallestProtocol) {
        feasibleProtocols.clear();
        sizeOfSmallestProtocol = feasibleProtocol.size();
        feasibleProtocols.add(feasibleProtocol);
      }

    }

    return feasibleProtocols;

  }

  /**
   * Make the specified protocol feasible (returning it as a new set).
   * NOTE: This method is overloaded for efficiency purposes (the method accepting an inverted Automaton as a parameter is more
   * efficient if makeProtocolFeasible() is being called multiple times on the same automaton, so there's no need to regenerate
   * the inverted automaton each time).
   * @param protocol  The protocol that is being made feasible
   * @return          The feasible protocol
   **/
  public Set<CommunicationData> makeProtocolFeasible(Set<CommunicationData> protocol) {
    return makeProtocolFeasible(protocol, invert());
  }

  /**
   * Make the specified protocol feasible (returning it as a new set).
   * @param protocol            The protocol that is being made feasible
   * @param invertedAutomaton   An automaton identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @return                    The feasible protocol
   **/
  private Set<CommunicationData> makeProtocolFeasible(Set<CommunicationData> protocol, Automaton invertedAutomaton) {

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

    return feasibleProtocol;

  }


  /**
   * Check to see if the specified protocol is feasible.
   * @param protocol            The protocol that is being checked for feasibility
   * @param invertedAutomaton   An automaton identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @return                    Whether or not the protocol is feasible
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
   * Refine this automaton by applying the specified feasible communication protocol, and doing the necessary pruning.
   * @param protocol      The chosen protocol (which must be feasible)
   * @param newHeaderFile The header file where the new automaton should be stored
   * @param newBodyFile   The body file where the new automaton should be stored
   * @return              The pruned automaton that had the specified protocol applied
   **/
  public UStructure applyFeasibleProtocol(Set<CommunicationData> protocol, File newHeaderFile, File newBodyFile) {

    UStructure automaton = duplicate(getTemporaryFile(), getTemporaryFile());
    // UStructure invertedAutomaton = invert();

      /* Remove all communications that are not part of the protocol */

    for (TransitionData data : nonPotentialCommunications)
      automaton.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

    Set<CommunicationData> potentialCommunicationsToRemove = new HashSet<CommunicationData>(potentialCommunications);
    potentialCommunicationsToRemove.removeAll(protocol);
    
    for (CommunicationData data : potentialCommunicationsToRemove)
      automaton.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

      /* Prune (which removes more transitions) */

    // CommunicationData initialData = null;

    for (CommunicationData data : protocol) {

      // if (initialData == null)
      //   initialData = data;

      // // Ensure that this state is still indistinguishable
      // else {

      //   // Find reachable states
      //   Set<Long> reachableStates = new HashSet<Long>();
      //   findReachableStates(automaton, invertedAutomaton, reachableStates, initialData.initialStateID, data.getIndexOfSender() + 1);
      //   if (!reachableStates.contains(initialData.initialStateID))
      //     continue;

      // }

      LabelVector vector = getEvent(data.eventID).vector;
      boolean[] vectorElementsFound = new boolean[vector.getSize()];

      automaton.prune(protocol, vector, vectorElementsFound, getState(data.initialStateID), 0);

    }

      /* Get the accessible part of the automaton */

    automaton = automaton.accessible(newHeaderFile, newBodyFile);

      /* Remove all inactive events */

    automaton.removeInactiveEvents();

      /* Write header file */

    automaton.writeHeaderFile();

    return automaton;

  }

  /**
   * Using recursion, starting at a given state, prune away all necessary transitions.
   * @param protocol                      The chosen protocol (which must be feasible)
   * @param communication                 The event vector representing the chosen communication
   * @param vectorElementsFound           Indicates which elements of the vector have already been found
   * @param currentState                  The state that we are currently on
   * @param depth                         The current depth of the recursion (first iteration has a depth of 0)
   **/
  private void prune(Set<CommunicationData> protocol, LabelVector communication, boolean[] vectorElementsFound, State currentState, int depth) {

      /* Base case */

    if (depth == nControllersBeforeUStructure)
      return;

      /* Recursive case */

    // Try all transitions leading from this state
    outer: for (Transition t : currentState.getTransitions()) {

      // We do not want to prune any of the chosen communications
      if (depth == 0)
        for (CommunicationData data : protocol)
          if (currentState.getID() == data.initialStateID && t.getEvent().getID() == data.eventID && t.getTargetStateID() == data.targetStateID)
            continue outer;

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

      // Prune this transition
      removeTransition(currentState.getID(), t.getEvent().getID(), t.getTargetStateID());

      // Recursive call to the state where this transition leads
      prune(protocol, communication, copy, getState(t.getTargetStateID()), depth + 1);

    }

  }

  /**
   * Remove a special transition, given its transition data.
   * @param data  The transition data associated with the special transitions to be removed
   **/
  @Override protected void removeTransitionData(TransitionData data) {

    super.removeTransitionData(data);
    
    if (unconditionalViolations != null)
      unconditionalViolations.remove(data);

    if (conditionalViolations != null)
      conditionalViolations.remove(data);
    
    if (potentialCommunications != null)
      while (potentialCommunications.remove(data)); // Multiple potential communications could exist for the same transition (more than one potential sender)
    
    if (nonPotentialCommunications != null)
      nonPotentialCommunications.remove(data);

  }

  /**
   * Remove all events which are inactive (meaning that they do not appear in a transition)
   **/
  private void removeInactiveEvents() { 

      /* Determine which events are active */

    boolean[] active = new boolean[events.size() + 1];
    for (long s = 1; s <= nStates; s++) 
      for (Transition t : getState(s).getTransitions())
        active[t.getEvent().getID()] = true;
    
      /* Remove the inactive events */
    
    Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
    int maxID = events.size();
    int newID = 1;
    for (int id = 1; id <= maxID; id++) {
      if (!active[id]) {
        if (!removeEvent(id))
          System.err.println("ERROR: Failed to remove inactive event.");
      } else
        mapping.put(id, newID++);
    }

      /* Re-number event IDs */

    // Update event IDs in body file
    for (long s = 1; s <= nStates; s++) {
      
      State state = getState(s);
      
      // Update the event ID in the transitions
      for (Transition t : state.getTransitions()) {
        Event e = t.getEvent();
        t.setEvent(new Event(e.getLabel(), mapping.get(e.getID()), e.isObservable(), e.isControllable()));
      }

      // Write updated state to file
      if (!state.writeToFile(bodyRAFile, nBytesPerState, labelLength, nBytesPerEventID, nBytesPerStateID))
        System.err.println("ERROR: Could not write state to file.");

    }

    // Update event IDs in header file
    for (Event e : events)
      e.setID(mapping.get(e.getID()));
    renumberEventsInTransitionData(mapping, badTransitions);
    renumberEventsInTransitionData(mapping, unconditionalViolations);
    renumberEventsInTransitionData(mapping, conditionalViolations);
    renumberEventsInTransitionData(mapping, potentialCommunications);
    renumberEventsInTransitionData(mapping, nonPotentialCommunications);

      /* Indicate that the header file needs to be updated */
    
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Remove the event with the specified ID.
   * NOTE: After calling this method, the events must be re-numbered, otherwise there will be complications.
   * @param id  The event's ID
   * @return    Whether or not the event was successfully removed
   **/
  private boolean removeEvent(int id) {

    Iterator iterator = events.iterator();

    while (iterator.hasNext()) {

      Event e = (Event) iterator.next();

      // Remove the event if the ID matches
      if (e.getID() == id) {
        
        iterator.remove();

        // Indicate that the header file needs to be updated
        headerFileNeedsToBeWritten = true;

        return true;

      }

    }

    return false;

  }

  /**
   * Helper method to re-number event IDs in the specified list of special transitions.
   * @param mapping The binary file containing the state ID mappings
   * @param list    The list of special transition data
   **/
  private void renumberEventsInTransitionData(Map<Integer, Integer> mapping, List<? extends TransitionData> list) {

    if (list != null)
      for (TransitionData data : list)
        data.eventID = mapping.get((Integer) data.eventID);

  }

  /**
   * Add an unconditional violation.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addUnconditionalViolation(long initialStateID, int eventID, long targetStateID) {

    if (unconditionalViolations == null)
      unconditionalViolations = new ArrayList<TransitionData>();

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

    if (conditionalViolations == null)
      conditionalViolations = new ArrayList<TransitionData>();

    conditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Add a potential communication.
   * @param initialStateID      The initial state
   * @param eventID             The event triggering the transition
   * @param targetStateID       The target state
   * @param communicationRoles  The roles associated with each controller
   **/
  public void addPotentialCommunication(long initialStateID, int eventID, long targetStateID, CommunicationRole[] communicationRoles) {

    if (potentialCommunications == null)
      potentialCommunications = new ArrayList<CommunicationData>();

    potentialCommunications.add(new CommunicationData(initialStateID, eventID, targetStateID, communicationRoles));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Add a non-potential communication (which are the communications that were added for mathmatical completeness but are not actually potential communications).
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addNonPotentialCommunication(long initialStateID, int eventID, long targetStateID) {

    if (nonPotentialCommunications == null)
      nonPotentialCommunications = new ArrayList<TransitionData>();

    nonPotentialCommunications.add(new TransitionData(initialStateID, eventID, targetStateID));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Get the list of potential communications.
   * @return  The potential communications
   **/
  public List<CommunicationData> getPotentialCommunications() {
    return potentialCommunications;
  }

  /**
   * Duplicate this automaton and store it in a different set of files.
   * @param newHeaderFile The new header file where the automaton is being copied to
   * @param newBodyFile   The new body file where the automaton is being copied to
   * @return              The duplicated automaton
   **/
  public UStructure duplicate(File newHeaderFile, File newBodyFile) {

    // Assign temporary files, if necessary
    if (newHeaderFile == null)
      newHeaderFile = getTemporaryFile();
    if (newBodyFile == null)
      newBodyFile = getTemporaryFile();

    // Copy the header and body files
    try {
    
      if (headerFile.exists())
        Files.copy(headerFile.toPath(), newHeaderFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      
      if (bodyFile.exists())
        Files.copy(bodyFile.toPath(), newBodyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return new UStructure(newHeaderFile, newBodyFile);

  }

  @Override protected String getInputCodeForSpecialTransitions(TransitionData transitionData) {

    String str = "";

    if (unconditionalViolations != null && unconditionalViolations.contains(transitionData))
      str += ",UNCONDITIONAL_VIOLATION";
    
    if (conditionalViolations != null && conditionalViolations.contains(transitionData))
      str += ",CONDITIONAL_VIOLATION";
    
    // Search entire list since there may be more than one potential communication
    if (potentialCommunications != null)
      for (CommunicationData communicationData : potentialCommunications) {
        if (transitionData.equals(communicationData)) {
          str += ",POTENTIAL_COMMUNICATION-";
          for (CommunicationRole role : communicationData.roles)
            str += role.getCharacter();
        }
      }

    if (nonPotentialCommunications != null && nonPotentialCommunications.contains(transitionData))
      str += ",COMMUNICATION";

    return super.getInputCodeForSpecialTransitions(transitionData) + str;

  }

  /**
   * Read all of the header information from file.
   **/
  @Override protected void readHeaderFile() {

    byte[] buffer = new byte[HEADER_SIZE];

    try {

        /* Do not try to load an empty file */

      if (headerRAFile.length() == 0)
        return;

        /* Go to the proper position and read in the bytes */

      headerRAFile.seek(0);
      headerRAFile.read(buffer);

        /* Calculate the values stored in these bytes */

      automatonType                = (int) ByteManipulator.readBytesAsLong(buffer, 0,  4);
      nStates                      =       ByteManipulator.readBytesAsLong(buffer, 4,  8);
      eventCapacity                = (int) ByteManipulator.readBytesAsLong(buffer, 12, 4);
      stateCapacity                =       ByteManipulator.readBytesAsLong(buffer, 16, 8);
      transitionCapacity           = (int) ByteManipulator.readBytesAsLong(buffer, 24, 4);
      labelLength                  = (int) ByteManipulator.readBytesAsLong(buffer, 28, 4);
      initialState                 =       ByteManipulator.readBytesAsLong(buffer, 32, 8);
      nControllers                 = (int) ByteManipulator.readBytesAsLong(buffer, 40, 4);
      nControllersBeforeUStructure = (int) ByteManipulator.readBytesAsLong(buffer, 44, 4);
      int nEvents                  = (int) ByteManipulator.readBytesAsLong(buffer, 48, 4);

      // None of the folowing things can exist if there are no events
      if (nEvents == 0)
        return;

      int nBadTransitions             = (int) ByteManipulator.readBytesAsLong(buffer, 52, 4);
      int nUnconditionalViolations    = (int) ByteManipulator.readBytesAsLong(buffer, 56, 4);
      int nConditionalViolations      = (int) ByteManipulator.readBytesAsLong(buffer, 60, 4);
      int nPotentialCommunications    = (int) ByteManipulator.readBytesAsLong(buffer, 64, 4);
      int nNonPotentialCommunications = (int) ByteManipulator.readBytesAsLong(buffer, 68, 4);

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

      if (nBadTransitions > 0) {
        badTransitions = new ArrayList<TransitionData>();
        readTransitionDataFromHeader(nBadTransitions, badTransitions);
      }
      
      if (nUnconditionalViolations > 0) {
        unconditionalViolations = new ArrayList<TransitionData>();
        readTransitionDataFromHeader(nUnconditionalViolations, unconditionalViolations);
      }
      
      if (nConditionalViolations > 0) {
        conditionalViolations = new ArrayList<TransitionData>();
        readTransitionDataFromHeader(nConditionalViolations, conditionalViolations);
      }
      
      if (nPotentialCommunications > 0) {
        potentialCommunications = new ArrayList<CommunicationData>();
        readCommunicationDataFromHeader(nPotentialCommunications, potentialCommunications);
      }

      if (nNonPotentialCommunications > 0) {
        nonPotentialCommunications = new ArrayList<TransitionData>();
        readTransitionDataFromHeader(nNonPotentialCommunications, nonPotentialCommunications);
      }

    } catch (IOException e) {
      e.printStackTrace();
    } 

  }

  /**
   * A helper method to read a list of potential communication transitions from the header file.
   * @param nCommunications The number of communications that need to be read
   * @param list            The list of communication data
   * @throws IOException    If there was problems reading from file
   **/
  private void readCommunicationDataFromHeader(int nCommunications, List<CommunicationData> list) throws IOException {

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
   * Write all of the header information to file.
   **/
  @Override public void writeHeaderFile() {

    // Do not write the header file unless we need to
    if (!headerFileNeedsToBeWritten)
      return;

      /* Write the header of the .hdr file */
    
    byte[] buffer = new byte[HEADER_SIZE];

    ByteManipulator.writeLongAsBytes(buffer, 0,  automatonType, 4);
    ByteManipulator.writeLongAsBytes(buffer, 4,  nStates, 8);
    ByteManipulator.writeLongAsBytes(buffer, 12, eventCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 16, stateCapacity, 8);
    ByteManipulator.writeLongAsBytes(buffer, 24, transitionCapacity, 4);
    ByteManipulator.writeLongAsBytes(buffer, 28, labelLength, 4);
    ByteManipulator.writeLongAsBytes(buffer, 32, initialState, 8);
    ByteManipulator.writeLongAsBytes(buffer, 40, nControllers, 4);
    ByteManipulator.writeLongAsBytes(buffer, 44, nControllersBeforeUStructure, 4);
    ByteManipulator.writeLongAsBytes(buffer, 48, events.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 52, (badTransitions             == null ? 0 : badTransitions.size()), 4);
    ByteManipulator.writeLongAsBytes(buffer, 56, (unconditionalViolations    == null ? 0 : unconditionalViolations.size()), 4);
    ByteManipulator.writeLongAsBytes(buffer, 60, (conditionalViolations      == null ? 0 : conditionalViolations.size()), 4);
    ByteManipulator.writeLongAsBytes(buffer, 64, (potentialCommunications    == null ? 0 : potentialCommunications.size()), 4);
    ByteManipulator.writeLongAsBytes(buffer, 68, (nonPotentialCommunications == null ? 0 : nonPotentialCommunications.size()), 4);

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
      writeTransitionDataToHeader(nonPotentialCommunications);  

        /* Indicate that the header file no longer need to be written */

      headerFileNeedsToBeWritten = false;

    } catch (IOException e) {
      e.printStackTrace();
    } 
    

  }

  /**
   * A helper method to write a list of special transitions to the header file.
   * NOTE: This could be made more efficient by using one buffer for all communication data. This
   * is only possible because data.roles.length is supposed to be the same for all data in the list.
   * @param list          The list of transition data
   * @throws IOException  If there was problems writing to file
   **/
  private void writeCommunicationDataToHeader(List<CommunicationData> list) throws IOException {

    if (list == null)
      return;

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

  @Override protected void addAdditionalEdgeProperties(Map<String, String> map) {

    super.addAdditionalEdgeProperties(map);

    if (unconditionalViolations != null)
      for (TransitionData t : unconditionalViolations) {
        String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
        if (map.containsKey(edge))
          map.put(edge, map.get(edge) + ",color=red");
        else
          map.put(edge, ",color=red"); 
      }

    if (conditionalViolations != null)
      for (TransitionData t : conditionalViolations) {
        String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
        if (map.containsKey(edge))
          map.put(edge, map.get(edge) + ",color=green3");
        else
          map.put(edge, ",color=green3"); 
      }

    if (potentialCommunications != null)
      for (TransitionData t : potentialCommunications) {
        String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
        if (map.containsKey(edge))
          map.put(edge, map.get(edge) + ",color=blue,fontcolor=blue");
        else
          map.put(edge, ",color=blue,fontcolor=blue"); 
      }

  }

  @Override protected void renumberStatesInAllTransitionData(RandomAccessFile mappingRAFile) throws IOException {

    super.renumberStatesInAllTransitionData(mappingRAFile);

    renumberStatesInTransitionData(mappingRAFile, unconditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, conditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, potentialCommunications);
    renumberStatesInTransitionData(mappingRAFile, nonPotentialCommunications);

  }

  public int getNumberOfControllersBeforeUStructure() {
    return nControllersBeforeUStructure;
  }

}