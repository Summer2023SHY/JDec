/**
 * UStructure - Extending Automaton, this class represents an un-pruned U-Structure.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructors
 *  -Automata Operations
 *  -Automata Operations Helper Methods
 *  -GUI Input Code Generation
 *  -Image Generation
 *  -Working with Files
 *  -Mutator Methods
 *  -Accessor Methods
 **/

import java.util.*;
import java.io.*;

public class UStructure extends Automaton {

    /* INSTANCE VARIABLES */

  // Special transitions
  protected List<TransitionData> unconditionalViolations;
  protected List<TransitionData> conditionalViolations;
  protected List<CommunicationData> potentialCommunications;
  protected List<TransitionData> invalidCommunications;
  protected List<NashCommunicationData> nashCommunications;

  protected int nControllersBeforeUStructure;

    /* CONSTRUCTORS */

  /**
   * Implicit constructor: used to load the U-Structure from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
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
   * Implicit constructor: used to load the U-Structure from file or when creating a new U-Structure.
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
   * @param headerFile                    The binary file to load the header information of the U-Structure from (information about events, etc.)
   * @param bodyFile                      The binary file to load the body information of the U-Structure from (states and transitions)
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
    if (nControllersBeforeUStructure != -1) {
      this.nControllersBeforeUStructure = nControllersBeforeUStructure;
      headerFileNeedsToBeWritten = true;
    }
    
	}

  @Override protected void initializeLists() {

    super.initializeLists();

    unconditionalViolations = new ArrayList<TransitionData>();
    conditionalViolations   = new ArrayList<TransitionData>();
    potentialCommunications = new ArrayList<CommunicationData>();
    invalidCommunications   = new ArrayList<TransitionData>();
    nashCommunications      = new ArrayList<NashCommunicationData>();
  
  }

    /* AUTOMATA OPERATIONS */

