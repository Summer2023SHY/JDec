/**
 * AutomatonGenerator - Abstract class used to generated automata. Automata can be generated using GUI input code, or they
 *                      can be randomly generated (with a number of specified properties).
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Random Automaton Generation (and associated helper methods)
 *  -Automaton Generation from GUI Input Code (and associated helper methods)
 **/

import java.util.*;
import java.io.*;

public abstract class AutomatonGenerator {

    /** RANDOM AUTOMATON GENERATION (AND ASSOCIATED HELPER METHODS) **/

  /**
   * Generate a random automaton with the specified properties.
   * NOTE: Generated automaton is not guaranteed to be accessible, co-accessible, controllable, or even connected.
   * @param fileName                The name of the file where the automaton will be stored (excluding the extension)
   * @param nEvents                 The number of events to be generated in the automaton
   * @param nStates                 The number of states to be generated in the automaton
   * @param minTransitionsPerState  The minimum number of outgoing transitions per state
   * @param maxTransitionsPerState  The maximum number of outgoing transitions per state
   * @param nControllers            The number of controllers in the automaton
   * @param nBadTransitions         The number of bad transition in the automaton
   **/
  public static Automaton generateRandom(String fileName, int nEvents, long nStates, int minTransitionsPerState, int maxTransitionsPerState, int nControllers, int nBadTransitions) {

      /* Create empty automaton with capacities that should prevent the need to re-create the body file */

    Automaton automaton = new Automaton(
      new File(fileName + ".hdr"),
      new File(fileName + ".bdy"),
      nEvents,
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

      int id = automaton.addEvent(generateEventLabel(i, nEvents), observability, controllability);
      if (id == 0)
          System.err.println("ERROR: Event could not be added.");

    }

      /* Generate states */
    {
      long initialStateID = generateLong(1, nStates);
      for (int i = 1; i <= nStates; i++) {
        long id = automaton.addState(String.valueOf(i), generateBoolean(), i == initialStateID);
        if (id == 0)
          System.err.println("ERROR: State could not be added.");
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

  /**
   * Give a unique name to the event, based on its ID and the largest possible ID (which is used to calculate how many letters are needed).
   * @param id    ID of the event
   * @param maxID Largest possible ID in this event set
   * @return the generated event label
   **/
  private static String generateEventLabel(int id, int maxID) {

    String label = "";

    // It's easier if they are 0-based, not 1-based
    id--;
    maxID--;

    while (maxID > 0) {
      label = (char) ((id % 26) + 97) + label;
      id /= 26;
      maxID /= 26;
    }

    return label;

  }

  /**
   * Generate random long value in the given range (inclusive).
   * @param min Minimum value
   * @param max Maximum value
   * @param random long value
   **/
  private static long generateLong(long min, long max) {

    return min + (long) (Math.random() * ((double) (max - min + 1)));

  }

  /**
   * Generate random integer value in the given range (inclusive).
   * @param min Minimum value
   * @param max Maximum value
   * @param random integer value
   **/
  private static int generateInt(int min, int max) {

    return min + (int) (Math.random() * ((double) (max - min + 1)));

  }

  /**
   * Generate random boolean.
   * @param random boolean value
   **/
  private static boolean generateBoolean() {

    return generateInt(0, 1) == 1;

  }

    /** AUTOMATON GENERATION FROM GUI INPUT CODE (AND ASSOCIATED HELPER METHODS) **/

  /**
   * Generate an automaton using the given GUI input code.
   * @param eventInputText      The event input text
   * @param stateInputText      The state input text
   * @param transitionInputText The transitionsInputText
   * @param nControllers        The number of controllers in the automaton
   * @param verbose             Whether or not parsing errors should be printed to the console
   * @param headerFile          The header file where the automaton will be written to
   * @return the generated automaton
   **/
  public static Automaton generateFromGUICode(String eventInputText, String stateInputText, String transitionInputText, int nControllers, boolean verbose, File headerFile) {

      /* Setup */
    
    Automaton automaton = new Automaton(headerFile, nControllers);
    HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events' labels to the events' ID
    HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the states' labels to the state's ID

      /* States */
    
    for (String line : stateInputText.split("\n")) {

      String[] splitLine = line.trim().split(",");
      String label = trimStateLabel(splitLine[0], Automaton.MAX_LABEL_LENGTH, verbose);

      // Check to see if this is a duplicate state label
      if (stateMapping.get(label) != null) {
        if (verbose)
          System.err.println("ERROR: Could not store '" + line + "' as a state, since there is already a state with this label.");
        continue;
      }

      // Try to add the state
      if (splitLine.length >= 1 && label.length() > 0) {

        boolean isInitialState = (label.charAt(0) == '@');

        // Ensure the user didn't only have a '@' symbols the name of the label (since '@' gets removed, we are left with an empty string)
        if (isInitialState && label.length() == 1) {
          if (verbose)
            System.err.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");
          continue;

        }

        // Remove '@' from the label name
        if (isInitialState)
          label = label.substring(1);

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.err.println("ERROR: Invalid label ('" + label + "').");
          continue;
        }

        long id = automaton.addState(label, splitLine.length < 2 || isTrue(splitLine[1]), isInitialState);

        // Error checking
        if (id == 0) {
          if (verbose)
            System.err.println("ERROR: Could not store '" + line + "' as a state.");
          continue;
        }

        // Add state
        stateMapping.put(label, id);

      } else if (line.length() > 0 && verbose)
        System.err.println("ERROR: Could not parse '" + line + "' as a state.");
    }
    
      /* Events */

    for (String line : eventInputText.split("\n")) {

      String[] splitLine = line.trim().split(",");
      String label = splitLine[0];

      // Check to see if this is a duplicate event label
      if (eventMapping.get(label) != null) {
        if (verbose)
          System.err.println("ERROR: Could not store '" + line + "' as an event, since there is already an event with this label.");
        continue;
      }

      // Try to add the event
      if (splitLine.length >= 1 && label.length() > 0) {

        // Setup (properties are true by default)
        boolean[] observable = new boolean[nControllers];
        boolean[] controllable = new boolean[nControllers];
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

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.err.println("ERROR: Invalid label ('" + label + "').");
          continue;
        }

        // Try to add event to automaton
        int id = automaton.addEvent(label, observable, controllable);

        // Error checking
        if (id == 0) {
          if (verbose)
            System.err.println("ERROR: Could not store '" + line + "' as an event.");
          continue;
        }


        // Add event
        eventMapping.put(label, id);

      } else if (line.length() > 0 && verbose)
        System.err.println("ERROR: Could not parse '" + line + "' as an event.");

    }

      /* Transitions */

    for (String line : transitionInputText.split("\n")) {

      String[] splitLine = line.trim().split(":");

      // Ensure that all 3 required parameters are present
      String[] firstHalf = splitLine[0].split(",");
      if (firstHalf.length >= 3) {

        String initialStateLabel = trimStateLabel(firstHalf[0].trim(), Automaton.MAX_LABEL_LENGTH, verbose);
        String eventLabel = firstHalf[1].trim();
        String targetStateLabel = trimStateLabel(firstHalf[2].trim(), Automaton.MAX_LABEL_LENGTH, verbose);

        // Get ID's of initial state, event, and target state
        Long initialStateID = stateMapping.get(initialStateLabel);
        Integer eventID = eventMapping.get(eventLabel);
        Long targetStateID = stateMapping.get(targetStateLabel);

        // Prevent crashing by checking to see if any of the values are null (indicates that they've entered a state or event that doesn't exist)
        if (initialStateID == null || eventID == null || targetStateID == null) {
          if (verbose)
            System.err.println("ERROR: Could not store '" + line + "' as a transition.");
        }

        // Add transition
        else {
         
          if (automaton.addTransition(initialStateID, eventID, targetStateID)) {
            if (splitLine.length > 1)
              parseAndAddSpecialTransitions(automaton, splitLine[1], new TransitionData(initialStateID, eventID, targetStateID));
          } else
            System.err.println("ERROR: Could not add '" + line + "' as a transition.");
        }
        
      } else if (line.length() > 0 && verbose)
        System.err.println("ERROR: Could not parse '" + line + "' as a transition.");
    }

    return automaton;
  
  }

  /**
   * Parse a string for special transitions, adding them to the automaton
   * @param automaton The automaton to add the special transitions to
   * @param line      The text to parse
   * @param data      The transition data (IDs of the associated event and states)
   **/
  private static void parseAndAddSpecialTransitions(Automaton automaton, String line, TransitionData data) {

    String[] split = line.split(",");

    // Parse each special transition
    for (String str : split) {

      str = str.trim();
      
      if (str.equals("BAD"))
        automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);
      
      else if (str.equals("UNCONDITIONAL_VIOLATION"))
        automaton.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
      
      else if (str.equals("CONDITIONAL_VIOLATION"))
        automaton.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
      
      else {

        String[] parts = str.split("-");
        
        if (parts[0].equals("POTENTIAL_COMMUNICATION") && parts.length == 2)
          automaton.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, parseCommunicationRoles(parts[1]));
        else
          System.err.println("ERROR: Could not parse '" + line + "' as special transition information.");

      }

    }

  }

  /**
   * Given a string, parse each character as a communication role, and return the generated array.
   * @param str The string representing the sequence of communication roles
   * @return the array of communication roles
   **/
  private static CommunicationRole[] parseCommunicationRoles(String str) {

    CommunicationRole[] roles = new CommunicationRole[str.length()];
    
    // Parse the roles one by one
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch == 'S' || ch == 's')
        roles[i] = CommunicationRole.SENDER;
      else if (ch == 'R' || ch == 'r')
        roles[i] = CommunicationRole.RECIEVER;
      else if (ch == '*')
        roles[i] = CommunicationRole.NONE;
      else
        System.err.println("ERROR: Unable to parse '" + ch + "'as a communication role.");
    }

    return roles;

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

  /**
   * Trim down the string to the desired length by removing characters from the end.
   * @param str     The string that needs to be trimmed
   * @param length  The desired length
   * @param verbose Whether or not a message should be printed if a label is trimmed
   * @return the trimmed string (or the original if it doesn't exceed the desired length)
   **/
  private static String trimStateLabel(String str, int length, boolean verbose) {

    if (str.length() <= length)
      return str;

    String trimmed = str.substring(0, length);
    
    if (verbose)
      System.err.println(String.format("NOTE: State labels must be %d characters or less. '%s' was trimmed to '%s'.", Automaton.MAX_LABEL_LENGTH, str, trimmed));

    return trimmed;

  }

}