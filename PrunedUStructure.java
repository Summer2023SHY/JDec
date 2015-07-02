import java.io.*;
import java.util.*;

public class PrunedUStructure extends UStructure {

  private List<NashCommunicationData> nashCommunications;

  /**
   * Implicit constructor: used to load crush from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   **/
  public PrunedUStructure(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);
  }

  /**
   * Implicit constructor: used when creating a new crush structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   **/
  public PrunedUStructure(File headerFile, File bodyFile, int nControllersBeforeUStructure) {
    super(headerFile, bodyFile, nControllersBeforeUStructure);
  }

  /**
   * Using recursion, starting at a given state, prune away all necessary transitions.
   * @param protocol        The chosen protocol (which must be feasible)
   * @param communication   The event vector representing the chosen communication
   * @param initialStateID  The ID of the state where the pruning begins at
   **/
  public void prune(Set<CommunicationData> protocol, LabelVector communication, long initialStateID) {

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
  private void pruneHelper(Set<CommunicationData> protocol, LabelVector communication, boolean[] vectorElementsFound, State currentState, int depth) {

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

  @Override public PrunedUStructure accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new PrunedUStructure(newHeaderFile, newBodyFile, nControllersBeforeUStructure));
  }

  /**
   * Remove all events which are inactive (meaning that they do not appear in a transition)
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
   * Add a nash communication.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @param targetStateID   The target state
   * @param roles           The communication roles associated with each controller
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public void addNashCommunication(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, int cost, double probability) {

    if (nashCommunications == null)
      nashCommunications = new ArrayList<NashCommunicationData>();

    nashCommunications.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

  // We are not using a mapping file, but instead a HashMap, because we assume that the crush will not have billions of states
  public Crush crush(File newHeaderFile, File newBodyFile, int indexOfController) {

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

      // Loop through event event
      outer: for (Event e : crush.events) {

        // Generate list of all reachable states from the current event
        Set<State> reachableStates = new HashSet<State>();
        for (State s : setOfStates)
          for (Transition t : s.getTransitions())
            if (t.getEvent().equals(e))
              findConnectingStates(reachableStates, getState(t.getTargetStateID()), indexOfController);

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

        }

      }

     }

      /* Ensure that the header file has been written to disk */

    crush.writeHeaderFile();

    return crush;

  }

  private void addStateToCrush(Crush crush, Set<State> setOfStates, boolean isInitialState, long id) {

    // Create a label for this state
    String label = "";
    for (State s : setOfStates)
      label += "," + s.getLabel();
    label = "<" + label.substring(1) + ">";

    // Add new state
    crush.addStateAt(label, false, new ArrayList<Transition>(), isInitialState, id);

  }

  private long combinedStateIDs(Set<State> setOfStates) {

    List<Long> listOfIDs = new ArrayList<Long>();

    for (State s : setOfStates)
      listOfIDs.add(s.getID());

    Collections.sort(listOfIDs);

    return combineIDs(listOfIDs, nStates);

  }

  // UNTESTED
  private void findConnectingStates(Set<State> set, State currentState, int indexOfController) {

   // Base case
   if (set.contains(currentState))
     return;

   set.add(currentState);

   // Find all unobservable events leading from this state, and add the target states to the set
   for (Transition t : currentState.getTransitions())
     if (t.getEvent().getVector().isUnobservableToController(indexOfController))
       findConnectingStates(set, getState(t.getTargetStateID()), indexOfController);

  }

  @Override protected void writeSpecialTransitionsToHeader() throws IOException {

    super.writeSpecialTransitionsToHeader();

      /* Write a number to indicate how many nash communications are in the file */

    byte[] buffer = new byte[4];
    ByteManipulator.writeLongAsBytes(buffer, 0, (nashCommunications == null ? 0 : nashCommunications.size()), 4);
    headerRAFile.write(buffer);

      /* Write nash communications to the .hdr file */

    writeNashCommunicationDataToHeader(nashCommunications);

  }

  @Override protected void readSpecialTransitionsFromHeader() throws IOException {

    super.readSpecialTransitionsFromHeader();

      /* Read the number which indicates how many nash communications are in the file */

    byte[] buffer = new byte[20];
    headerRAFile.read(buffer);
    int nNashCommunications = (int) ByteManipulator.readBytesAsLong(buffer, 0, 4);

      /* Read in nash communications from the .hdr file */

    if (nNashCommunications > 0) {
      nashCommunications = new ArrayList<NashCommunicationData>();
      readNashCommunicationDataFromHeader(nNashCommunications, nashCommunications);
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

  /**
   * A helper method to write a list of communications to the header file.
   * NOTE: This could be made more efficient by using one buffer for all communication data. This
   * is possible because each piece of data in the list is supposed to have the same number of roles.
   * @param list          The list of nash communication data
   * @throws IOException  If there was problems writing to file
   **/
  private void writeNashCommunicationDataToHeader(List<NashCommunicationData> list) throws IOException {

    if (list == null)
      return;

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

}