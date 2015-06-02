import java.util.*;
import java.io.*;

public abstract class AutomatonGenerator {

  public static Automaton generateRandom(String fileName, int nEvents, long nStates, int minTransitionsPerState, int maxTransitionsPerState, int nControllers, int nBadTransitions) {

      /* Create empty automaton with capacities that should prevent the need to re-create the body file */

    Automaton automaton = new Automaton(
      new File(fileName + ".hdr"),
      new File(fileName + ".bdy"),
      nStates,
      maxTransitionsPerState,
      String.valueOf(nStates).length(),
      nControllers,
      true
      );

      /* Generate events */

    for (int i = 1; i <= nEvents; i++) {

      boolean[] observability = new boolean[nControllers],
                controllability = new boolean[nControllers];

      for (int j = 0; j < nControllers; j++) {
        observability[j] = generateBoolean();
        controllability[j] = generateBoolean();
      }

      int id = automaton.addEvent(String.valueOf(i), observability, controllability);
      if (id == 0)
          System.out.println("ERROR: Event could not be added.");

    }

      /* Generate states */
    {
      long initialStateID = generateLong(1, nStates);
      for (int i = 1; i <= nStates; i++) {
        long id = automaton.addState(String.valueOf(i), generateBoolean(), i == initialStateID);
        if (id == 0)
          System.out.println("ERROR: State could not be added.");
      }
    }

      /* Generate transitions */

    for (long s = 1; s <= nStates; s++) {
      int nTransitions = generateInt(minTransitionsPerState, maxTransitionsPerState);
      for (int i = 0; i < nTransitions; i++) {

        int eventID;
        long targetStateID;

        // Ensure that we don't produce any duplicates
        do {
          eventID = generateInt(1, nEvents);
          targetStateID = generateLong(1, nStates);
        } while (automaton.transitionExists(s, eventID, targetStateID));

        automaton.addTransition(s, eventID, targetStateID);
      }
    }

      /* Add bad transitions */

    for (int i = 0; i < nBadTransitions; i++) {

      int eventID;
      long initialStateID, targetStateID;

      // Ensure that the transition exists (and that it is not already a bad transition)
      do {
        initialStateID = generateLong(1, nStates);
        eventID = generateInt(1, nEvents);
        targetStateID = generateLong(1, nStates);
      } while (!automaton.transitionExists(initialStateID, eventID, targetStateID) || automaton.isBadTransition(initialStateID, eventID, targetStateID));

      automaton.markTransitionAsBad(initialStateID, eventID, targetStateID);

    }

    return automaton;

  }

  private static long generateLong(long min, long max) {

    return min + (long) (Math.random() * ((double) (max - min + 1)));

  }

  private static int generateInt(int min, int max) {

    return min + (int) (Math.random() * ((double) (max - min + 1)));

  }

  private static boolean generateBoolean() {

    return generateInt(0, 1) == 1;

  }

  public static Automaton generateFromGUICode(String eventInputText, String stateInputText, String transitionInputText, int nControllers, boolean verbose, File headerFile) {

      /* Setup */
    
    Automaton automaton = new Automaton(headerFile, nControllers);
    HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events' labels to the events' ID
    HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the states' labels to the state's ID

      /* States */
    
    for (String line : stateInputText.split("\n")) {

      String[] splitLine = line.trim().split(",");
      String label = splitLine[0];

      // Check to see if this is a duplicate state label
      if (stateMapping.get(label) != null) {
        if (verbose)
          System.out.println("ERROR: Could not store '" + line + "' as a state, since there is already a state with this label.");
        continue;
      }

      // Try to add the state
      if (splitLine.length >= 1 && label.length() > 0) {

        boolean isInitialState = (label.charAt(0) == '@');

        // Ensure the user didn't only have a '@' symbols the name of the label (since '@' gets removed, we are left with an empty string)
        if (isInitialState && label.length() == 1) {
          if (verbose)
            System.out.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");
          continue;

        }

        // Remove '@' from the label name
        if (isInitialState)
          label = label.substring(1);

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.out.println("ERROR: Invalid label ('" + label + "').");
          continue;
        }

        long id = automaton.addState(label, splitLine.length < 2 || isTrue(splitLine[1]), isInitialState);

        // Error checking
        if (id == 0) {
          if (verbose)
            System.out.println("ERROR: Could not store '" + line + "' as a state.");
          continue;
        }

        // Add state
        stateMapping.put(label, id);

      } else if (line.length() > 0 && verbose)
      System.out.println("ERROR: Could not parse '" + line + "' as a state.");
    }

      /* The image will be blank if there are no states */

    if (stateMapping.isEmpty())
      return null;
    
      /* Events */