  @Override public UStructure accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new UStructure(newHeaderFile, newBodyFile, nControllersBeforeUStructure));
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

  @Override protected UStructure invert() {
    return invertHelper(new UStructure(null, null, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllersBeforeUStructure, true));
  }

    /**
   * Generate a new U-Structure, with all communications added (potential communications are marked).
   * @param newHeaderFile The header file where the new U-Structure should be stored
   * @param newBodyFile   The body file where the new U-Structure should be stored
   * @return              The U-Structure with the added transitions
   **/
  public UStructure addCommunications(File newHeaderFile, File newBodyFile) {
    
      /* Setup */

    // Generate all potential
    Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
    for (Event e : events)
      leastUpperBounds.add(e.getVector());
    Set<CommunicationLabelVector> potentialCommunications = findPotentialCommunicationLabels(leastUpperBounds);
    
    // Generate all least upper bounds
    generateLeastUpperBounds(leastUpperBounds);
    
    UStructure uStructure = duplicate(newHeaderFile, newBodyFile);

      /* Add communications (marking the potential communications) */

    for (long s = 1; s < uStructure.getNumberOfStates(); s++) {

      State startingState = uStructure.getState(s);

      // Try each least upper bound
      for (LabelVector vector : leastUpperBounds) {
        
        boolean[] vectorElementsFound = new boolean[vector.getSize()];
        State destinationState = uStructure.findWhereCommunicationLeads(vector, vectorElementsFound, startingState);
        
        if (destinationState != null) {

          // Add event if it doesn't already exist
          int id;
          Event event = uStructure.getEvent(vector.toString());
          if (event == null)
            id = uStructure.addEvent(vector.toString(), new boolean[] {true}, new boolean[] {true});
          else
            id = event.getID();

          // Add the transition (if it doesn't already exist)
          if (!uStructure.transitionExists(startingState.getID(), id, destinationState.getID())) {

            // Add transition
            uStructure.addTransition(startingState.getID(), id, destinationState.getID());

            // There could be more than one potential communication, so we need to mark them all
            boolean found = false;
            for (CommunicationLabelVector data : potentialCommunications)
              if (vector.equals((LabelVector) data)) {
                uStructure.addPotentialCommunication(startingState.getID(), id, destinationState.getID(), data.roles);
                found = true;
              }

            // If there were no potential communications, then it must be a invalid communication
            if (!found)
              uStructure.addInvalidCommunication(startingState.getID(), id, destinationState.getID());
    
          }
         
        }

      }

    }

      /* Copy over all of the special transitions */

    copyOverSpecialTransitions(uStructure);

      /* Ensure that the header file has been written to disk */

    uStructure.writeHeaderFile();

    return uStructure;
    
  }

  /**
   * Checking the feasibility for all possible communication protocols, generate a list of the feasible protocols.
   * @param communications  The communications to be considered
   *                        NOTE: These should be a subset of the potentialCommunications list of this U-Structure
   * @return                The feasible protocols, sorted smallest to largest
   **/
  public <T extends CommunicationData> List<Set<T>> generateAllFeasibleProtocols(List<T> communications, boolean mustAlsoSolveControlProblem) {

      /* Generate powerset of communication protocols */

    List<Set<T>> protocols = new ArrayList<Set<T>>();
    powerSet(protocols, communications);

      /* Generate list of feasible protocols */

    List<Set<T>> feasibleProtocols = new ArrayList<Set<T>>();
    for (Set<T> protocol : protocols) {

      // Ignore the protocol with no communications (doesn't make sense in our context)
      if (protocol.size() == 0)
        continue;

      if (isFeasibleProtocol(new HashSet<CommunicationData>(protocol), mustAlsoSolveControlProblem))
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
   * @param communications  The communications to be considered (which should be a subset of the potentialCommunications list of this U-Structure)
   * @return                The feasible protocols
   **/
  public List<Set<CommunicationData>> generateSmallestFeasibleProtocols(List<CommunicationData> communications) {

      /* Generate powerset of communication protocols */

    List<Set<CommunicationData>> protocols = new ArrayList<Set<CommunicationData>>();
    powerSet(protocols, communications);

      /* Sort sets by size (so that protocols with fewer communications appear first) */

    Collections.sort(protocols, new Comparator<Set<?>>() {
        @Override public int compare(Set<?> set1, Set<?> set2) {
          return Integer.valueOf(set1.size()).compareTo(set2.size());
        }
      }
    );

      /* Generate list of feasible protocols */

    List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
    int minFeasibleSize = Integer.MAX_VALUE;
    
    for (Set<CommunicationData> protocol : protocols) {

      // We only want the smalelst feasible protocols
      if (protocol.size() > minFeasibleSize)
        break;

      // Ignore the protocol with no communications (doesn't make sense in our context)
      if (protocol.size() == 0)
        continue;

      // Add the protocol to the list if it is feasible
      if (isFeasibleProtocol(protocol, false)) {
        feasibleProtocols.add(protocol);
        minFeasibleSize = protocol.size();
      }

    }

    return feasibleProtocols;

  }

  /**
   * Find all feasible protocols which contain each communication in the requested protocol.
   * @param requestedProtocol The protocol that is being made feasible
   * @return                  All feasible protocols
   **/
  public List<Set<CommunicationData>> makeProtocolFeasible(Set<CommunicationData> requestedProtocol) {

      /* Generate powerset of communication protocols */

    List<Set<CommunicationData>> protocols = new ArrayList<Set<CommunicationData>>();
    powerSetSubset(protocols, potentialCommunications, requestedProtocol);

      /* Generate list of feasible protocols */

    List<Set<CommunicationData>> feasibleProtocols = new ArrayList<Set<CommunicationData>>();
    for (Set<CommunicationData> protocol : protocols) {

      // Ignore the protocol with no communications (doesn't make sense in our context)
      if (protocol.size() == 0)
        continue;

      if (isFeasibleProtocol(protocol, false))
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
   * Refine this U-Structure by applying the specified communication protocol, and doing the necessary pruning.
   * @param protocol      The chosen protocol
   * @param newHeaderFile The header file where the new U-Structure should be stored
   * @param newBodyFile   The body file where the new U-Structure should be stored
   * @return              The pruned U-Structure that had the specified protocol applied
   **/
  public <T extends CommunicationData> PrunedUStructure applyProtocol(Set<T> protocol, File newHeaderFile, File newBodyFile) {

    PrunedUStructure prunedUStructure = duplicateAsPrunedUStructure(null, null);

      /* Remove all communications that are not part of the protocol */

    for (TransitionData data : invalidCommunications)
      prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

    for (CommunicationData data : getPotentialAndNashCommunications())
      if (!protocol.contains(data))
        prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

      /* Prune (which removes more transitions) */

    for (CommunicationData data : protocol)
      prunedUStructure.prune(protocol, getEvent(data.eventID).getVector(), data.initialStateID);
    
      /* Get the accessible part of the U-Structure */

    prunedUStructure = prunedUStructure.accessible(newHeaderFile, newBodyFile);

      /* Remove all inactive events */

    prunedUStructure.removeInactiveEvents();

      /* Write header file */

    prunedUStructure.writeHeaderFile();
    
    return prunedUStructure;

  }

  /**
   * Duplicate this U-Structure as a pruned U-Structure.
   * NOTE: This only works because the pruned U-Structure has identical .bdy and .hdr formats.
   * @param newHeaderFile The header file where the pruned U-Structure should be stored
   * @param newBodyFile   The body file where the pruned U-Structure should be stored
   * @return              The duplicated U-Structure (as a pruned U-Structure)
   **/
  public PrunedUStructure duplicateAsPrunedUStructure(File newHeaderFile, File newBodyFile) {

    if (newHeaderFile == null)
      newHeaderFile = getTemporaryFile();

    if (newBodyFile == null)
      newBodyFile = getTemporaryFile();

    if (!duplicateHelper(newHeaderFile, newBodyFile))
      return null;

    // Change the first byte (which indicates the automaton type)
    try {
      RandomAccessFile raFile = new RandomAccessFile(newHeaderFile, "rw");
      raFile.writeByte((byte) Type.PRUNED_U_STRUCTURE.getNumericValue());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return new PrunedUStructure(newHeaderFile, newBodyFile);    

  }

  /** NOT YET COMMENTED!!! **/
  public List<Set<NashCommunicationData>> nash(Crush.CombiningCosts combiningCostsMethod) throws DoesNotSatisfyObservabilityException {

      /* Generate protocol vectors */

    // Generate the list of all feasible protocols that also solve the control problem
    List<Set<NashCommunicationData>> feasibleProtocols = generateAllFeasibleProtocols(nashCommunications, true);

    // Throw error if the system does not satisfy observability
    if (feasibleProtocols.size() == 0)
      throw new DoesNotSatisfyObservabilityException();

    // Combine costs as requested (NOTE: Unless unit costs were specified, each protocol will now
    // contain references to different NashCommunicationData objects)
    for (Set<NashCommunicationData> feasibleProtocol : feasibleProtocols)
      combineCommunicationCosts(feasibleProtocol, combiningCostsMethod);

    // Split each protocol into 2 parts (by sending controller)
    List<ProtocolVector> protocolVectors = new ArrayList<ProtocolVector>();
    for (Set<NashCommunicationData> protocol : feasibleProtocols)
      protocolVectors.add(new ProtocolVector(protocol, nControllersBeforeUStructure));

      /* Sort protocol vectors */

    Collections.sort(protocolVectors, new Comparator<ProtocolVector>() {
      @Override public int compare(ProtocolVector v1, ProtocolVector v2) {

        // Sort by difference in size
        int diff1 = Math.abs(v1.getCommunications(0).length - v1.getCommunications(1).length);
        int diff2 = Math.abs(v2.getCommunications(0).length - v2.getCommunications(1).length);
        if (diff1 != diff2)
          return Integer.valueOf(diff1).compareTo(diff2);
      
        // In the event of a tie, place smaller protocols first
        int sum1 = v1.getCommunications(0).length + v1.getCommunications(1).length;
        int sum2 = v2.getCommunications(0).length + v2.getCommunications(1).length;
        return Integer.valueOf(sum1).compareTo(sum2);
      
      }
    });

      /* Find compatible protocol vectors */

    // NOTE: This is used to keep track of whether 2 given protocol vectors have the same set of communications at a given index
    boolean[][][] isCompatible = new boolean[2][protocolVectors.size()][protocolVectors.size()];

    // NOTE: This has been optimized, taking advantage of the fact that the grid is symmetric across the last two dimensions of the 3D array 
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < protocolVectors.size(); j++)
        for (int k = j; k < protocolVectors.size(); k++) {

          ProtocolVector v1 = protocolVectors.get(j); 
          ProtocolVector v2 = protocolVectors.get(k);

          // This works under the assumption that communications will be ordered the same in both arrays if they have the same elements
          isCompatible[i][j][k] = isCompatible[i][k][j] = Arrays.deepEquals(v1.getCommunications(i), v2.getCommunications(i));

        }

      /* Look for a Nash equilibrium */

    List<Set<NashCommunicationData>> nashEquilibriaProtocols = new ArrayList<Set<NashCommunicationData>>();

    outer: for (int i = 0; i < protocolVectors.size(); i++) {
      
      ProtocolVector protocol1 = protocolVectors.get(i);

      for (int j = 0; j < protocolVectors.size(); j++) {
        
        // No need to compare it with itself
        if (j == i)
          continue;

        ProtocolVector protocol2 = protocolVectors.get(j);

        // We've found a contradiction if there is a cheaper protocol that is compatible
        if (protocol2.getValue() < protocol1.getValue() && (isCompatible[0][i][j] || isCompatible[1][i][j]))
            continue outer;

      } // for j

      // If we've gotten this far, then we have a Nash equilibrium
      nashEquilibriaProtocols.add(protocol1.getOriginalProtocol());

    } // for i

    return nashEquilibriaProtocols;

  }

    /* AUTOMATA OPERATION HELPER METHODS */

  @Override protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

    UStructure uStructure = (UStructure) automaton;

    for (TransitionData data : unconditionalViolations)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
    
    for (TransitionData data : conditionalViolations)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

    for (CommunicationData data : potentialCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, (CommunicationRole[]) data.roles.clone());

    for (NashCommunicationData data : nashCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addNashCommunication(data.initialStateID, data.eventID, data.targetStateID, (CommunicationRole[]) data.roles.clone(), data.cost, data.probability);

    for (TransitionData data : invalidCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addInvalidCommunication(data.initialStateID, data.eventID, data.targetStateID);

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
      for (int i = 0; i < t.getEvent().getVector().getSize(); i++) {

        String element = t.getEvent().getVector().getLabelAtIndex(i);

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
   * @param leastUpperBounds  The set of LUBs
   * @return                  The set of potential communications, including communication roles
   **/
  private Set<CommunicationLabelVector> findPotentialCommunicationLabels(Set<LabelVector> leastUpperBounds) {

      /* Separate observable and unobservable labels */

    Set<LabelVector> observableLabels   = new HashSet<LabelVector>();
    Set<LabelVector> unobservableLabels = new HashSet<LabelVector>();

    for (LabelVector v : leastUpperBounds) {
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
            potentialCommunication += "," + label1;
            newEventLabel = label1;
            if (i > 0)
              roles[i - 1] = CommunicationRole.SENDER;
          } else if (!label2.equals("*")) {
            potentialCommunication += "," + label2;
            newEventLabel = label2;
            if (i > 0)
              roles[i - 1] = CommunicationRole.RECIEVER;
          } else {
            potentialCommunication += ",*";
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
              leastUpperBound += "," + label2;
            else
              leastUpperBound += "," + label1;

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
   * Check to see if the specified protocol is feasible.
   * NOTE: This method works under the assumption that the protocol has at least one communication.
   * @param protocol                    The protocol that is being checked for feasibility
   * @param mustAlsoSolveControlProblem Whether or not the protocol must solve the control problem (meaning there are no violations after pruning)
   * @return                            Whether or not the protocol is feasible
   **/
  private boolean isFeasibleProtocol(Set<CommunicationData> protocol, boolean mustAlsoSolveControlProblem) {

    UStructure copy = duplicate();
    copy = copy.applyProtocol(protocol, null, null);

    // If it must also solve the control problem, but there are still violations, then return false
    if (mustAlsoSolveControlProblem)
      if (copy.conditionalViolations.size() > 0 || copy.unconditionalViolations.size() > 0)
        return false;

    // If there was a change in the number of communications after pruning, then it is clearly not feasible
    if (copy.getSizeOfPotentialAndNashCommunications() != protocol.size())
      return false;

    UStructure invertedUStructure = copy.invert();

    for (CommunicationData data : copy.getPotentialAndNashCommunications()) {
      
      // Find indistinguishable states
      Set<Long> reachableStates = new HashSet<Long>();
      findReachableStates(copy, invertedUStructure, reachableStates, data.initialStateID, data.getIndexOfSender() + 1);
      
      // Any strict subset of this communication's event vector which is found at an indistinguishable states
      // indicates that there used to be a communication here (before the protocol was applied), but that
      // it should have been part of the protocol, meaning this protocol is not feasible
      LabelVector eventVector = copy.getEvent(data.eventID).getVector();
      for (Long s : reachableStates)
        for (Transition t : copy.getState(s).getTransitions())
          if (isStrictSubVector(t.getEvent().getVector(), eventVector))
            return false;

    }
  
    return true;

  }

  /**
   * Check to see whether the first vector is a strict sub-vector of the second vector.
   * @param v1  The first vector
   * @param v2  The second vector
   * @return    Whether or not the first vector is a strict sub-vector of the second
   **/
  private boolean isStrictSubVector(LabelVector v1, LabelVector v2) {

    // If the vectors are equal or the sizes are different, then it cannot be a strict sub-vector
    if (v1.equals(v2) || v1.getSize() != v2.getSize())
      return false;

    // Compare each pair of elements, ensuring that it's a strict sub-vector
    for (int i = 0; i < v1.getSize(); i++) {
      String label1 = v1.getLabelAtIndex(i);
      String label2 = v2.getLabelAtIndex(i);
      if (!label1.equals(label2) && !label1.equals("*"))
        return false;
    }

    return true;

  }

  /**
   * Using recursion, determine which states are reachable through transitions which are unobservable to the sender.
   * @param uStructure          The relevant U-Structure
   * @param invertedUStructure  A U-Structure identical to the previous (except all transitions are going the opposite direction)
   *                            NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @param reachableStates     The set of reachable states that are being built during this recursive process
   * @param currentStateID      The current state
   * @param vectorIndexOfSender The index in the event vector which corresponds to the sending controller
   **/
  private static void findReachableStates(UStructure uStructure, UStructure invertedUStructure, Set<Long> reachableStates, long currentStateID, int vectorIndexOfSender) {

    reachableStates.add(currentStateID);

    for (Transition t : uStructure.getState(currentStateID).getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(vectorIndexOfSender) && !reachableStates.contains(t.getTargetStateID()))
        findReachableStates(uStructure, invertedUStructure, reachableStates, t.getTargetStateID(), vectorIndexOfSender);

    for (Transition t : invertedUStructure.getState(currentStateID).getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(vectorIndexOfSender) && !reachableStates.contains(t.getTargetStateID()))
        findReachableStates(uStructure, invertedUStructure, reachableStates, t.getTargetStateID(), vectorIndexOfSender);

  }

  /**
   * For a given feasible protocol (that solves the control problem), combine communication costs using
   * the specified technique.
   * NOTE: Most methods will need to apply the protocol, then generate 1 or more Crush structures.
   * @param feasibleProtocol      The list of Nash communications in which costs will be combined
   *                              NOTE: Unless unit costs are used, then new NashCommunicationData objects
   *                                    will be created (since those objects could be referenced in other
   *                                    protocols, and we do not want to interfere with them)
   * @param combiningCostsMethod  The method in which the communications are being combined
   **/
  private void combineCommunicationCosts(Set<NashCommunicationData> feasibleProtocol, Crush.CombiningCosts combiningCostsMethod) {

    // No costs need to be combined if we're using unit costs
    if (combiningCostsMethod == Crush.CombiningCosts.UNIT)
      return;

    // Generated the pruned U-Structure by applying the protocol
    PrunedUStructure prunedUStructure = applyProtocol(feasibleProtocol, null, null);
    
    // Determine which Crushes will need to be generated (we need to generate 1 or more)
    boolean[] crushNeedsToBeGenerated = new boolean[nControllersBeforeUStructure];
    for (NashCommunicationData communication : feasibleProtocol)
      crushNeedsToBeGenerated[communication.getIndexOfSender()] = true;

    // Generate the neccessary Crushes, storing only the communication cost mappings
    List<Map<NashCommunicationData, Integer>> costMappingsByCrush = new ArrayList<Map<NashCommunicationData, Integer>>();
    for (int i = 0; i < nControllersBeforeUStructure; i++)
      if (crushNeedsToBeGenerated[i]) {
        Map<NashCommunicationData, Integer> costMapping = new HashMap<NashCommunicationData, Integer>();
        prunedUStructure.crush(null, null, i + 1, costMapping, combiningCostsMethod);
      } else
        costMappingsByCrush.add(null);

    // Clear set of communications (since we are creating new NashCommuniationData objects)
    Set<NashCommunicationData> originalCommunicationData = new HashSet<NashCommunicationData>(feasibleProtocol);
    feasibleProtocol.clear();

    // Adjust the costs according to the mappings, re-adding the new objects to the set
    for (NashCommunicationData communication : originalCommunicationData) {

      Map<NashCommunicationData, Integer> costMapping = costMappingsByCrush.get(communication.getIndexOfSender());

      int newCost = costMapping.get(communication);
      feasibleProtocol.add(new NashCommunicationData(communication.initialStateID, communication.eventID, communication.targetStateID, communication.roles, newCost, communication.probability));

    }

  }

  /**
   * Generate a list of all possible sets in the powerset which contain the required elements.
   * @param results           This is a list of sets where all of the sets in the powerset will be stored
   * @param masterList        This is the original list of elements in the set
   * @param requiredElements  This is the set of elements which must be included in each generated set
   **/
  private static <T> void powerSetSubset(List<Set<T>> results, List<T> masterList, Set<T> requiredElements) {

    List<T> copyOfMasterList = new ArrayList<T>(masterList);
    copyOfMasterList.removeAll(requiredElements);

    powerSetHelper(results, copyOfMasterList, new HashSet<T>(requiredElements), 0);

  }

  /**
   * A generic method to generate the powerset of the given list, which are stored in the list of sets that you give it.
   * @param results     This is a list of sets where all of the sets in the powerset will be stored
   * @param masterList  This is the original list of elements in the set
   **/
  private static <T> void powerSet(List<Set<T>> results, List<T> masterList) {

    powerSetHelper(results, masterList, new HashSet<T>(), 0);

  }

  /**
   * A method used to help generate the powerset.
   * @param results         This is a list of sets where all of the sets in the powerset will be stored
   * @param masterList      This is the original list of elements in the set
   * @param elementsChosen  This maintains the elements chosen so far
   * @param index           The current index in the master list
   **/
  private static <T> void powerSetHelper(List<Set<T>> results, List<T> masterList, Set<T> elementsChosen, int index) {

      /* Base case */

    if (index == masterList.size()) {
      results.add(elementsChosen);
      return;
    }

      /* Recursive case */

    Set<T> includingElement = new HashSet<T>();
    Set<T> notIncludingElement = new HashSet<T>();
    
    for (T e : elementsChosen) {
      includingElement.add(e);
      notIncludingElement.add(e);
    }

    includingElement.add(masterList.get(index));

    // Recursive calls
    powerSetHelper(results, masterList, includingElement, index + 1);
    powerSetHelper(results, masterList, notIncludingElement, index + 1);

  }

  @Override protected void renumberStatesInAllTransitionData(RandomAccessFile mappingRAFile) throws IOException {

    renumberStatesInTransitionData(mappingRAFile, unconditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, conditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, potentialCommunications);
    renumberStatesInTransitionData(mappingRAFile, nashCommunications);
    renumberStatesInTransitionData(mappingRAFile, invalidCommunications);

  }

    /* IMAGE GENERATION */

  @Override protected void addAdditionalEdgeProperties(Map<String, String> map) {

    for (TransitionData t : unconditionalViolations) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (map.containsKey(edge))
        map.put(edge, map.get(edge) + ",color=red");
      else
        map.put(edge, ",color=red"); 
    }

    for (TransitionData t : conditionalViolations) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (map.containsKey(edge))
        map.put(edge, map.get(edge) + ",color=green3");
      else
        map.put(edge, ",color=green3"); 
    }

    for (TransitionData t : potentialCommunications) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (map.containsKey(edge))
        map.put(edge, map.get(edge) + ",color=blue,fontcolor=blue");
      else
        map.put(edge, ",color=blue,fontcolor=blue"); 
    }

    for (TransitionData t : nashCommunications) {
      String edge = "\"_" + getState(t.initialStateID).getLabel() + "\" -> \"_" + getStateExcludingTransitions(t.targetStateID).getLabel() + "\"";
      if (map.containsKey(edge))
        map.put(edge, map.get(edge) + ",color=blue,fontcolor=blue");
      else
        map.put(edge, ",color=blue,fontcolor=blue"); 
    }

  }

    /* GUI INPUT CODE GENERATION */

  @Override protected String getInputCodeForSpecialTransitions(TransitionData transitionData) {

    String str = "";

    if (unconditionalViolations.contains(transitionData))
      str += ",UNCONDITIONAL_VIOLATION";
    
    if (conditionalViolations.contains(transitionData))
      str += ",CONDITIONAL_VIOLATION";
    
    // Search entire list since there may be more than one potential communication
    String identifier = (type == Type.U_STRUCTURE ? ",POTENTIAL_COMMUNICATION-" : ",COMMUNICATION-");
    for (CommunicationData communicationData : potentialCommunications)
      if (transitionData.equals(communicationData)) {
        str += identifier;
        for (CommunicationRole role : communicationData.roles)
          str += role.getCharacter();
      }

    if (invalidCommunications.contains(transitionData))
      str += ",INVALID_COMMUNICATION";

    // Search entire list since there may be more than one nash communication
    for (NashCommunicationData communicationData : nashCommunications)
      if (transitionData.equals(communicationData)) {
        str += ",NASH_COMMUNICATION-";
        for (CommunicationRole role : communicationData.roles)
          str += role.getCharacter();
        str += "-" + communicationData.cost;
        str += "-" + communicationData.probability;
      }

    return str;

  }

    /* WORKING WITH FILES */

  @Override public UStructure duplicate() {
    return duplicate(getTemporaryFile(), getTemporaryFile());
  }

  @Override public UStructure duplicate(File newHeaderFile, File newBodyFile) {

    if (!duplicateHelper(newHeaderFile, newBodyFile))
      return null;

    return new UStructure(newHeaderFile, newBodyFile);

  }

  @Override protected void writeSpecialTransitionsToHeader() throws IOException {

      /* Write numbers to indicate how many special transitions are in the file */

    byte[] buffer = new byte[24];
    ByteManipulator.writeLongAsBytes(buffer, 0,  nControllersBeforeUStructure,      4);
    ByteManipulator.writeLongAsBytes(buffer, 4,  unconditionalViolations.size(),    4);
    ByteManipulator.writeLongAsBytes(buffer, 8,  conditionalViolations.size(),      4);
    ByteManipulator.writeLongAsBytes(buffer, 12, potentialCommunications.size(),    4);
    ByteManipulator.writeLongAsBytes(buffer, 16, invalidCommunications.size(),      4);
    ByteManipulator.writeLongAsBytes(buffer, 20, nashCommunications.size(),         4);
    headerRAFile.write(buffer);

      /* Write special transitions to the .hdr file */

    writeTransitionDataToHeader(unconditionalViolations);
    writeTransitionDataToHeader(conditionalViolations);
    writeCommunicationDataToHeader(potentialCommunications);
    writeTransitionDataToHeader(invalidCommunications);
    writeNashCommunicationDataToHeader(nashCommunications);

  }

  /**
   * A helper method to write a list of communications to the header file.
   * NOTE: This could be made more efficient by using one buffer for all communication data. This
   * is possible because each piece of data in the list is supposed to have the same number of roles.
   * @param list          The list of communication data
   * @throws IOException  If there was problems writing to file
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
   * A helper method to write a list of communications to the header file.
   * NOTE: This could be made more efficient by using one buffer for all communication data. This
   * is possible because each piece of data in the list is supposed to have the same number of roles.
   * @param list          The list of nash communication data
   * @throws IOException  If there was problems writing to file
   **/
  private void writeNashCommunicationDataToHeader(List<NashCommunicationData> list) throws IOException {


    for (NashCommunicationData data : list) {

      byte[] buffer = new byte[32 + data.roles.length];
      int index = 0;

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.cost, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, Double.doubleToLongBits(data.probability), 8);
      index += 8;

      for (CommunicationRole role : data.roles)
        buffer[index++] = role.getNumericValue();
      
      headerRAFile.write(buffer);

    }

  }

  @Override protected void readSpecialTransitionsFromHeader() throws IOException {

      /* Read the number which indicates how many special transitions are in the file */

    byte[] buffer = new byte[24];
    headerRAFile.read(buffer);

    nControllersBeforeUStructure    = (int) ByteManipulator.readBytesAsLong(buffer, 0,  4);
    int nUnconditionalViolations    = (int) ByteManipulator.readBytesAsLong(buffer, 4,  4);
    int nConditionalViolations      = (int) ByteManipulator.readBytesAsLong(buffer, 8,  4);
    int nPotentialCommunications    = (int) ByteManipulator.readBytesAsLong(buffer, 12, 4);
    int nInvalidCommunications      = (int) ByteManipulator.readBytesAsLong(buffer, 16, 4);
    int nNashCommunications         = (int) ByteManipulator.readBytesAsLong(buffer, 20, 4);

      /* Read in special transitions from the .hdr file */
    
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

    if (nInvalidCommunications > 0) {
      invalidCommunications = new ArrayList<TransitionData>();
      readTransitionDataFromHeader(nInvalidCommunications, invalidCommunications);
    }

    if (nNashCommunications > 0) {
      nashCommunications = new ArrayList<NashCommunicationData>();
      readNashCommunicationDataFromHeader(nNashCommunications, nashCommunications);
    }

  }

  /**
   * A helper method to read a list of communication transitions from the header file.
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
   * A helper method to read a list of nash communication transitions from the header file.
   * @param nCommunications The number of communications that need to be read
   * @param list            The list of nash communication data
   * @throws IOException    If there was problems reading from file
   **/
  private void readNashCommunicationDataFromHeader(int nCommunications, List<NashCommunicationData> list) throws IOException {

    byte[] buffer = new byte[nCommunications * (32 + nControllersBeforeUStructure)];
    headerRAFile.read(buffer);
    int index = 0;

    for (int i = 0; i < nCommunications; i++) {

      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      int cost = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;

      double probability = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, 8));
      index += 8;

      CommunicationRole[] roles = new CommunicationRole[nControllersBeforeUStructure];
      for (int j = 0; j < roles.length; j++)
        roles[j] = CommunicationRole.getRole(buffer[index++]);
      
      list.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));
    
    }

  }

    /* MUTATOR METHODS */

  /**
   * Remove a special transition, given its transition data.
   * @param data  The transition data associated with the special transitions to be removed
   **/
  @Override protected void removeTransitionData(TransitionData data) {
    
    unconditionalViolations.remove(data);

    conditionalViolations.remove(data);
    
    // Multiple potential communications could exist for the same transition (this happens when there are more than one potential sender)
    while (potentialCommunications.remove(data));

    // Multiple Nash communications could exist for the same transition (this happens when there are more than one potential sender)
    while (nashCommunications.remove(data));
    
    invalidCommunications.remove(data);

  }

  /**
   * Add an unconditional violation.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addUnconditionalViolation(long initialStateID, int eventID, long targetStateID) {

    unconditionalViolations.add(new TransitionData(initialStateID, eventID, targetStateID));
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

    potentialCommunications.add(new CommunicationData(initialStateID, eventID, targetStateID, communicationRoles));
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Clears the list of potential communications.
   **/
  public void removeAllPotentialCommunications() {
    potentialCommunications.clear();
    headerFileNeedsToBeWritten = true;
  }

  /**
   * Add an invalid communication (which are the communications that were added for mathmatical completeness but are not actually potential communications).
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   **/
  public void addInvalidCommunication(long initialStateID, int eventID, long targetStateID) {

    invalidCommunications.add(new TransitionData(initialStateID, eventID, targetStateID));
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Add a nash communication.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @param targetStateID   The target state
   * @param roles           The communication roles associated with each controller
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public void addNashCommunication(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, int cost, double probability) {

    nashCommunications.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));
    headerFileNeedsToBeWritten = true;

  }

  /**
   * Clears the list of nash communications.
   **/
  public void removeAllNashCommunications() {
    nashCommunications.clear();
    headerFileNeedsToBeWritten = true;
  }

    /* ACCESSOR METHODS */

  /**
   * Check to see if this U-Structure contains violations.
   * @return Whether or not there are one or more violations
   **/
  public boolean hasViolations() {
    return unconditionalViolations.size() > 0 || conditionalViolations.size() > 0;
  }

  /**
   * Get the list of potential communications.
   * @return The potential communications
   **/
  public List<CommunicationData> getPotentialCommunications() {
    return potentialCommunications;
  }

  /**
   * Get the list of Nash communications.
   * @return  The Nash communications
   **/
  public List<NashCommunicationData> getNashCommunications() {
    return nashCommunications;
  }

  /**
   * Get the size of the union of the list of potential communications and Nash communications.
   * NOTE: This method gets the size without actually creating a union of the two sets.
   * @return  The combined size of the potential communications and Nash communications
   **/
  public int getSizeOfPotentialAndNashCommunications() {
    return potentialCommunications.size() + nashCommunications.size();
  }

  /**
   * Get the union of the list of potential communications and Nash communications.
   * NOTE: This method generates a new list each time it is called.
   * @return  The potential communications and Nash communications
   **/
  public List<CommunicationData> getPotentialAndNashCommunications() {

    List<CommunicationData> communications = new ArrayList<CommunicationData>();
    communications.addAll(potentialCommunications);
    communications.addAll(nashCommunications);

    return communications;
  
  }

  /**
   * Get the number of controller that were present before the U-Structure was created.
   * @return The number of controllers before the U-Structure
   **/
  public int getNumberOfControllersBeforeUStructure() {
    return nControllersBeforeUStructure;
  }

}