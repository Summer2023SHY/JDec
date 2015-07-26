/**
 * PrunedUStructure - Extending UStructure, this class represents a pruned U-Structure.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Constructors
 *  -Automata Operations
 *  -Automata Operations Helper Methods
 *  -Mutator Methods
 **/

import java.io.*;
import java.util.*;

public class PrunedUStructure extends UStructure {

    /* CONSTRUCTORS */

  /**
   * Implicit constructor: used to load pruned U-Structure from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   **/
  public PrunedUStructure(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);
  }

  /**
   * Implicit constructor: used when creating a new pruned U-Structure structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   **/
  public PrunedUStructure(File headerFile, File bodyFile, int nControllersBeforeUStructure) {
    super(headerFile, bodyFile, nControllersBeforeUStructure);
  }

    /* AUTOMATA OPERATIONS */

  @Override public PrunedUStructure accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new PrunedUStructure(newHeaderFile, newBodyFile, nControllersBeforeUStructure));
  }

  /**
   * Using recursion, starting at a given state, prune away all necessary transitions.
   * @param protocol        The chosen protocol (which must be feasible)
   * @param communication   The event vector representing the chosen communication
   * @param initialStateID  The ID of the state where the pruning begins at
   **/
  public <T extends CommunicationData> void prune(Set<T> protocol, LabelVector communication, long initialStateID) {
    pruneHelper(protocol, communication, new boolean[communication.getSize()], getState(initialStateID), 0);
  }

  /**
   * Helper method used to prune the U-Structure.
   * @param protocol            The chosen protocol (which must be feasible)
   * @param communication       The event vector representing the chosen communication
   * @param vectorElementsFound Indicates which elements of the vector have already been found
   * @param currentState        The state that we are currently on
   * @param depth               The current depth of the recursion (first iteration has a depth of 0)
   **/
  private <T extends CommunicationData> void pruneHelper(Set<T> protocol, LabelVector communication, boolean[] vectorElementsFound, State currentState, int depth) {

      /* Base case */

    if (depth == nControllersBeforeUStructure)
      return;

      /* Recursive case */

    // Try all transitions leading from this state
    outer: for (Transition t : currentState.getTransitions()) {

      // We do not want to prune any of the chosen communications
      if (depth == 0)
        for (T data : protocol)
          if (currentState.getID() == data.initialStateID && t.getEvent().getID() == data.eventID && t.getTargetStateID() == data.targetStateID)
            continue outer;

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

      // Prune this transition
      removeTransition(currentState.getID(), t.getEvent().getID(), t.getTargetStateID());

      // Recursive call to the state where this transition leads
      pruneHelper(protocol, communication, copy, getState(t.getTargetStateID()), depth + 1);

    }

  }

  /**
   * Take the crush with respect to a particular controller.
   * NOTE: A HashMap is used instead of a mapping file to map the IDs, under the assumption that
   *       a crush will not contain billions of states.
   * NOTE: All communications in the pruned U-Structure should be Nash communications.
   * @param newHeaderFile         The file where the header should be stored
   * @param newBodyFile           The file where the body should be stored
   * @param indexOfController     The index of the controller in which the crush is taken with respect to (1-based)
   * @param combinedCostsMappings Passed in as an empty map, this method maps the original Nash communications
   *                              with the combined costs (if null, then a HashMap will simply not be populated)
   * @param combiningCostsMethod  The method used to combine communication costs
   * @return                      The crush
   **/
  public Crush crush(File newHeaderFile, File newBodyFile, int indexOfController, Map<NashCommunicationData, Long> combinedCostsMappings, Crush.CombiningCosts combiningCostsMethod) {

    if (potentialCommunications.size() > 0)
      System.err.println("WARNING: " + potentialCommunications.size() + " communications were ignored. Only Nash communications are considered in the Crush operation.");

      /* Setup */

    Crush crush = new Crush(newHeaderFile, newBodyFile, nControllersBeforeUStructure);
    for (Event e : events)
      if (!e.getVector().isUnobservableToController(indexOfController))
        crush.addEvent(e.getLabel(), e.isObservable(), e.isControllable());

    // Maps the combined IDs to the ID of the state in the crush, meaning we do not need to re-number states afterwards
    HashMap<Long, Long> mappings = new HashMap<Long, Long>();
    long nextID = 1;

    // Find all connecting states
    Stack<Set<State>> stackOfConnectedStates = new Stack<Set<State>>();
    Set<State> statesConnectingToInitial = new HashSet<State>();
    findConnectingStates(statesConnectingToInitial, getState(initialState), indexOfController);
    stackOfConnectedStates.push(statesConnectingToInitial);

    boolean isInitialState = true;

      /* Build Crush */

    while (stackOfConnectedStates.size() > 0) {

      // Get set from stack and generate unique ID for that collection of states
      Set<State> setOfStates = stackOfConnectedStates.pop();
      long combinedID = combinedStateIDs(setOfStates);
      Long mappedID = mappings.get(combinedID);
      if (mappedID == null) {
        mappings.put(combinedID, mappedID = nextID++);
        addStateToCrush(crush, setOfStates, isInitialState, mappedID);
      }

      isInitialState = false;

      // Loop through each event
      outer: for (Event e : crush.events) {

        // Generate list of all reachable states from the current event
        Set<State> reachableStates = new HashSet<State>();
        Map<Long, Set<NashCommunicationData>> communicationsToBeCopied = new HashMap<Long, Set<NashCommunicationData>>(); // Separated into lists by target state ID (which indicates which ones are combined)
        for (State s : setOfStates)
          for (Transition t : s.getTransitions())
            if (t.getEvent().equals(e)) {

              // Find reachable states
              findConnectingStates(reachableStates, getState(t.getTargetStateID()), indexOfController);

              // Check to see if there are any potential or Nash communications that need to be copied over
              TransitionData transitionData = new TransitionData(s.getID(), t.getEvent().getID(), t.getTargetStateID());
              for (NashCommunicationData communication : getNashCommunications())
                if (transitionData.equals(communication)) {
                  Set<NashCommunicationData> set = communicationsToBeCopied.get(t.getTargetStateID());
                  if (set == null)
                    set = new HashSet<NashCommunicationData>();
                  set.add(communication);
                }
            
            }

        // Add the transition (if applicable)
        if (reachableStates.size() > 0) {

          stackOfConnectedStates.push(reachableStates);

          long combinedTargetID = combinedStateIDs(reachableStates);
          Long mappedTargetID = mappings.get(combinedTargetID);
          if (mappedTargetID == null) {
            mappings.put(combinedTargetID, mappedTargetID = nextID++);
            addStateToCrush(crush, reachableStates, false, mappedTargetID);
          }
          
          crush.addTransition(mappedID, e.getID(), mappedTargetID);

          // Add Nash communications
          for (Set<NashCommunicationData> set : communicationsToBeCopied.values()) {

            // Combine the communication costs as specified, and combine the probabilities as a sum
            CommunicationRole[] roles = null;
            int totalCost = 0;
            double totalProbability = 0.0;
            for (NashCommunicationData communication : set) {

              if (roles == null)
                roles = (CommunicationRole[]) communication.roles.clone();

              totalProbability += communication.probability;
            
              switch (combiningCostsMethod) {

                case SUM: case AVERAGE:
                  totalCost += communication.cost;
                  // Prevent overflow by simply capping at Integer.MAX_VALUE
                  // (NOTE: This only works under the assumption that all costs are non-negative)
                  if (totalCost < 0)
                    totalCost = Integer.MAX_VALUE;
                  break;

                case MAX:
                  totalCost = Math.max(totalCost, communication.cost);
                  break;

                default:
                  System.err.println("ERROR: Could not combine communication costs as requested.");
                  break;
              }

            }

            // Take the average of the total cost, if specified
            if (combiningCostsMethod == Crush.CombiningCosts.AVERAGE)
              totalCost /= (double) nControllersBeforeUStructure;

            // Store the mappings in between communications and combined costs, if requested (for example, this is used in the Nash operation)
            if (combinedCostsMappings != null)
              for (NashCommunicationData communication : set)
                combinedCostsMappings.put(communication, (long) totalCost);

            // Add the communication to the Crush
            crush.addNashCommunication(mappedID, e.getID(), mappedTargetID, roles, totalCost, totalProbability);
          
          }

        }

      } // outer for

    } // while 

      /* Ensure that the header file has been written to disk */

    crush.writeHeaderFile();

    return crush;

  }

    /* AUTOMATA OPERATIONS HELPER METHODS */

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
  private long combinedStateIDs(Set<State> setOfStates) {

    List<Long> listOfIDs = new ArrayList<Long>();

    for (State s : setOfStates)
      listOfIDs.add(s.getID());

    Collections.sort(listOfIDs);

    return combineIDs(listOfIDs, nStates);

  }

  /**
   * Starting at the specified state, find all indistinguishable states with respect to a particular controller.
   * @param set               The set of connected states, which will be populated by this method
   * @param currentState      The current state
   * @param indexOfController The index of the controller
   **/
  private void findConnectingStates(Set<State> set, State currentState, int indexOfController) {

      /* Base Case */
    
    if (set.contains(currentState))
      return;

      /* Recursive Case */

    set.add(currentState);

    // Find all unobservable events leading from this state, and add the target states to the set
    for (Transition t : currentState.getTransitions())
      if (t.getEvent().getVector().isUnobservableToController(indexOfController))
        findConnectingStates(set, getState(t.getTargetStateID()), indexOfController);

  }

    /* MUTATOR METHODS */

  /**
   * Remove all events which are inactive (meaning that they do not appear in a transition).
   **/
  public void removeInactiveEvents() { 

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
    renumberEventsInTransitionData(mapping, unconditionalViolations);
    renumberEventsInTransitionData(mapping, conditionalViolations);
    renumberEventsInTransitionData(mapping, potentialCommunications);
    renumberEventsInTransitionData(mapping, invalidCommunications);
    renumberEventsInTransitionData(mapping, nashCommunications);

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

    for (TransitionData data : list)
      data.eventID = mapping.get((Integer) data.eventID);

    headerFileNeedsToBeWritten = true;

  }

}