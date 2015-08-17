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
import java.math.*;

public class UStructure extends Automaton {

    /* INSTANCE VARIABLES */

  // Special transitions
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

  @Override public UStructure complement(File newHeaderFile, File newBodyFile) throws OperationFailedException {

    UStructure uStructure = new UStructure(
      newHeaderFile,
      newBodyFile,
      eventCapacity,
      stateCapacity,
      events.size(), // This is the new number of transitions that will be required for each state
      labelLength,
      nControllers,
      true
    );

    return complementHelper(uStructure);

  }

  @Override protected UStructure invert() {
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
   * @param communications              The communications to be considered
   *                                    NOTE: These should be a subset of the potentialCommunications list of this U-Structure
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
   * @param protocol      The chosen protocol
   * @param newHeaderFile The header file where the new U-Structure should be stored
   * @param newBodyFile   The body file where the new U-Structure should be stored
   * @return              The pruned U-Structure that had the specified protocol applied
   **/
  public <T extends CommunicationData> PrunedUStructure applyProtocol(Set<T> protocol,
                                                                      File newHeaderFile,
                                                                      File newBodyFile) {

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

  /**
   * Find all nash equilibria using the specified method of combining communication costs.
   * @param combiningCostsMethod                  The method in which communication costs should be combined
   * @return                                      The list of Nash equilibria
   * @throws DoesNotSatisfyObservabilityException If the system does not satisfy observability, meaning
   *                                              that there are no feasible protocols that satisfy the
   *                                              control problem.
   **/
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
   **/
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

  /**
   * Take the crush with respect to a particular controller.
   * NOTE: A HashMap is used instead of a mapping file to map the IDs, under the assumption that
   *       a crush will not contain billions of states.
   * NOTE: All communications in the pruned U-Structure should be Nash communications.
   * @param newHeaderFile         The file where the header should be stored
   * @param newBodyFile           The file where the body should be stored
   * @param indexOfController     The index of the controller in which the crush is taken with respect to (1-based)
   * @param combinedCostsMappings Passed in as an empty map, this method maps the Nash communications as strings
   *                              with the combined costs (if null, then a HashMap will simply not be populated)
   * @param combiningCostsMethod  The method used to combine communication costs (can be null if there are no communications)
   * @return                      The crush
   **/
  public Crush crush(File newHeaderFile,
                     File newBodyFile,
                     int indexOfController,
                     Map<String, Double> combinedCostsMappings,
                     Crush.CombiningCosts combiningCostsMethod) {

    if (potentialCommunications.size() > 0)
      System.err.println("WARNING: " + potentialCommunications.size() + " communications were ignored. Only Nash communications are being considered in the Crush operation.");

      /* Setup */

    // Invert this U-Structure (so that we can travel backwards over transitions)
    UStructure invertedUStructure = invert();

    // Create empty crush, copy over events oberservable by the controller
    Crush crush = new Crush(newHeaderFile, newBodyFile, nControllers);
    for (Event e : events)
      if (!e.getVector().isUnobservableToController(indexOfController))
        crush.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    // Maps the combined IDs to the ID of the state in the crush, meaning we do not need to re-number states afterwards
    HashMap<BigInteger, Long> mappings = new HashMap<BigInteger, Long>();
    long nextID = 1;

    // Find all connecting states
    Stack<Set<State>> stackOfConnectedStates = new Stack<Set<State>>();
    HashSet<BigInteger> crushedStatesAlreadyPushed = new HashSet<BigInteger>();
    Set<State> connectingStates = new HashSet<State>();
    findConnectingStates(this, connectingStates, getState(initialState), indexOfController);
    Set<State> connectingStatesFromInverted = new HashSet<State>();
    findConnectingStates(invertedUStructure, connectingStatesFromInverted, invertedUStructure.getState(initialState), indexOfController);
    connectingStates.addAll(connectingStatesFromInverted);
    stackOfConnectedStates.push(connectingStates);
    crushedStatesAlreadyPushed.add(combineStateIDs(connectingStates));

    boolean isInitialState = true;

      /* Build Crush */

    while (stackOfConnectedStates.size() > 0) {

      // Get set from stack and generate unique ID for that collection of states
      Set<State> setOfStates = stackOfConnectedStates.pop();
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
        Set<State> reachableStates = new HashSet<State>();
        Set<NashCommunicationData> communicationsToBeCopied = new HashSet<NashCommunicationData>();
        boolean isDisablementDecision = false;
        boolean[] disablementControllers = new boolean[nControllers];
        Arrays.fill(disablementControllers, true);
        
        // Generate list of all reachable states from the current event        
        for (State s : setOfStates)
          for (Transition t : s.getTransitions())
            if (t.getEvent().equals(e)) {

              // Find reachable states
              findConnectingStates(this, reachableStates, getState(t.getTargetStateID()), indexOfController);
              findConnectingStates(invertedUStructure, reachableStates, invertedUStructure.getState(t.getTargetStateID()), indexOfController);

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
                roles = (CommunicationRole[]) communication.roles.clone();
              totalProbability += communication.probability;
            
              switch (combiningCostsMethod) {

                case SUM: case AVERAGE:
                  totalCost += communication.cost;
                  break;

                case MAX:
                  totalCost = Math.max(totalCost, communication.cost);
                  break;

                default:
                  System.err.println("ERROR: Could not combine communication costs as requested.");
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
   * Find the Shapley values, printing them out to the console.
   * NOTE: This is not currently not set designed to handle violations.
   **/
  public void findShapleyValues() {

    // Generate crushes for each component (including the 0th component)
    Crush[] crushes = new Crush[nControllers + 1]; // 1-based
    for (int i = 0; i <= nControllers; i++)
      crushes[i] = crush(null, null, i, null, null);

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
    for (Set<Integer> coalition : coalitions) {

      // The integers in this set represent disablement decisions (since we gave them unique IDs)
      Set<Integer> countedDisablements = new HashSet<Integer>();

      for (Integer controller : coalition)
        for (DisablementData data : crushes[controller].getDisablementDecisions())
          if (data.controllers[controller - 1])
            countedDisablements.add(disablementIDs.get(controller - 1).get(data));

      System.out.println(coalition.toString() + " : " + countedDisablements.size());

    }

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

    for (TransitionData data : invalidCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addInvalidCommunication(data.initialStateID, data.eventID, data.targetStateID);

    for (NashCommunicationData data : nashCommunications)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addNashCommunication(data.initialStateID, data.eventID, data.targetStateID, (CommunicationRole[]) data.roles.clone(), data.cost, data.probability);

    for (DisablementData data : disablementDecisions)
      if (uStructure.stateExists(data.initialStateID) && uStructure.stateExists(data.targetStateID))
        uStructure.addDisablementDecision(data.initialStateID, data.eventID, data.targetStateID, (boolean[]) data.controllers.clone());

  }

  /**
   * Using recursion, starting at a given state, determine which state the specified communication leads to (if it exists).
   * @param communication       The event vector representing the communication
   * @param vectorElementsFound Indicates which elements of the vector have been found
   * @param currentState        The state that we are currently on
   * @return                    The destination state (or null if the communication does not lead to a state)
   **/
  private State findWhereCommunicationLeads(LabelVector communication,
                                            boolean[] vectorElementsFound,
                                            State currentState) {

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
   * @param mustAlsoSolveControlProblem Whether or not the protocol must solve the control problem (meaning
   *                                    there are no violations after pruning)
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
  private static void findReachableStates(UStructure uStructure,
                                          UStructure invertedUStructure,
                                          Set<Long> reachableStates,
                                          long currentStateID,
                                          int vectorIndexOfSender) {

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
   * NOTE: This method was made public in order to be able to test it using another
   * @param feasibleProtocol      The list of Nash communications in which costs will be combined
   *                              NOTE: Unless unit costs are used, then new NashCommunicationData objects
   *                                    will be created (since those objects could be referenced in other
   *                                    protocols, and we do not want to interfere with them)
   * @param combiningCostsMethod  The method in which the communications are being combined
   **/
  public void combineCommunicationCosts(Set<NashCommunicationData> feasibleProtocol,
                                        Crush.CombiningCosts combiningCostsMethod) {

    // No costs need to be combined if we're using unit costs
    if (combiningCostsMethod == Crush.CombiningCosts.UNIT)
      return;

    // Generated the pruned U-Structure by applying the protocol
    PrunedUStructure prunedUStructure = applyProtocol(feasibleProtocol, null, null);

    // Determine which Crushes will need to be generated (we need to generate 1 or more)
    boolean[] crushNeedsToBeGenerated = new boolean[nControllers];
    for (NashCommunicationData communication : feasibleProtocol)
      crushNeedsToBeGenerated[communication.getIndexOfSender()] = true;

    // Generate the neccessary Crushes, storing only the communication cost mappings
    List<Map<String, Double>> costMappingsByCrush = new ArrayList<Map<String, Double>>();
    for (int i = 0; i < nControllers; i++)
      if (crushNeedsToBeGenerated[i]) {
        Map<String, Double> costMapping = new HashMap<String, Double>();
        prunedUStructure.crush(null, null, i + 1, costMapping, combiningCostsMethod);
        costMappingsByCrush.add(costMapping);
      } else
        costMappingsByCrush.add(null);

    // Clear set of communications (since we are creating new NashCommuniationData objects)
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
   **/
  private void addStateToCrush(Crush crush, Set<State> setOfStates, boolean isInitialState, long id) {

    // Create a label for this state
    String label = "";
    for (State s : setOfStates)
      label += "," + s.getLabel();
    label = "<" + label.substring(1) + ">";

    // Add new state
    crush.addStateAt(label, false, new ArrayList<Transition>(), isInitialState, id);

  }

  /**
   * Given a set of states, create a unique combined ID.
   * @param setOfStates The set of states which are being combined
   * @return            The combined ID
   **/
  private BigInteger combineStateIDs(Set<State> setOfStates) {

    List<Long> listOfIDs = new ArrayList<Long>();

    for (State s : setOfStates)
      listOfIDs.add(s.getID());

    Collections.sort(listOfIDs);

    return combineBigIDs(listOfIDs, nStates);

  }

  /**
   * Starting at the specified state, find all indistinguishable states with respect to a particular controller.
   * @param uStructure        The relevant U-Structure
   * @param set               The set of connected states, which will be populated by this method
   * @param currentState      The current state
   * @param indexOfController The index of the controller
   **/
  private static void findConnectingStates(UStructure uStructure, Set<State> set, State currentState, int indexOfController) {

      /* Base Case */
    
    if (set.contains(currentState))
      return;

      /* Recursive Case */

    set.add(currentState);

    // Find all unobservable events leading from this state, and add the target states to the set
    for (Transition t : currentState.getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(indexOfController))
        findConnectingStates(uStructure, set, uStructure.getState(t.getTargetStateID()), indexOfController);

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

    renumberStatesInTransitionData(mappingRAFile, unconditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, conditionalViolations);
    renumberStatesInTransitionData(mappingRAFile, potentialCommunications);
    renumberStatesInTransitionData(mappingRAFile, invalidCommunications);
    renumberStatesInTransitionData(mappingRAFile, nashCommunications);
    renumberStatesInTransitionData(mappingRAFile, disablementDecisions);

  }

    /* IMAGE GENERATION */

  @Override protected void addAdditionalEdgeProperties(Map<String, String> map) {

    for (TransitionData data : unconditionalViolations)
      appendValueToMap(map, createKey(data), ",color=red");

    for (TransitionData data : conditionalViolations)
      appendValueToMap(map, createKey(data), ",color=green3");

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
    ByteManipulator.writeLongAsBytes(buffer, 0,  unconditionalViolations.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 4,  conditionalViolations.size(),   4);
    ByteManipulator.writeLongAsBytes(buffer, 8,  potentialCommunications.size(), 4);
    ByteManipulator.writeLongAsBytes(buffer, 12, invalidCommunications.size(),   4);
    ByteManipulator.writeLongAsBytes(buffer, 16, nashCommunications.size(),      4);
    ByteManipulator.writeLongAsBytes(buffer, 20, disablementDecisions.size(),    4);
    headerRAFile.write(buffer);

      /* Write special transitions to the .hdr file */

    writeTransitionDataToHeader(unconditionalViolations);
    writeTransitionDataToHeader(conditionalViolations);
    writeCommunicationDataToHeader(potentialCommunications);
    writeTransitionDataToHeader(invalidCommunications);
    writeNashCommunicationDataToHeader(nashCommunications);
    writeDisablementDataToHeader(disablementDecisions);

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
      
      headerRAFile.write(buffer);

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
      
      headerRAFile.write(buffer);

    }

  }

  @Override protected void readSpecialTransitionsFromHeader() throws IOException {

      /* Read the number which indicates how many special transitions are in the file */

    byte[] buffer = new byte[24];
    headerRAFile.read(buffer);

    int nUnconditionalViolations = (int) ByteManipulator.readBytesAsLong(buffer, 0,  4);
    int nConditionalViolations   = (int) ByteManipulator.readBytesAsLong(buffer, 4,  4);
    int nPotentialCommunications = (int) ByteManipulator.readBytesAsLong(buffer, 8,  4);
    int nInvalidCommunications   = (int) ByteManipulator.readBytesAsLong(buffer, 12, 4);
    int nNashCommunications      = (int) ByteManipulator.readBytesAsLong(buffer, 16, 4);
    int nDisablementDecisions    = (int) ByteManipulator.readBytesAsLong(buffer, 20, 4);

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

  }

  /**
   * A helper method to read a list of communication transitions from the header file.
   * @param nCommunications The number of communications that need to be read
   * @param list            The list of communication data
   * @throws IOException    If there was problems reading from file
   **/
  private void readCommunicationDataFromHeader(int nCommunications,
                                               List<CommunicationData> list) throws IOException {

    byte[] buffer = new byte[nCommunications * (20 + nControllers)];
    headerRAFile.read(buffer);
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

    byte[] buffer = new byte[nCommunications * (36 + nControllers)];
    headerRAFile.read(buffer);
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

    byte[] buffer = new byte[nDisablements * (20 + nControllers)];
    headerRAFile.read(buffer);
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
   * NOTE: Conditional violations are not included for out purposes.
   * @return  Whether or not there are one or more violations
   **/
  public boolean hasViolations() {
    return unconditionalViolations.size() > 0;
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