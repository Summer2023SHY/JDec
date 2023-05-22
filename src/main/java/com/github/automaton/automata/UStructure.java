package com.github.automaton.automata;

/*
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
 */

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.util.ByteManipulator;
import com.github.automaton.io.IOUtility;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.MutableNode;

/**
 * Represents an un-pruned U-Structure.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class UStructure extends Automaton {

    /* CLASS CONSTANTS */

  /** Whether or not invalid communications should be added for mathematical completeness, or suppressed for efficiency purposes */
  public static boolean SUPPRESS_INVALID_COMMUNICATIONS = true;

  private static Logger logger = LogManager.getLogger();

    /* INSTANCE VARIABLES */

  // Special transitions
  /**
   * Transitions that are graphically suppressed for readability
   * 
   * @since 2.0
   */
  protected List<TransitionData> suppressedTransitions;
  protected List<TransitionData> unconditionalViolations;
  protected List<TransitionData> conditionalViolations;
  protected List<CommunicationData> potentialCommunications;
  protected List<TransitionData> invalidCommunications;
  protected List<NashCommunicationData> nashCommunications;
  protected List<DisablementData> disablementDecisions;

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
   * @param headerFile    The file where the header should be stored
   * @param bodyFile      The file where the body should be stored
   * @param nControllers  The number of controllers
   **/
  public UStructure(File headerFile, File bodyFile, int nControllers) {
    this(
      headerFile,
      bodyFile,
      nControllers,
      true
    );
  }

  /**
   * Implicit constructor: used to load the U-Structure from file or when creating a new U-Structure.
   * @param headerFile    The file where the header should be stored
   * @param bodyFile      The file where the body should be stored
   * @param nControllers  The number of controllers
   * @param clearFiles    Whether or not the header and body files should be wiped before use
   **/
  public UStructure(File headerFile, File bodyFile, int nControllers, boolean clearFiles) {
    this(
      headerFile,
      bodyFile,
      DEFAULT_EVENT_CAPACITY,
      DEFAULT_STATE_CAPACITY,
      DEFAULT_TRANSITION_CAPACITY,
      DEFAULT_LABEL_LENGTH,
      nControllers,
      clearFiles
    );
  }
	
	/**
   * Main constructor.
   * @param headerFile          The binary file to load the header information of the U-Structure from (information about events, etc.)
   * @param bodyFile            The binary file to load the body information of the U-Structure from (states and transitions)
   * @param eventCapacity       The initial event capacity (increases by a factor of 256 when it is exceeded)
   * @param stateCapacity       The initial state capacity (increases by a factor of 256 when it is exceeded)
   * @param transitionCapacity  The initial maximum number of transitions per state (increases by 1 whenever it is exceeded)
   * @param labelLength         The initial maximum number characters per state label (increases by 1 whenever it is exceeded)
   * @param nControllers        The number of controllers
   * @param clearFiles          Whether or not the header and body files should be cleared prior to use
   **/
  public UStructure(File headerFile,
                    File bodyFile,
                    int eventCapacity,
                    long stateCapacity,
                    int transitionCapacity,
                    int labelLength,
                    int nControllers,
                    boolean clearFiles) {
    
    super(headerFile, bodyFile, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllers, clearFiles);
    
	}

  @Override protected void initializeLists() {

    super.initializeLists();

    suppressedTransitions = new ArrayList<TransitionData>();
    unconditionalViolations = new ArrayList<TransitionData>();
    conditionalViolations   = new ArrayList<TransitionData>();
    potentialCommunications = new ArrayList<CommunicationData>();
    invalidCommunications   = new ArrayList<TransitionData>();
    nashCommunications      = new ArrayList<NashCommunicationData>();
    disablementDecisions    = new ArrayList<DisablementData>();
  
  }

    /* AUTOMATA OPERATIONS */

  @Override public UStructure accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new UStructure(newHeaderFile, newBodyFile, nControllers));
  }

  // NOTE: This method works, but it is simply unnecessary, so I commented it out.
  // @Override public UStructure complement(File newHeaderFile, File newBodyFile) throws OperationFailedException {

  //   UStructure uStructure = new UStructure(
  //     newHeaderFile,
  //     newBodyFile,
  //     eventCapacity,
  //     stateCapacity,
  //     events.size(), // This is the new number of transitions that will be required for each state
  //     labelLength,
  //     nControllers,
  //     true
  //   );

  //   return complementHelper(uStructure);

  // }

  @Override
  public UStructure invert() {
    return invertHelper(new UStructure(null, null, eventCapacity, stateCapacity, transitionCapacity, labelLength, nControllers, true));
  }

  /**
   * Generate a new U-Structure, with all communications added (potential communications are marked).
   * @param newHeaderFile The header file where the new U-Structure should be stored
   * @param newBodyFile   The body file where the new U-Structure should be stored
   * @return              The U-Structure with the added transitions
   **/
  public UStructure addCommunications(File newHeaderFile, File newBodyFile) {
    
      /* Setup */

    // Create a mapping between the event labels and their associated properties in the original automaton
    // NOTE: The controller's index (1-based, in this case) is appended to the event's label since each
    //       controller has different properties for each event
    Map<String, Boolean> observableMapping   = new HashMap<String, Boolean>();
    Map<String, Boolean> controllableMapping = new HashMap<String, Boolean>();
    for (Event e : events) {
      LabelVector vector = e.getVector();
      for (int i = 1; i < vector.getSize(); i++) {
        String label = vector.getLabelAtIndex(i);
        observableMapping.put(label + i, e.isObservable()[i - 1]);    
        controllableMapping.put(label + i, e.isControllable()[i - 1]);    
      }
    }

    // Generate all potential communication labels
    Set<LabelVector> leastUpperBounds = new HashSet<LabelVector>();
    for (Event e : events)
      leastUpperBounds.add(e.getVector());
    Set<CommunicationLabelVector> potentialCommunications = findPotentialCommunicationLabels(leastUpperBounds);
    
    // Generate all least upper bounds (if invalid communications are not being suppressed)
    if (SUPPRESS_INVALID_COMMUNICATIONS){
      leastUpperBounds.addAll(potentialCommunications);
    } else
      generateLeastUpperBounds(leastUpperBounds);

    UStructure uStructure = duplicate(newHeaderFile, newBodyFile);

      /* Add communications (marking the potential communications) */

    // Map<String, State> memoization = new HashMap<String, State>();
    for (long s = 1; s <= uStructure.getNumberOfStates(); s++) {

      State startingState = uStructure.getState(s);

      // Try each least upper bound
      for (LabelVector vector : leastUpperBounds) {

        boolean[] vectorElementsFound = new boolean[vector.getSize()];
        State destinationState = uStructure.findWhereCommunicationLeads(vector, vectorElementsFound, startingState/*, memoization*/);
        
        if (destinationState != null) {

          // Add event if it doesn't already exist
          int id;
          Event event = uStructure.getEvent(vector.toString());
          if (event == null) {

            // Determine observable and controllable properties of the event vector
            boolean[] observable   = new boolean[nControllers];
            boolean[] controllable = new boolean[nControllers];
            for (int i = 1; i < vector.getSize(); i++) {
              String label = vector.getLabelAtIndex(i);
              if (!label.equals("*")) {
                observable[i - 1]   = observableMapping.get(label + i);
                controllable[i - 1] = controllableMapping.get(label + i);
              }
            }

            id = uStructure.addEvent(vector.toString(), observable, controllable);
          } else
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
            if (!found) {
              if (SUPPRESS_INVALID_COMMUNICATIONS)
                logger.error("Invalid communication was not suppressed: " + vector);
              uStructure.addInvalidCommunication(startingState.getID(), id, destinationState.getID());
            }
    
          }
         
        }

      }

    }

      /* Ensure that the header file has been written to disk */

    uStructure.writeHeaderFile();

    return uStructure;
    
  }

  /**
   * Checking the feasibility for all possible communication protocols, generate a list of the feasible protocols.
   * @param <T>                         The type of communication data
   * @param communications              The communications to be considered
   *                                    <p>NOTE: These should be a subset of the {@link #potentialCommunications} list of this U-Structure
   * @param mustAlsoSolveControlProblem Whether or not the generated protocols must also solve the control problem
   * @return                            The feasible protocols, sorted smallest to largest
   **/
  public <T extends CommunicationData> List<Set<T>> generateAllFeasibleProtocols(List<T> communications,
                                                                                 boolean mustAlsoSolveControlProblem) {

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
   * @param communications  The communications to be considered (which should be a subset of the {@link #potentialCommunications} list of this U-Structure)
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

      // We only want the smallest feasible protocols
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
   * Greedily generate a feasible protocol (optimality is not guaranteed).
   * @param communications  The communications to be considered (which should be a subset of the {@link #potentialCommunications}/{@link #nashCommunications} lists of this U-Structure)
   * @return                The feasible protocol
   **/
  public Set<CommunicationData> generateFeasibleProtocol(List<CommunicationData> communications) {

    Set<CommunicationData> protocol = new HashSet<CommunicationData>();

    UStructure uStructure = this;

    // Continue until no more violations exist
    while (uStructure.unconditionalViolations.size() > 0 || uStructure.conditionalViolations.size() > 0) {

      // System.out.println(uStructure.unconditionalViolations.size() + " " + uStructure.conditionalViolations.size() + " " + uStructure.getNumberOfStates());

      // Choose an arbitrary violation
      TransitionData chosenViolation = (uStructure.conditionalViolations.size() > 0 ? uStructure.conditionalViolations.get(0) : uStructure.unconditionalViolations.get(0));
      // System.out.println(chosenViolation.toString(uStructure));
      // System.out.println(uStructure.getState(chosenViolation.initialStateID));

      // Determine a communication which is necessary in order to help prevent this violation
      // NOTE: It is possible that more than one communication will be necessary, but this will
      //       be taken care of in subsequent iterations
      CommunicationData associatedCommunication = uStructure.findCommunicationToBeAdded(chosenViolation, this, protocol);

      if (protocol.contains(associatedCommunication)) {
        logger.error("ERROR : There was an infinite loop detected.");
        break;
      }
      protocol.addAll(addCommunicationsToEnsureFeasibility(associatedCommunication));

      // System.out.println("communications added. protocol size is now: " + protocol.size());

      // Apply the protocol, pruning as necessary
      uStructure = applyProtocol(protocol, null, null, false);

      // System.out.println("protocol applied. number of states is now: " + uStructure.getNumberOfStates());
      // System.out.println("actual number of communications left in u-structure: " + uStructure.getSizeOfPotentialAndNashCommunications());

    }

    // System.out.println("finished!");

    return protocol;

  }

  /**
   * Find all feasible protocols which contain each communication in the requested protocol.
   * @param requestedProtocol The protocol that is being made feasible
   * @return                  All feasible protocols
   **/
  public List<Set<CommunicationData>> makeProtocolFeasible(Set<CommunicationData> requestedProtocol) {

      /* Generate powerset of communication protocols */

    List<Set<CommunicationData>> protocols = new ArrayList<Set<CommunicationData>>();
    powerSetSubset(protocols, getPotentialAndNashCommunications(), requestedProtocol);

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
   * @param <T>                         The type of communication data
   * @param protocol                    The chosen protocol
   * @param newHeaderFile               The header file where the new U-Structure should be stored
   * @param newBodyFile                 The body file where the new U-Structure should be stored
   * @param discardUnusedCommunications Whether or not the unused communications should be discarded
   * @return                            This pruned U-Structure that had the specified protocol applied
   **/
  public <T extends CommunicationData> PrunedUStructure applyProtocol(Set<T> protocol,
                                                                      File newHeaderFile,
                                                                      File newBodyFile,
                                                                      boolean discardUnusedCommunications) {

    PrunedUStructure prunedUStructure = duplicateAsPrunedUStructure(null, null);

      /* Remove all communications that are not part of the protocol */

    if (discardUnusedCommunications) {

      for (TransitionData data : invalidCommunications)
        prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

      for (CommunicationData data : getPotentialAndNashCommunications())
        if (!protocol.contains(data))
          prunedUStructure.removeTransition(data.initialStateID, data.eventID, data.targetStateID);

    }

      /* Prune (which removes more transitions) */

    for (CommunicationData data : protocol)
      prunedUStructure.prune(protocol, getEvent(data.eventID).getVector(), data.initialStateID, data.getIndexOfSender() + 1);
    
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
   * NOTE: This only works because the pruned U-Structure currently has identical .bdy and .hdr formats.
   * @param newHeaderFile The header file where the pruned U-Structure should be stored
   * @param newBodyFile   The body file where the pruned U-Structure should be stored
   * @return              The duplicated U-Structure (as a pruned U-Structure)
   **/
  public PrunedUStructure duplicateAsPrunedUStructure(File newHeaderFile, File newBodyFile) {

    if (newHeaderFile == null)
      newHeaderFile = IOUtility.getTemporaryFile();

    if (newBodyFile == null)
      newBodyFile = IOUtility.getTemporaryFile();

    if (!duplicateHelper(newHeaderFile, newBodyFile))
      return null;

    // Change the first byte (which indicates the automaton type)
    try (RandomAccessFile raFile = new RandomAccessFile(newHeaderFile, "rw")) {
      raFile.writeByte((byte) Type.PRUNED_U_STRUCTURE.getNumericValue());
    } catch (IOException e) {
      logger.catching(e);
      return null;
    }

    return new PrunedUStructure(newHeaderFile, newBodyFile);    

  }

  /**
   * Find all nash equilibria using the specified method of combining communication costs.
   * @param combiningCostsMethod                  The method in which communication costs should be combined
   * @return                                      The list of Nash equilibria
   * @throws DoesNotSatisfyObservabilityException If the system does not satisfy observability, meaning
   *                                              that there are no feasible protocols that satisfy the
   *                                              control problem.
   * 
   * @deprecated Operations for Nash equilibria depend on {@link Crush}. As {@link Crush} is deprecated
   * and subject to removal, all Nash equilibria operations are deprecated.
   **/
  @Deprecated(since="1.1")
  public List<Set<NashCommunicationData>> findNashEquilibria(Crush.CombiningCosts combiningCostsMethod)
                                                             throws DoesNotSatisfyObservabilityException {

    List<Set<NashCommunicationData>> feasibleProtocols = generateAllFeasibleProtocols(nashCommunications, true);

    // Throw error if the system does not satisfy observability
    if (feasibleProtocols.size() == 0)
      throw new DoesNotSatisfyObservabilityException();

    return findNashEquilibria(combiningCostsMethod, feasibleProtocols);

  }

  /**
   * Find all nash equilibria using the specified method of combining communication costs and the specified
   * list of feasible protocols.
   * @param combiningCostsMethod                  The method in which communication costs should be combined
   * @param feasibleProtocols                     The list of feasible protocols to consider
   *                                              NOTE: They must all solve the control problem
   * @return                                      The list of Nash equilibria
   * 
   * @deprecated Operations for Nash equilibria depend on {@link Crush}. As {@link Crush} is deprecated
   * and subject to removal, all Nash equilibria operations are deprecated.
   **/
  @Deprecated(since="1.1")
  public List<Set<NashCommunicationData>> findNashEquilibria(Crush.CombiningCosts combiningCostsMethod,
                                                             List<Set<NashCommunicationData>> feasibleProtocols) {

      /* Generate protocol vectors */

    // Combine costs as requested (NOTE: Unless unit costs were specified, each protocol will now
    // contain references to different NashCommunicationData objects)
    for (Set<NashCommunicationData> feasibleProtocol : feasibleProtocols)
      combineCommunicationCosts(feasibleProtocol, combiningCostsMethod);

    // Split each protocol into 2 parts (by sending controller)
    List<ProtocolVector> protocolVectors = new ArrayList<ProtocolVector>();
    for (Set<NashCommunicationData> protocol : feasibleProtocols)
      protocolVectors.add(new ProtocolVector(protocol, nControllers));

      /* Sort protocol vectors */

    Collections.sort(protocolVectors, new Comparator<ProtocolVector>() {
      @Override public int compare(ProtocolVector v1, ProtocolVector v2) {

        // Sort by difference in size
        int diff1 = Math.abs(v1.getCommunications(0).length - v1.getCommunications(1).length);
        int diff2 = Math.abs(v2.getCommunications(0).length - v2.getCommunications(1).length);
        if (diff1 != diff2)
          return Integer.valueOf(sum1).compareTo(sum2);
      
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

  /**
   * Take the crush with respect to a particular controller.
   * NOTE: A HashMap is used instead of a mapping file to map the IDs, under the assumption that
   *       a crush will not contain billions of states.
   * NOTE: All communications in the U-Structure should be Nash communications with this method.
   * @param newHeaderFile         The file where the header should be stored
   * @param newBodyFile           The file where the body should be stored
   * @param indexOfController     The index of the controller in which the crush is taken with respect to (1-based)
   * @param combinedCostsMappings Passed in as an empty map, this method maps the Nash communications as strings
   *                              with the combined costs (if null, then a HashMap will simply not be populated)
   * @param combiningCostsMethod  The method used to combine communication costs (can be null if there are no communications)
   * @return                      The crush
   * 
   * @deprecated Crush is too restrictive in terms of its capabilities, and all operations related to it are subject to removal.
   **/
  @Deprecated(forRemoval = true, since="1.1")
  public Crush crush(File newHeaderFile,
                     File newBodyFile,
                     int indexOfController,
                     Map<String, Double> combinedCostsMappings,
                     Crush.CombiningCosts combiningCostsMethod) {

    if (potentialCommunications.size() > 0)
      logger.warn(potentialCommunications.size() + " communications were ignored. Only Nash communications are being considered.");

      /* Setup */

    // Invert this U-Structure (so that we can travel backwards over transitions)
    UStructure invertedUStructure = invert();

    // Create empty crush, copy over events oberservable by the controller
    Crush crush = new Crush(newHeaderFile, newBodyFile, nControllers);
    for (Event e : events)
      if (!e.getVector().isUnobservableToController(indexOfController))
        crush.addEvent(e.getLabel(), e. isObservable(), e.isControllable());

    // Maps the combined IDs to the ID of the state in the crush, meaning we do not need to re-number states afterwards
    HashMap<BigInteger, Long> mappings = new HashMap<BigInteger, Long>();
    long nextID = 1;

    // Find all connecting states
    Stack<Set<Long>> stackOfConnectedStates = new Stack<Set<Long>>();
    HashSet<BigInteger> crushedStatesAlreadyPushed = new HashSet<BigInteger>();
    Set<Long> connectingStates = new HashSet<Long>();
    findConnectingStates(this, invertedUStructure, connectingStates, initialState, indexOfController);
    stackOfConnectedStates.push(connectingStates);
    crushedStatesAlreadyPushed.add(combineStateIDs(connectingStates));

    boolean isInitialState = true;

      /* Build Crush */

    while (stackOfConnectedStates.size() > 0) {

      // Get set from stack and generate unique ID for that collection of states
      Set<Long> setOfStates = stackOfConnectedStates.pop();
      BigInteger combinedID = combineStateIDs(setOfStates);
      Long mappedID = mappings.get(combinedID);
      if (mappedID == null) {
        mappings.put(combinedID, mappedID = nextID++);
        addStateToCrush(crush, setOfStates, isInitialState, mappedID);
      }

      isInitialState = false;

      // Loop through each event
      outer: for (Event e : crush.events) {

        // Setup
        Set<Long> reachableStates = new HashSet<Long>();
        Set<NashCommunicationData> communicationsToBeCopied = new HashSet<NashCommunicationData>();
        boolean isDisablementDecision = false;
        boolean[] disablementControllers = new boolean[nControllers];
        Arrays.fill(disablementControllers, true);
        
        // Generate list of all reachable states from the current event        
        for (Long id : setOfStates) {
          State s = getState(id);
          for (Transition t : s.getTransitions()) {
            if (t.getEvent().equals(e)) {

              // Find reachable states
              findConnectingStates(this, invertedUStructure, reachableStates, t.getTargetStateID(), indexOfController);

              TransitionData transitionData = new TransitionData(s.getID(), t.getEvent().getID(), t.getTargetStateID());
              
              // Check to see if there are any potential or Nash communications that need to be copied over
              for (NashCommunicationData communication : getNashCommunications())
                if (transitionData.equals(communication))
                  communicationsToBeCopied.add(communication);
              
              // Check to see if there is a disablement decision to be copied over
              for (DisablementData disablementData : disablementDecisions)
                if (transitionData.equals(disablementData)) {
                  isDisablementDecision = true;
                  for (int i = 0; i < nControllers; i++)
                    if (!disablementData.controllers[i])
                      disablementControllers[i] = false;
                  break;
                }

            }
          }
        }

        // Add the transition (if applicable)
        if (reachableStates.size() > 0) {

          BigInteger combinedTargetID = combineStateIDs(reachableStates);
          if (!crushedStatesAlreadyPushed.contains(combinedTargetID)) {
            stackOfConnectedStates.push(reachableStates);
            crushedStatesAlreadyPushed.add(combinedTargetID);
          }
          Long mappedTargetID = mappings.get(combinedTargetID);
          if (mappedTargetID == null) {
            mappings.put(combinedTargetID, mappedTargetID = nextID++);
            addStateToCrush(crush, reachableStates, false, mappedTargetID);
          }
          
          crush.addTransition(mappedID, e.getID(), mappedTargetID);

          // Add disablement decision
          if (isDisablementDecision)
            crush.addDisablementDecision(mappedID, e.getID(), mappedTargetID, disablementControllers);

          // Add Nash communication using combined cost
          if (communicationsToBeCopied.size() > 0) {

            // Combine the communication costs as specified, and combine the probabilities as a sum
            CommunicationRole[] roles = null;
            double totalCost = 0.0;
            double totalProbability = 0.0;

            for (NashCommunicationData communication : communicationsToBeCopied) {

              if (roles == null)
                roles = ArrayUtils.clone(communication.roles);
              totalProbability += communication.probability;
            
              switch (combiningCostsMethod) {

                case SUM: case AVERAGE:
                  totalCost += communication.cost;
                  break;

                case MAX:
                  totalCost = Math.max(totalCost, communication.cost);
                  break;

                default:
                  logger.error("Could not combine communication costs as requested.");
                  break;
              }

            } // for

            // Take the average of the total cost, if specified
            if (combiningCostsMethod == Crush.CombiningCosts.AVERAGE)
              totalCost /= (double) communicationsToBeCopied.size();

            // Store the mappings in between communications and combined costs, if requested (for example, this is used in the Nash operation)
            if (combinedCostsMappings != null)
              for (NashCommunicationData communication : communicationsToBeCopied)
                combinedCostsMappings.put(communication.toNashString(this), totalCost);

            // Add the communication to the Crush
            crush.addNashCommunication(mappedID, e.getID(), mappedTargetID, roles, totalCost, totalProbability);
          
          }

        } // if

      } // outer for

    } // while 

      /* Ensure that the header file has been written to disk */

    crush.writeHeaderFile();

    return crush;

  }

  /**
   * Take the crush with respect to a particular controller.
   * <p>NOTE: All Nash information (cost and probability) will be ignored by this method.
   * @param newHeaderFile         The file where the header should be stored
   * @param newBodyFile           The file where the body should be stored
   * @param indexOfController     The index of the controller in which the crush is taken with respect to (1-based)
   * @return                      The crush
   * 
   * @deprecated Crush is too restrictive in terms of its capabilities, and all operations related to it are subject to removal.
   **/
  @Deprecated(forRemoval = true, since="1.1")
  public Crush crush(File newHeaderFile,
                     File newBodyFile,
                     int indexOfController) {

    if (nashCommunications.size() > 0)
      logger.warn("Nash information was ignored.");

      /* Setup */

    // Invert this U-Structure (so that we can travel backwards over transitions)
    UStructure invertedUStructure = invert();

    // Create empty crush, copy over events observable by the controller
    Crush crush = new Crush(newHeaderFile, newBodyFile, nControllers);
    for (Event e : events)
      if (!e.getVector().isUnobservableToController(indexOfController))
        crush.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    // Maps the combined IDs to the ID of the state in the crush, meaning we do not need to re-number states afterwards
    HashMap<BigInteger, Long> mappings = new HashMap<BigInteger, Long>();
    long nextID = 1;

    // Find all connecting states
    Stack<Set<Long>> stackOfConnectedStates = new Stack<Set<Long>>();
    HashSet<BigInteger> crushedStatesAlreadyPushed = new HashSet<BigInteger>();
    Set<Long> connectingStates = new HashSet<Long>();
    findConnectingStates(this, invertedUStructure, connectingStates, initialState, indexOfController);
    stackOfConnectedStates.push(connectingStates);
    crushedStatesAlreadyPushed.add(combineStateIDs(connectingStates));

    boolean isInitialState = true;

      /* Build Crush */

    while (stackOfConnectedStates.size() > 0) {

      // Get set from stack and generate unique ID for that collection of states
      Set<Long> setOfStates = stackOfConnectedStates.pop();
      BigInteger combinedID = combineStateIDs(setOfStates);
      Long mappedID = mappings.get(combinedID);
      if (mappedID == null) {
        mappings.put(combinedID, mappedID = nextID++);
        addStateToCrush(crush, setOfStates, isInitialState, mappedID);
      }

      isInitialState = false;

      // Loop through each event
      outer: for (Event e : crush.events) {

        // Setup
        Set<Long> reachableStates = new HashSet<Long>();
        CommunicationData communicationToBeCopied = null;
        boolean isDisablementDecision = false;
        boolean[] disablementControllers = new boolean[nControllers];
        Arrays.fill(disablementControllers, true);
        
        // Generate list of all reachable states from the current event        
        for (Long s : setOfStates) {
          State state = getState(s);
          for (Transition t : state.getTransitions())
            if (t.getEvent().equals(e)) {

              // Find reachable states
              findConnectingStates(this, invertedUStructure, reachableStates, t.getTargetStateID(), indexOfController);

              TransitionData transitionData = new TransitionData(s, t.getEvent().getID(), t.getTargetStateID());
              
              // Check to see if there are any communications that need to be copied over
              for (CommunicationData communication : getPotentialCommunications())
                if (transitionData.equals(communication)) {
                  communicationToBeCopied = communication;
                  break;
                }
              
              // Check to see if there is a disablement decision to be copied over
              for (DisablementData disablementData : disablementDecisions)
                if (transitionData.equals(disablementData)) {
                  isDisablementDecision = true;
                  for (int i = 0; i < nControllers; i++)
                    if (!disablementData.controllers[i])
                      disablementControllers[i] = false;
                  break;
                }

            }
        }

        // Add the transition (if applicable)
        if (reachableStates.size() > 0) {

          BigInteger combinedTargetID = combineStateIDs(reachableStates);
          if (!crushedStatesAlreadyPushed.contains(combinedTargetID)) {
            stackOfConnectedStates.push(reachableStates);
            crushedStatesAlreadyPushed.add(combinedTargetID);
          }
          Long mappedTargetID = mappings.get(combinedTargetID);
          if (mappedTargetID == null) {
            mappings.put(combinedTargetID, mappedTargetID = nextID++);
            addStateToCrush(crush, reachableStates, false, mappedTargetID);
          }
          
          crush.addTransition(mappedID, e.getID(), mappedTargetID);

          // Add disablement decision
          if (isDisablementDecision)
            crush.addDisablementDecision(mappedID, e.getID(), mappedTargetID, disablementControllers);

          // Add combined communication
          if (communicationToBeCopied != null)
            crush.addPotentialCommunication(mappedID, e.getID(), mappedTargetID, communicationToBeCopied.roles);

        } // if

      } // outer for

    } // while 

      /* Ensure that the header file has been written to disk */

    crush.writeHeaderFile();

    return crush;

  }

  /**
   * Find the Shapley values for each coalition.
   * <p>NOTE: This can also be used to find the Myerson values once the U-Structure has been pruned.
   * @return  The mapping between the coalitions and their respective values (or {@code null} if there were violations)
   * 
   * @deprecated Crush is too restrictive in terms of its capabilities, and all operations related to it are subject to removal.
   **/
  @Deprecated(forRemoval = true, since="1.1")
  public Map<Set<Integer>, Integer> findShapleyValues() {

    // Ensure that there are no violations
    if (hasViolations())
      return null;

    // Generate crushes for each component (including the 0th component)
    Crush[] crushes = new Crush[nControllers + 1]; // 1-based
    for (int i = 0; i <= nControllers; i++)
      crushes[i] = crush(null, null, i);

    // Get the event and states needed in the crush of the 0th component (because we will use them often)
    List<State> initialStates  = new ArrayList<State>();
    List<Event> disabledEvents = new ArrayList<Event>();
    List<State> targetStates   = new ArrayList<State>();
    for (DisablementData data : crushes[0].getDisablementDecisions()) {
      initialStates.add(crushes[0].getState(data.initialStateID));
      disabledEvents.add(crushes[0].getEvent(data.eventID));
      targetStates.add(crushes[0].getState(data.targetStateID));
    }
    int nGlobalDisablements = initialStates.size();

    // Map the disablements to an ID (each crush needs a separate map, except for the 0th component, in
    // which the disablements are simply numbered sequentially by their order in the list)
    // NOTE: Identical IDs mean that two disablements are the same global disablement decision
    List<HashMap<DisablementData, Integer>> disablementIDs = new ArrayList<HashMap<DisablementData, Integer>>(); // 0-based

    // Add mappings for each Crush
    for (int i = 1; i <= nControllers; i++) {
      HashMap<DisablementData, Integer> mapping = new HashMap<DisablementData, Integer>();
      
      // Add an entry for each disablement decision (which should all be given unique values in this map)
      for (DisablementData data : crushes[i].getDisablementDecisions()) {

        State state1 = crushes[i].getState(data.initialStateID);
        Event event = crushes[i].getEvent(data.eventID);
        State state2 = crushes[i].getState(data.targetStateID);

        for (int j = 0; j < nGlobalDisablements; j++) {
          if (event.equals(disabledEvents.get(j)) // If the event is not the same, then it is a different disablement
              && hasNonNullIntersection(state1, initialStates.get(j)) // The state vectors must share at least one common state
              && hasNonNullIntersection(state2, targetStates.get(j))) { // The state vectors must share at least one common state

            // Add entry
            mapping.put(data, j);

            // Each global disablement should correspond to only one disablement
            // NOTE: If this break statement is remove, we should still get the same answer (just less efficiently)
            break;
          }
        }

      }

      disablementIDs.add(mapping);
    }

    // Generate powerset of controllers (1-based)
    List<Integer> elements = new ArrayList<Integer>();
    for (int i = 1; i <= nControllers; i++)
      elements.add(i);
    List<Set<Integer>> coalitions = new ArrayList<Set<Integer>>();
    powerSet(coalitions, elements);

    // Count the number of disablement decisions that are detected by each coalition
    Map<Set<Integer>, Integer> shapleyValueMappings = new HashMap<Set<Integer>, Integer>();
    for (Set<Integer> coalition : coalitions) {

      // The integers in this set represent disablement decisions (since we gave them unique IDs)
      Set<Integer> countedDisablements = new HashSet<Integer>();

      for (Integer controller : coalition)
        for (DisablementData data : crushes[controller].getDisablementDecisions())
          if (data.controllers[controller - 1])
            countedDisablements.add(disablementIDs.get(controller - 1).get(data));

      shapleyValueMappings.put(coalition, countedDisablements.size());

    }

    return shapleyValueMappings;

  }

  /**
   * Given the Shapley values for each coalition, and the index of a controller, calculate its Shapley value.
   * NOTE: This calculation is specified in the paper 'Coalitions of the willing: Decentralized discrete-event
   *       control as a cooperative game', in section 3.
   * @param shapleyValues     The mappings between the coalitions and their associated Shapley values
   * @param indexOfController The index of the controller (1-based)
   * @return                  The Shapley value of the specified controller
   **/
  public double findShapleyValueForController(Map<Set<Integer>, Integer> shapleyValues, int indexOfController) {

    int sum = 0;

    // Iterate through each coalition
    for (Map.Entry<Set<Integer>, Integer> entry : shapleyValues.entrySet()) {
      Set<Integer> coalition = entry.getKey();

      // Skip this coalition if it contains the controller
      if (coalition.contains(indexOfController))
        continue;

      Integer shapleyValueWithoutController = entry.getValue();

      // Find the Shapley value of this coalition if the controller were to be added
      Set<Integer> coalitionWithController = new HashSet<Integer>(coalition);
      coalitionWithController.add(indexOfController);
      Integer shapleyValueWithController = shapleyValues.get(coalitionWithController);

      // Add calculated value to summation
      sum += factorial(coalition.size())
             * factorial(getNumberOfControllers() - coalition.size() - 1)
             * (shapleyValueWithController - shapleyValueWithoutController);

    }

    return (double) sum / (double) factorial(getNumberOfControllers());

  }

    /* AUTOMATA OPERATION HELPER METHODS */

  /**
   * Determine a communication to be added in order to help prevent the specified violation.
   * @param violation           The violation that we are trying to avoid
   * @param originalUStructure  The original U-Structure
   * @param preExistingProtocol The protocol that we have currently found so far for the original U-Structure
   * @return                    The communication which should be added ({@code null} if nothing was found, which should not happen)
   **/
  private CommunicationData findCommunicationToBeAdded(TransitionData violation, UStructure originalUStructure, Set<CommunicationData> preExistingProtocol) {

      /* Setup */

    UStructure inverted = invert();
    Set<Long> visitedStates = new HashSet<Long>();
    Queue<Long> stateQueue = new LinkedList<Long>();
    Queue<Integer> eventQueue = new LinkedList<Integer>();
    stateQueue.offer(violation.initialStateID);
    eventQueue.offer(violation.eventID);

      /* Do a breadth-first search until we find a communication which can be added that will prevent the violation */

    while (stateQueue.size() > 0) {

      long stateID = stateQueue.poll();
      int eventID  = eventQueue.poll();

      // Check to see if we've found a communication which can prevent the specified violation
      for (CommunicationData communication : getPotentialAndNashCommunications())
        if (communication.initialStateID == stateID)
          if (isStrictSubVector(getEvent(eventID).getVector(), getEvent(communication.eventID).getVector())) {

            // Find the associated communication in the original U-Structure (since the IDs may no longer match after a protocol is applied)
            String initialStateLabel = getState(communication.initialStateID).getLabel();
            String eventLabel        = getEvent(communication.eventID).getLabel();
            String targetStateLabel  = getState(communication.targetStateID).getLabel();
            CommunicationData associatedCommunication = new CommunicationData(
              originalUStructure.getStateID(initialStateLabel),
              originalUStructure.getEvent(eventLabel).getID(),
              originalUStructure.getStateID(targetStateLabel),
              communication.roles
            );

            // Only return it, if it is not already in the protocol
            if (!preExistingProtocol.contains(associatedCommunication))
              return associatedCommunication;
          }

      // State has already been visited
      if (visitedStates.contains(stateID))
        continue;

      visitedStates.add(stateID);

      // Traverse each transition backwards
      State currentState = inverted.getState(stateID);
      for (Transition transition : currentState.getTransitions()) {
        stateQueue.offer(transition.getTargetStateID());
        eventQueue.offer(transition.getEvent().getID());
      }

    }

    logger.error("Could not locate communication to be removed.");

    return null;

  }

  /**
   * Find the communications needed in order to ensure that adding the specified communication is feasible.
   * @param initialCommunication  The communication
   * @return                      The feasible protocol
   **/
  private Set<CommunicationData> addCommunicationsToEnsureFeasibility(CommunicationData initialCommunication) {

    Set<CommunicationData> feasibleProtocol = new HashSet<CommunicationData>();
    UStructure inverted = invert();

    // Group the communications so that they are accessible by state ID
    // NOTE: We do this to reduce the time complexity of this method
    Map<Long, List<CommunicationData>> map = new HashMap<Long, List<CommunicationData>>();
    for (CommunicationData communication : getPotentialAndNashCommunications()) {
      List<CommunicationData> list = map.get(communication.initialStateID);
      if (list == null) {
        list = new ArrayList<CommunicationData>();
        list.add(communication);
        map.put(communication.initialStateID, list);
      } else {
        list.add(communication);
      }

    }

    // Find indistinguishable states
    Set<Long> reachableStates = new HashSet<Long>();
    findReachableStates(this, inverted, reachableStates, initialCommunication.initialStateID, initialCommunication.getIndexOfSender() + 1);

    // Add indistinguishable communications
    for (Long stateID : reachableStates) {
      List<CommunicationData> communications = map.get(stateID);
      if (communications != null)
        for (Transition transition : getState(stateID).getTransitions())
          for (CommunicationData data : communications)
            if (data.eventID == transition.getEvent().getID() && Arrays.deepEquals(initialCommunication.roles, data.roles))
              feasibleProtocol.add(data);
    }

    return feasibleProtocol;

  }

  /**
   * Recursively find the factorial of the specified number.
   * @param n The number to take the factorial of, must be in the range [0,12]
   * @return  The factorial value
   **/
  private int factorial(int n) {

    // Error checking
    if (n < 0 || n > 12) {
      logger.error("Factorial value of " + n + " is outside allowed range.");
      return -1;
    }
    
    // Base case
    if (n == 0)
      return 1;

    // Recursive case
    return n * factorial(n - 1);

  }

  @Override protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

    UStructure uStructure = (UStructure) automaton;

    for (TransitionData data : suppressedTransitions)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
          uStructure.addSuppressedTransition(data.initialStateID, data.eventID, data.targetStateID);

    for (TransitionData data : unconditionalViolations)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
    
    for (TransitionData data : conditionalViolations)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);

    for (CommunicationData data : potentialCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, ArrayUtils.clone(data.roles));

    for (TransitionData data : invalidCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addInvalidCommunication(data.initialStateID, data.eventID, data.targetStateID);

    for (NashCommunicationData data : nashCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addNashCommunication(data.initialStateID, data.eventID, data.targetStateID, ArrayUtils.clone(data.roles), data.cost, data.probability);

    for (DisablementData data : disablementDecisions)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addDisablementDecision(data.initialStateID, data.eventID, data.targetStateID, ArrayUtils.clone(data.controllers));

  }

  /**
   * Using recursion, starting at a given state, determine which state the specified communication leads to (if it exists).
   * @param communication       The event vector representing the communication
   * @param vectorElementsFound Indicates which elements of the vector have been found
   * @param currentState        The state that we are currently on
   * @return                    The destination state (or {@code null} if the communication does not lead to a state)
   **/
  private State findWhereCommunicationLeads(LabelVector communication,
                                            boolean[] vectorElementsFound,
                                            State currentState/*,
                                            Map<String, State> memoization*/) {

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

      /* Memoization

    // NOTE: Memoziation is commented out since it is extremely memory intensive, and should not be used in the general case

    String key = encodeString(communication, vectorElementsFound, currentState);
    if (memoization.containsKey(key))
      return memoization.get(key);

      */

      /* Recursive case */

    // Try all transitions leading from this state
    outer: for (Transition t : currentState.getTransitions()) {

      boolean[] copy = ArrayUtils.clone(vectorElementsFound);

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
      State destinationState = findWhereCommunicationLeads(communication, copy, getState(t.getTargetStateID())/*, memoization*/);
      
      // Return destination if it is found (there will only ever be one destination for a given communication from a given state, so we can stop as soon as we find it the first time)
      if (destinationState != null) {
        // memoization.put(key, destinationState); // Save the answer (NOTE: Saving the dead-ends is more important than saving the answers)
        return destinationState;
      }

    }

    // memoization.put(key, null); // Indicate that this is a dead-end
    return null;

  }

  /**
   * Encode the current method's state in order to use it as a key in a hash map.
   * <p>NOTE: This makes the assumption that commas don't appear in event labels
   * (which {@link com.github.automaton.gui.JDec JDec} prevents)
   * @param communication       The event vector representing the communication
   * @param vectorElementsFound Indicates which elements of the vector have been found
   * @param currentState        The state that we are currently on
   * @return                    The resulting string
   **/
  private String encodeString(LabelVector communication,
                              boolean[] vectorElementsFound,
                              State currentState) {

    StringBuilder missingVectorElements = new StringBuilder();
    for (int i = 0; i < communication.getSize(); i++) {
      String element = communication.getLabelAtIndex(i);
      if (!vectorElementsFound[i]) {
        missingVectorElements.append("," + element);
      } else {
        missingVectorElements.append(",*");
      }
    }

    return currentState.getID() + missingVectorElements.toString();
  
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

    // Find all LUB's of the unobservable labels (which will add communications where there is more than one receiver)
    generateLeastUpperBounds(unobservableLabels);

      /* Find potential communications */

    Set<CommunicationLabelVector> potentialCommunications = new HashSet<CommunicationLabelVector>();
    
    for (LabelVector v1 : observableLabels) {
      for (LabelVector v2 : unobservableLabels) {

          /* Error checking */

        if (v1.getSize() == -1 || v2.getSize() == -1 || v1.getSize() != v2.getSize()) {
          logger.error("Bad event vectors. Least upper bounds generation aborted.");
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
              roles[i - 1] = CommunicationRole.RECEIVER;
          } else {
            potentialCommunication += ",*";
            if (i > 0)
              roles[i - 1] = CommunicationRole.NONE;
          }

          // Make sure that the senders and receivers all are working with the same event
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

              CommunicationRole[] copy = ArrayUtils.clone(roles);
              
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
            logger.error("Bad event vectors. Pair of label vectors skipped.");
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
   * <p>NOTE: This method works under the assumption that the protocol has at least one communication.
   * @param protocol                    The protocol that is being checked for feasibility
   * @param mustAlsoSolveControlProblem Whether or not the protocol must solve the control problem (meaning
   *                                    there are no violations after pruning)
   * @return                            Whether or not the protocol is feasible
   **/
  private boolean isFeasibleProtocol(Set<CommunicationData> protocol, boolean mustAlsoSolveControlProblem) {

    UStructure copy = duplicate();
    copy = copy.applyProtocol(protocol, null, null, true);

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
   *                            <p>NOTE: There is no need for extra information (such as special transitions) to be in the inverted automaton
   * @param reachableStates     The set of reachable states that are being built during this recursive process
   * @param currentStateID      The current state
   * @param vectorIndexOfSender The index in the event vector which corresponds to the sending controller
   **/
  private static void findReachableStates(UStructure uStructure,
                                          UStructure invertedUStructure,
                                          Set<Long> reachableStates,
                                          long currentStateID,
                                          int vectorIndexOfSender) {
    
    // Base case
    if (reachableStates.contains(currentStateID))
      return;

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
   * <p>NOTE: Most methods will need to apply the protocol, then generate 1 or more Crush structures.</p>
   * <p>NOTE: This method was made public in order to be able to test it using another</p>
   * @param feasibleProtocol      The list of Nash communications in which costs will be combined
   *                              <p>NOTE: Unless unit costs are used, then new {@link NashCommunicationData} objects
   *                                    will be created (since those objects could be referenced in other
   *                                    protocols, and we do not want to interfere with them)
   * @param combiningCostsMethod  The method in which the communications are being combined
   * 
   * @deprecated Crush is too restrictive in terms of its capabilities, and all operations related to it are subject to removal.
   **/
  @Deprecated(forRemoval = true, since="1.1")
  public void combineCommunicationCosts(Set<NashCommunicationData> feasibleProtocol,
                                        Crush.CombiningCosts combiningCostsMethod) {

    // No costs need to be combined if we're using unit costs
    if (combiningCostsMethod == Crush.CombiningCosts.UNIT)
      return;

    // Generated the pruned U-Structure by applying the protocol
    PrunedUStructure prunedUStructure = applyProtocol(feasibleProtocol, null, null, true);

    // Determine which Crushes will need to be generated (we need to generate 1 or more)
    boolean[] crushNeedsToBeGenerated = new boolean[nControllers];
    for (NashCommunicationData communication : feasibleProtocol)
      crushNeedsToBeGenerated[communication.getIndexOfSender()] = true;

    // Generate the necessary Crushes, storing only the communication cost mappings
    List<Map<String, Double>> costMappingsByCrush = new ArrayList<Map<String, Double>>();
    for (int i = 0; i < nControllers; i++)
      if (crushNeedsToBeGenerated[i]) {
        Map<String, Double> costMapping = new HashMap<String, Double>();
        prunedUStructure.crush(null, null, i + 1, costMapping, combiningCostsMethod);
        costMappingsByCrush.add(costMapping);
      } else
        costMappingsByCrush.add(null);

    // Clear set of communications (since we are creating new NashCommunicationData objects)
    Set<NashCommunicationData> originalCommunicationData = new HashSet<NashCommunicationData>(feasibleProtocol);
    feasibleProtocol.clear();

    // Adjust the costs according to the mappings, re-adding the new objects to the set
    for (NashCommunicationData communication : originalCommunicationData) {

      Map<String, Double> costMapping = costMappingsByCrush.get(communication.getIndexOfSender());
      double newCost = costMapping.get(communication.toNashString(this));
      feasibleProtocol.add(new NashCommunicationData(communication.initialStateID, communication.eventID, communication.targetStateID, communication.roles, newCost, communication.probability));

    }

  }

  /**
   * Create a label for the crushed state, and add it to the crush.
   * @param crush           The crush structure in which the crushed state is being added to
   * @param setOfStates     The set of states which are being crushed
   * @param isInitialState  Whether or not this crushed state is the initial state
   * @param id              The ID of the crushed state
   * 
   * @deprecated Crush is too restrictive in terms of its capabilities, and all operations related to it are subject to removal.
   **/
  @Deprecated(forRemoval = true, since="1.1")
  private void addStateToCrush(Crush crush, Set<Long> setOfStates, boolean isInitialState, long id) {

    // Create a label for this state
    String label = "";
    for (Long s : setOfStates) {
      State state = getState(s);
      label += "," + state.getLabel();
    }
    label = "<" + label.substring(1) + ">";

    // Add new state
    crush.addStateAt(label, false, new ArrayList<Transition>(), isInitialState, id);

  }

  /**
   * Given a set of states, create a unique combined ID.
   * @param setOfStates The set of states which are being combined
   * @return            The combined ID
   **/
  private BigInteger combineStateIDs(Set<Long> setOfStates) {

    List<Long> listOfIDs = new ArrayList<Long>();

    for (Long s : setOfStates)
      listOfIDs.add(s);

    Collections.sort(listOfIDs);

    return combineBigIDs(listOfIDs, nStates);

  }

  /**
   * Starting at the specified state, find all indistinguishable states with respect to a particular controller.
   * @param uStructure          The relevant U-Structure
   * @param invertedUStructure  The relevant inverted U-Structure
   * @param set                 The set of connected states, which will be populated by this method
   * @param currentStateID      The current state ID
   * @param indexOfController   The index of the controller
   **/
  protected static void findConnectingStates(UStructure uStructure, UStructure invertedUStructure, Set<Long> set, long currentStateID, int indexOfController) {

      /* Base Case */
    
    if (set.contains(currentStateID))
      return;

      /* Recursive Case */

    set.add(currentStateID);

    State currentState = uStructure.getState(currentStateID);

    // Find all unobservable events leading from this state, and add the target states to the set
    for (Transition t : currentState.getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(indexOfController))
        findConnectingStates(uStructure, invertedUStructure, set, t.getTargetStateID(), indexOfController);

    State currentInvertedState = invertedUStructure.getState(currentStateID);

    // Find all unobservable events leading to this state, and add those states to the set
    for (Transition t : currentInvertedState.getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(indexOfController))
        findConnectingStates(uStructure, invertedUStructure, set, t.getTargetStateID(), indexOfController);

  }

  /**
   * Given two states (which have to have label vectors), check if the intersection of the crushed
   * states are non-null, meaning that they share at least one state in common. 
   * @param s1  The first state
   * @param s2  The second state
   * @return    Whether or not the crushed states have at least one state in common
   **/
  public static boolean hasNonNullIntersection(State s1, State s2) {

    LabelVector v1 = new LabelVector(s1.getLabel());
    LabelVector v2 = new LabelVector(s2.getLabel());

    for (int i = 0; i < v1.getSize(); i++)
      for (int j = 0; j < v2.getSize(); j++)
        if (v1.getLabelAtIndex(i).equals(v2.getLabelAtIndex(j)))
          return true;

    return false;

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
  private static <T> void powerSetHelper(List<Set<T>> results,
                                        List<T> masterList,
                                        Set<T> elementsChosen,
                                        int index) {

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

    renumberStatesInTransitionData(mappingRAFile, suppressedTransitions);
    renumberStatesInTransitionData(mappingRAFile, unconditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, conditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, potentialCommunications);
    renumberStatesInTransitionData(mappingRAFile, invalidCommunications);
    renumberStatesInTransitionData(mappingRAFile, nashCommunications);
    renumberStatesInTransitionData(mappingRAFile, disablementDecisions);

  }

  /**
   * Find a counter-example, if one exists. The counter-example is returned in the form of a list
   * of sequences of event labels. There will be one sequence for the system, plus one more for each
   * controller that can control the final event.
   * @param findShortest  If {@code true}, the first path to a unconditional violation found will be selected as a
   *                      counter-example. If {@code false}, then the shortest paths to all unconditional violations
   *                      will be found, and then the longest one will be returned 
   * @return              The list of sequences of event labels (or {@code null} if there are no counter-examples)
   **/
  public List<List<String>> findCounterExample(boolean findShortest) {

    if (!hasViolations())
      return null;

    if (nStates + 1 > Integer.MAX_VALUE)
      logger.error("Integer overflow due to too many states.");

      /* Find counter-examples using a breadth-first search */
    
    // Setup
    Set<List<State>> paths = new HashSet<List<State>>();
    List<State> initialPath = new ArrayList<State>();
    initialPath.add(getState(initialState));
    paths.add(initialPath);
    boolean[] visited = new boolean[(int) (nStates + 1)];
    List<State> longestPath = null;
    TransitionData longestViolation = null;

    // Continue until we have checked all of the paths
    outer: while (paths.size() > 0) {

      Set<List<State>> newPaths = new HashSet<List<State>>(); 

      for (List<State> path : paths) {

        // Check to see if this path leads to a counter-example
        State lastState = path.get(path.size() - 1);
        TransitionData violation = findUnconditionalViolation(lastState);

        // A counter-example has been found, so let's store it
        if (violation != null) {

          // Stop early if we only wanted the shortest one
          if (findShortest)
            return generateSequences(path, violation);

          // Store the path if it is the longest path to a violation so far
          if (longestPath == null || longestPath.size() < path.size()) {
            longestPath = path;
            longestViolation = violation;
          }
            
        }

        // Add the new paths
        for (Transition t : lastState.getTransitions()) {
          int targetStateID = (int) t.getTargetStateID();
          if (!visited[targetStateID]) {
            List<State> copy = new ArrayList<State>(path);
            copy.add(getState(targetStateID));
            visited[targetStateID] = true;
            newPaths.add(copy);
          }
        }

      }

      paths = newPaths;

    }

    // Generate the list of sequences for the longest path to a counter-example and return it
    return generateSequences(longestPath, longestViolation);
    

  }

  /**
   * Generate the list of event label sequences for the counter-example represented by the path of states.
   * @param path      The path of states leading to the counter-example
   * @param violation The relevant violation
   * @return          The list of event label sequences
   **/
  private List<List<String>> generateSequences(List<State> path, TransitionData violation) {

      /* Build sequence of events and event labels for this counter-example */
              
    // Setup
    List<Event> eventSequence = new ArrayList<Event>();
    List<String> labelSequence = new ArrayList<String>();
    Iterator<State> iterator = path.iterator();
    State currentState = iterator.next();
    
    // Build sequence
    while (iterator.hasNext()) {
      State nextState = iterator.next();

      // Find the transition that was followed, and add the event to the sequence
      for (Transition t : currentState.getTransitions()) {
        if (t.getTargetStateID() == nextState.getID()) {
          eventSequence.add(t.getEvent());
          String label = t.getEvent().getVector().getLabelAtIndex(0);
          if (!label.equals("*"))
            labelSequence.add(label);
          break;
        }
      }

      currentState = nextState;
    }

    // Add final event
    Event finalEvent = getEvent(violation.eventID);
    String finalEventLabel = finalEvent.getVector().getLabelAtIndex(0);
    if (!finalEventLabel.equals("*"))
      labelSequence.add(finalEventLabel);
    eventSequence.add(finalEvent);

      /* Create event label sequences for each controller as well */

    List<List<String>> sequences = new ArrayList<List<String>>();
    sequences.add(labelSequence);

    for (int i = 0; i < getNumberOfControllers(); i++) {

      // In counter-example notation, we put a dash if the controller cannot control the final event
      if (!finalEvent.isControllable()[i])
        continue;

      // Build sequence
      List<String> sequence = new ArrayList<String>();
      for (Event e : eventSequence) {
        String label = e.getVector().getLabelAtIndex(i + 1);
        if (!label.equals("*"))
          sequence.add(label);
      }

      sequences.add(sequence);

    }

    return sequences;

  }

    /* IMAGE GENERATION */

  /**
   * {@inheritDoc}
   * 
   * @since 2.0
   */
  @Override
  protected void addAdditionalNodeProperties(State state, MutableNode node) {
    if (state.isEnablementState()) {
      node.add(Color.GREEN3);
    } else if (state.isDisablementState()) {
      node.add(Color.RED);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @since 2.0
   */
  @Override
  protected void addAdditionalLinkProperties(Map<String, Attributes<? extends ForLink>> map) {

    for (TransitionData data : suppressedTransitions) {
      combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.TRANSPARENT, Label.of("")));
    }

    for (TransitionData data : unconditionalViolations) {
      combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.RED, Color.RED.font()));
    }

    for (TransitionData data : conditionalViolations) {
      combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.GREEN3, Color.GREEN3.font()));
    }

    for (TransitionData data : getPotentialCommunications()) {
      combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.BLUE, Color.BLUE.font()));
    }

    for (TransitionData data : getNashCommunications()) {
      combineAttributesInMap(map, createKey(data), Attributes.attrs(Color.BLUE, Color.BLUE.font()));
    }

    for (TransitionData data : getNashCommunications()) {
      combineAttributesInMap(map, createKey(data), Style.DOTTED);
    }
  }

  /**
   * {@inheritDoc}
   * @deprecated This method is no longer used. Use {@link #addAdditionalLinkProperties(Map)} instead.
   */
  @Override
  @Deprecated(since = "2.0", forRemoval = true)
  protected void addAdditionalEdgeProperties(Map<String, String> map) {

    for (TransitionData data : unconditionalViolations)
      appendValueToMap(map, createKey(data), ",color=red,fontcolor=red");

    for (TransitionData data : conditionalViolations)
      appendValueToMap(map, createKey(data), ",color=green3,fontcolor=green3");

    for (TransitionData data : getPotentialCommunications())
      appendValueToMap(map, createKey(data), ",color=blue,fontcolor=blue");

    for (TransitionData data : getNashCommunications())
      appendValueToMap(map, createKey(data), ",color=blue,fontcolor=blue");

    for (TransitionData data : disablementDecisions)
      appendValueToMap(map, createKey(data), ",style=dotted");

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

    // Search entire list since there may be more than one Nash communication
    for (NashCommunicationData communicationData : nashCommunications)
      if (transitionData.equals(communicationData)) {
        str += ",NASH_COMMUNICATION-";
        for (CommunicationRole role : communicationData.roles)
          str += role.getCharacter();
        str += "-" + communicationData.cost;
        str += "-" + communicationData.probability;
      }

    // There is only supposed to be one piece of disablement data per transition
    for (DisablementData disablementData : disablementDecisions)
      if (transitionData.equals(disablementData)) {
        str += ",DISABLEMENT_DECISION-";
        for (boolean b : disablementData.controllers)
          str += (b ? "T" : "F");
        break;
      }
    
    return str;

  }

    /* WORKING WITH FILES */

  @Override public UStructure duplicate() {
    return duplicate(IOUtility.getTemporaryFile(), IOUtility.getTemporaryFile());
  }

  @Override public UStructure duplicate(File newHeaderFile, File newBodyFile) {

    if (!duplicateHelper(newHeaderFile, newBodyFile))
      return null;

    return new UStructure(newHeaderFile, newBodyFile);

  }

  @Override protected void writeSpecialTransitionsToHeader() throws IOException {

      /* Write numbers to indicate how many special transitions are in the file */

    byte[] buffer = new byte[28];
    ByteManipulator.writeLongAsBytes(buffer, 0,  unconditionalViolations.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 4,  conditionalViolations.size(),   4);
    ByteManipulator.writeLongAsBytes(buffer, 8,  potentialCommunications.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 12, invalidCommunications.size(),   4);
    ByteManipulator.writeLongAsBytes(buffer, 16, nashCommunications.size(),      4);
    ByteManipulator.writeLongAsBytes(buffer, 20, disablementDecisions.size(),    4);
    ByteManipulator.writeLongAsBytes(buffer, 24, suppressedTransitions.size(),   4);
    haf.write(buffer);

      /* Write special transitions to the .hdr file */

    writeTransitionDataToHeader(unconditionalViolations);
    writeTransitionDataToHeader(conditionalViolations);
    writeCommunicationDataToHeader(potentialCommunications);
    writeTransitionDataToHeader(invalidCommunications);
    writeNashCommunicationDataToHeader(nashCommunications);
    writeDisablementDataToHeader(disablementDecisions);
    writeTransitionDataToHeader(suppressedTransitions);

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
      
      haf.write(buffer);

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

      byte[] buffer = new byte[36 + data.roles.length];
      int index = 0;

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, Double.doubleToLongBits(data.cost), 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, Double.doubleToLongBits(data.probability), 8);
      index += 8;

      for (CommunicationRole role : data.roles)
        buffer[index++] = role.getNumericValue();
      
      haf.write(buffer);

    }

  }

  /**
   * A helper method to write a list of disablement decisions to the header file.
   * NOTE: This could be made more efficient by using one buffer for all disablement decisions.
   * @param list          The list of disablement decisions
   * @throws IOException  If there were any problems writing to file
   **/
  private void writeDisablementDataToHeader(List<DisablementData> list) throws IOException {

    for (DisablementData data : list) {

      byte[] buffer = new byte[20 + data.controllers.length];
      int index = 0;

      ByteManipulator.writeLongAsBytes(buffer, index, data.initialStateID, 8);
      index += 8;

      ByteManipulator.writeLongAsBytes(buffer, index, data.eventID, 4);
      index += 4;

      ByteManipulator.writeLongAsBytes(buffer, index, data.targetStateID, 8);
      index += 8;

      for (boolean b : data.controllers)
        buffer[index++] = (byte) (b ? 1 : 0);
      
      haf.write(buffer);

    }

  }

  @Override protected void readSpecialTransitionsFromHeader() throws IOException {

      /* Read the number which indicates how many special transitions are in the file */

    byte[] buffer = haf.readHeaderBytes(28);

    int nUnconditionalViolations = ByteManipulator.readBytesAsInt(buffer, 0,  4);
    int nConditionalViolations   = ByteManipulator.readBytesAsInt(buffer, 4,  4);
    int nPotentialCommunications = ByteManipulator.readBytesAsInt(buffer, 8,  4);
    int nInvalidCommunications   = ByteManipulator.readBytesAsInt(buffer, 12, 4);
    int nNashCommunications      = ByteManipulator.readBytesAsInt(buffer, 16, 4);
    int nDisablementDecisions    = ByteManipulator.readBytesAsInt(buffer, 20, 4);
    int nSuppressedTransitions   = ByteManipulator.readBytesAsInt(buffer, 24, 4);

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

    if (nDisablementDecisions > 0) {
      disablementDecisions = new ArrayList<DisablementData>();
      readDisablementDataFromHeader(nDisablementDecisions, disablementDecisions);
    }

    if (nSuppressedTransitions > 0) {
      suppressedTransitions = new ArrayList<TransitionData>();
      readTransitionDataFromHeader(nSuppressedTransitions, suppressedTransitions);
    }

  }

  /**
   * A helper method to read a list of communication transitions from the header file.
   * @param nCommunications The number of communications that need to be read
   * @param list            The list of communication data
   * @throws IOException    If there was problems reading from file
   **/
  private void readCommunicationDataFromHeader(int nCommunications,
                                               List<CommunicationData> list) throws IOException {

    byte[] buffer = haf.readHeaderBytes(nCommunications * (20 + nControllers));
    int index = 0;

    for (int i = 0; i < nCommunications; i++) {

      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      CommunicationRole[] roles = new CommunicationRole[nControllers];
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
  private void readNashCommunicationDataFromHeader(int nCommunications,
                                                   List<NashCommunicationData> list) throws IOException {

    byte[] buffer = haf.readHeaderBytes(nCommunications * (36 + nControllers));
    int index = 0;

    for (int i = 0; i < nCommunications; i++) {

      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      double cost = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, 8));
      index += 8;

      double probability = Double.longBitsToDouble(ByteManipulator.readBytesAsLong(buffer, index, 8));
      index += 8;

      CommunicationRole[] roles = new CommunicationRole[nControllers];
      for (int j = 0; j < roles.length; j++)
        roles[j] = CommunicationRole.getRole(buffer[index++]);
      
      list.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));
    
    }

  }

  /**
   * A helper method to read a list of disablement decisions from the header file.
   * @param nDisablements The number of disablement decisions that need to be read
   * @param list          The list of disablement decisions
   * @throws IOException  If there were any problems reading from file
   **/
  private void readDisablementDataFromHeader(int nDisablements,
                                             List<DisablementData> list) throws IOException {

    byte[] buffer = haf.readHeaderBytes(nDisablements * (20 + nControllers));
    int index = 0;

    for (int i = 0; i < nDisablements; i++) {

      long initialStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;
      
      int eventID = (int) ByteManipulator.readBytesAsLong(buffer, index, 4);
      index += 4;
      
      long targetStateID = ByteManipulator.readBytesAsLong(buffer, index, 8);
      index += 8;

      boolean[] controllers = new boolean[nControllers];
      for (int j = 0; j < controllers.length; j++)
        controllers[j] = (buffer[index++] == 1);
      
      list.add(new DisablementData(initialStateID, eventID, targetStateID, controllers));
    
    }

  }

    /* MUTATOR METHODS */

  /**
   * Remove a special transition, given its transition data.
   * @param data  The transition data associated with the special transitions to be removed
   **/
  @Override protected void removeTransitionData(TransitionData data) {

    suppressedTransitions.remove(data);
    
    unconditionalViolations.remove(data);

    conditionalViolations.remove(data);
    
    // Multiple potential communications could exist for the same transition (this happens when there are more than one potential sender)
    while (potentialCommunications.remove(data));

    // Multiple Nash communications could exist for the same transition (this happens when there are more than one potential sender)
    while (nashCommunications.remove(data));
    
    invalidCommunications.remove(data);

    disablementDecisions.remove(data);

  }

  /**
   * Add a suppressed transition.
   * @param initialStateID   The initial state
   * @param eventID          The event triggering the transition
   * @param targetStateID    The target state
   * 
   * @since 2.0
   **/
  public void addSuppressedTransition(long initialStateID, int eventID, long targetStateID) {
    suppressedTransitions.add(new TransitionData(initialStateID, eventID, targetStateID));
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
  public void addPotentialCommunication(long initialStateID,
                                        int eventID,
                                        long targetStateID,
                                        CommunicationRole[] communicationRoles) {

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
   * Add an invalid communication (which are the communications that were added for mathematical completeness but are not actually potential communications).
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
  public void addNashCommunication(long initialStateID,
                                   int eventID,
                                   long targetStateID,
                                   CommunicationRole[] roles,
                                   double cost,
                                   double probability) {

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

  /**
   * Add a disablement decision.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @param targetStateID   The target state
   * @param controllers     An array indicating which controllers can disable this transition 
   **/
  public void addDisablementDecision(long initialStateID, int eventID, long targetStateID, boolean[] controllers) {
    disablementDecisions.add(new DisablementData(initialStateID, eventID, targetStateID, controllers));
    headerFileNeedsToBeWritten = true;
  }

    /* ACCESSOR METHODS */

  /**
   * Check to see if this U-Structure contains violations.
   * <p>NOTE: Conditional violations are not included for our purposes.
   * @return  Whether or not there are one or more violations
   **/
  public boolean hasViolations() {
    return unconditionalViolations.size() > 0;
  }

  /**
   * Find an arbitrary unconditional violation leading from this state, if one exists.
   * @param startingState The state in which the unconditional violation should come from
   * @return              The violation data (or {@code null}, if none existed)
   **/
  public TransitionData findUnconditionalViolation(State startingState) {

    // Check each transition
    for (Transition transition : startingState.getTransitions()) {

      // Return the first violation that matches the state's ID (if one is found)
      for (TransitionData data : unconditionalViolations)
        if (data.initialStateID == startingState.getID())
          return data;

    }

    return null;

  }

  /**
   * Get the list of potential communications.
   * @return  The potential communications
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
   * Get the list of disablement decisions.
   * @return  The disablement decisions
   **/
  public List<DisablementData> getDisablementDecisions() {
    return disablementDecisions;
  }

}