    for (String line : eventInputText.split("\n")) {

      String[] splitLine = line.trim().split(",");
      String label = splitLine[0];

      // Check to see if this is a duplicate event label
      if (eventMapping.get(label) != null) {
        if (verbose)
          System.out.println("ERROR: Could not store '" + line + "' as an event, since there is already an event with this label.");
        continue;
      }

      // Try to add the event
      if (splitLine.length >= 1 && label.length() > 0) {

        // Setup
        boolean[]   observable = new boolean[nControllers],
        controllable = new boolean[nControllers];
        Arrays.fill(observable, true);
        Arrays.fill(controllable, true);

        // Parse controller properties
        if (splitLine.length == 3) {
          if (splitLine[1].length() == nControllers && splitLine[2].length() == nControllers) {
            observable = isTrueArray(splitLine[1]);
            controllable = isTrueArray(splitLine[2]);
          } else {
            System.out.println(
              String.format(
                "ERROR: The number of controllers (%d) does not match the number of properties specified (%d and %d).",
                nControllers,
                splitLine[1].length(),
                splitLine[2].length()
                )
              );
          }
        }

        // Try to add event to automaton
        int id = automaton.addEvent(label, observable, controllable);

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.out.println("ERROR: Invalid label ('" + label + "').");
          continue;
        }

        // Error checking
        if (id == 0) {
          if (verbose)
            System.out.println("ERROR: Could not store '" + line + "' as an event.");
          continue;
        }


        // Add event
        eventMapping.put(label, id);

      } else if (line.length() > 0 && verbose)
        System.out.println("ERROR: Could not parse '" + line + "' as an event.");

    }

      /* Transitions */

    for (String line : transitionInputText.split("\n")) {

      String[] splitLine = line.trim().split(":");
      boolean isUnconditionalViolation = false,
      isConditionalViolation = false,
      isBadTransition = false;

      // Take care of the second half (which identifies special transitions)
      if (splitLine.length > 1) {
        String[] secondHalf = splitLine[1].split(",");
        for (String str : secondHalf) {
          str = str.trim();
          if (str.equals("BAD"))
            isBadTransition = true;
          else if (str.equals("UNCONDITIONAL_VIOLATION"))
            isUnconditionalViolation = true;
          else if (str.equals("CONDITIONAL_VIOLATION"))
            isConditionalViolation = true;   
        }
      }

      // Ensure that all 3 required parameters are present
      String[] firstHalf = splitLine[0].split(",");
      if (firstHalf.length >= 3) {

        String initialStateLabel = firstHalf[0].trim();
        String eventLabel = firstHalf[1].trim();
        String targetStateLabel = firstHalf[2].trim();

        // Get ID's of initial state, event, and target state
        Long initialStateID = stateMapping.get(initialStateLabel);
        Integer eventID = eventMapping.get(eventLabel);
        Long targetStateID = stateMapping.get(targetStateLabel);

        // Prevent crashing by checking to see if any of the values are null (indicates that they've entered a state or event that doesn't exist)
        if (initialStateID == null || eventID == null || targetStateID == null) {
          if (verbose)
            System.out.println("ERROR: Could not store '" + line + "' as a transition.");
        }

        // Add transition
        else {
         
          if (automaton.addTransition(initialStateID, eventID, targetStateID)) {

            // Special transitions
            if (isBadTransition)
              automaton.markTransitionAsBad(initialStateID, eventID, targetStateID);
            if (isUnconditionalViolation)
              automaton.addUnconditionalViolation(initialStateID, eventID, targetStateID);
            if (isConditionalViolation)
              automaton.addConditionalViolation(initialStateID, eventID, targetStateID);

          } else
            System.out.println("ERROR: Could not add '" + line + "' as a transition.");
        }
        
      } else if (line.length() > 0 && verbose)
        System.out.println("ERROR: Could not parse '" + line + "' as a transition.");
    }

    return automaton;
  
  }

  /**
   * Simple helper method to detect whether the given String is either "T" or "t".
   * @param str   The String to parse
   * @return whether or not the String represents "TRUE" 
   **/
  public static boolean isTrue(String str) {
      return str.toUpperCase().equals("T");
  }

  /**
   * Simple helper method to transform a series of T's and F's into a boolean array.
   * @param str   The String to parse
   * @return an array containing a boolean value for each character
   **/
  public static boolean[] isTrueArray(String str) {

      /* Setup */

    str = str.toUpperCase();
    boolean[] arr = new boolean[str.length()];

      /* Determine which characters represent "TRUE" */

    for (int i = 0; i < str.length(); i++)
        arr[i] = (str.charAt(i) == 'T');

    return arr;

  }

  /**
   * Label must consist of only letters, digits, and/or a small set of other special characters.
   * NOTE: Special characters have special meaning attached to them so it is advised not to use them when naming states and events.
   * @param label The label to validate
   * @return whether or not the label is valid
   **/
  private static boolean isValidLabel(String label) {

    // Must be at least one character long
    if (label.length() < 1)
      return false;

    // All characters must be either letters, digits, or one of the allowed special characters
    for (int i = 0; i < label.length(); i++)
      if (!Character.isLetterOrDigit(label.charAt(i))
          && label.charAt(i) != '_'
          && label.charAt(i) != '*'
          && label.charAt(i) != '<'
          && label.charAt(i) != '>')
        return false;

    return true;

  }

}