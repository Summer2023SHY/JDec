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
import javax.swing.*;
import java.awt.*;

public abstract class AutomatonGenerator<T> {

    /** RANDOM AUTOMATON GENERATION (AND ASSOCIATED HELPER METHODS) **/

  /**
   * Generate a random automaton with the specified properties.
   * NOTE: Generated automaton is not guaranteed to be accessible, co-accessible, controllable, or even connected.
   * @param headerFile              The name of the header file where the automaton will be stored
   * @param bodyFile                The name of the body file where the automaton will be stored
   * @param nEvents                 The number of events to be generated in the automaton
   * @param nStates                 The number of states to be generated in the automaton
   * @param minTransitionsPerState  The minimum number of outgoing transitions per state
   * @param maxTransitionsPerState  The maximum number of outgoing transitions per state
   * @param nControllers            The number of controllers in the automaton
   * @param nBadTransitions         The number of bad transition in the automaton
   * @param progressBar             The progress bar to be updated during the generation process
   * @return                        The randomly generated automaton
   **/
  public static Automaton generateRandom(File headerFile, File bodyFile, int nEvents, long nStates, int minTransitionsPerState, int maxTransitionsPerState, int nControllers, int nBadTransitions, JProgressBar progressBar) {

    long nTotalTasks = (long) nEvents + (nStates * 2) + (long) nBadTransitions;

      /* Create empty automaton with capacities that should prevent the need to re-create the body file */

    Automaton automaton = new Automaton(
      headerFile,
      bodyFile,
      nEvents,
      nStates,
      maxTransitionsPerState,
      String.valueOf(nStates).length(),
      nControllers,
      true
    );

      /* Generate events */

    for (int i = 1; i <= nEvents; i++) {

      // Update progress bar
      updateProgressBar(i, nTotalTasks, progressBar);

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

        // Update progress bar
      updateProgressBar(nEvents + i, nTotalTasks, progressBar);

        long id = automaton.addState(String.valueOf(i), generateBoolean(), i == initialStateID);
        if (id == 0)
          System.err.println("ERROR: State could not be added.");
      }
    }

      /* Generate transitions */

    for (long s = 1; s <= nStates; s++) {

      // Update progress bar
      updateProgressBar(nEvents + nStates + s, nTotalTasks, progressBar);

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

      // Update progress bar
      updateProgressBar(nEvents + (nStates * 2) + i, nTotalTasks, progressBar);

      while (true) {

        long initialStateID = generateLong(1, nStates);
        java.util.List<Transition> transitions = automaton.getState(initialStateID).getTransitions();

        // There needs to be at least one transition
        if (transitions.size() == 0)
          continue;

        Transition transition = transitions.get(generateInt(0, transitions.size() - 1));

        // Mark this one as bad, as long as it isn't already a bad transition
        if (!automaton.isBadTransition(initialStateID, transition.getEvent().getID(), transition.getTargetStateID())) {
          automaton.markTransitionAsBad(initialStateID, transition.getEvent().getID(), transition.getTargetStateID());
          break;
        }

      }

    }

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

    return automaton;

  }

  /**
   * Update the progress bar.
   * @param nTasksComplete  The number of tasks that have already been completed
   * @param nTotalTasks     The total number of tasks that need to be done
   * @param progressBar     The progress bar that is being updated
   **/
  private static void updateProgressBar(long nTasksComplete, long nTotalTasks, final JProgressBar progressBar) {
    
    if (progressBar != null) {

      final int newValue = (int) ((nTasksComplete * 100) / nTotalTasks);

      if (newValue != progressBar.getValue())
        EventQueue.invokeLater(new Runnable() {
          @Override public void run() {
            progressBar.setString(newValue + "%");
            progressBar.setValue(newValue);
            progressBar.repaint();
          }
        });
      
    }

  }

  /**
   * Give a unique name to the event, based on its ID and the largest possible ID (which is used to calculate how many letters are needed).
   * @param id    ID of the event
   * @param maxID Largest possible ID in this event set
   * @return      The generated event label
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
   * @return    The randomly generated long value
   **/
  private static long generateLong(long min, long max) {

    return min + (long) (Math.random() * ((double) (max - min + 1)));

  }

  /**
   * Generate random integer value in the given range (inclusive).
   * @param min Minimum value
   * @param max Maximum value
   * @return    The randomly generated integer value       
   **/
  private static int generateInt(int min, int max) {

    return min + (int) (Math.random() * ((double) (max - min + 1)));

  }

  /**
   * Generate random boolean.
   * @return  The randomly generated boolean value
   **/
  private static boolean generateBoolean() {

    return generateInt(0, 1) == 1;

  }

    /** AUTOMATON GENERATION FROM GUI INPUT CODE (AND ASSOCIATED HELPER METHODS) **/

  /**
   * Generate an automaton using the given GUI input code.
   * @param automaton           The empty automaton in which the generated data will be inserted
   * @param eventInputText      The event input text
   * @param stateInputText      The state input text
   * @param transitionInputText The transitionsInputText
   * @param verbose             Whether or not parsing errors should be printed to the console
   * @return                    The generated automaton
   **/
  public static <T extends Automaton> T generateFromGUICode(T automaton, String eventInputText, String stateInputText, String transitionInputText, boolean verbose) {

      /* Setup */

    HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events' labels to the events' ID
    HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the states' labels to the state's ID

      /* States */
    
    for (String line : stateInputText.split("\n")) {

      // Parse input differently depending on automaton type
      String label = null;
      boolean marked;
      if (automaton.getClass().equals(Automaton.class)) {

        String[] splitLine = line.trim().split(",");

        label = trimStateLabel(splitLine[0], Automaton.MAX_LABEL_LENGTH, verbose);
        marked = (splitLine.length >= 2 && isTrue(splitLine[1]));

      } else {
        label = trimStateLabel(line, Automaton.MAX_LABEL_LENGTH, verbose);
        marked = false;
      }

      // Check to see if this is a duplicate state label
      if (stateMapping.get(label) != null) {
        if (verbose)
          System.err.println("ERROR: Could not store '" + line + "' as a state, since there is already a state with this label.");
        continue;
      }

      // Try to add the state
      if (label.length() > 0) {

        boolean isInitialState = (label.charAt(0) == '@');

        // Ensure the user didn't only have a '@' symbol as the name of the label (since '@' gets removed, we are left with an empty string)
        if (isInitialState) {
          
          if (label.length() == 1) {
            if (verbose)
              System.err.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");
            continue;
          }

          // Remove '@' character from the label name
          label = label.substring(1);
        
        }

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.err.println("ERROR: Invalid label ('" + label + "').");
          continue;
        }

        long id = automaton.addState(label, marked, isInitialState);

        // Error checking
        if (id == 0) {
          if (verbose)
            System.err.println("ERROR: Could not store '" + line + "' as a state.");
          continue;
        }

        // Add state
        stateMapping.put(label, id);

      }

    }
    
      /* Events */

    for (String line : eventInputText.split("\n")) {

      String[] splitLine = splitStringWithVectors(line);
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
        boolean[] observable = new boolean[automaton.getNumberOfControllers()];
        boolean[] controllable = new boolean[automaton.getNumberOfControllers()];
        Arrays.fill(observable, true);
        Arrays.fill(controllable, true);

        // Parse controller properties
        if (splitLine.length == 3) {
          if (splitLine[1].length() == automaton.getNumberOfControllers() && splitLine[2].length() == automaton.getNumberOfControllers()) {
            observable = isTrueArray(splitLine[1]);
            controllable = isTrueArray(splitLine[2]);
          } else {
            System.out.println(
              String.format(
                "ERROR: The number of controllers (%d) does not match the number of properties specified (%d and %d).",
                automaton.getNumberOfControllers(),
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
      String[] firstHalf = splitStringWithVectors(splitLine[0]);
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

      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

    return automaton;
  
  }

  /**
   * Parse a string for special transitions, adding them to the automaton
   * @param automaton The automaton to add the special transitions to
   * @param line      The text to parse
   * @param data      The transition data (IDs of the associated event and states)
   **/
  private static <T extends Automaton> void parseAndAddSpecialTransitions(T automaton, String line, TransitionData data) {

    String[] split = line.split(",");

    // Parse each special transition
    for (String str : split) {

      str = str.trim();
      
      // Only applies to automata
      if (automaton.getClass().equals(Automaton.class)) {

        if (str.equals("BAD")) {
          automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }

      }
      
      // Applies to both U-Structures and Nash U-Structures 
      if (automaton.getClass().equals(UStructure.class) || automaton.getClass().equals(NashUStructure.class)) {

        UStructure uStructure = (UStructure) automaton;

        if (str.equals("UNCONDITIONAL_VIOLATION")) {
          uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }
        
        if (str.equals("CONDITIONAL_VIOLATION")) {
          uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }
    
        if (str.equals("COMMUNICATION")) {
          uStructure.addNonPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }

      }

      // Only applies to U-Structures
      if (automaton.getClass().equals(UStructure.class)) {

        UStructure uStructure = (UStructure) automaton;

        String[] parts = str.split("-");

        if (parts[0].equals("POTENTIAL_COMMUNICATION")) {

          if (parts.length == 2)
            uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, parseCommunicationRoles(parts[1]));
          else
            System.err.println("ERROR: Could not parse '" + line + "' as special transition information.");

          continue;

        }

      }

      // Only applies to Nash U-Structures
      if (automaton.getClass().equals(NashUStructure.class)) {

        NashUStructure nashUStructure = (NashUStructure) automaton;

        String[] parts = str.split("-");

        if (parts[0].equals("NASH_COMMUNICATION")) {

          try {

            if (parts.length == 4) {
              nashUStructure.addNashCommunication(
                data.initialStateID,
                data.eventID,
                data.targetStateID,
                parseCommunicationRoles(parts[1]),
                Integer.valueOf(parts[2]),
                Double.valueOf(parts[3])
              );
            }

          } catch (NumberFormatException e) {
            // Do nothing

          } finally {
            System.err.println("ERROR: Could not parse '" + str + "' as as a Nash communication. Please ensure that it is formatted correctly.");
            continue;
          }

        }

      }

      System.err.println("ERROR: Unable to parse '" + str + "' as a special transition identifier.");

    } // for

  }

  /**
   * Given a string, parse each character as a communication role, and return the generated array.
   * @param str The string representing the sequence of communication roles
   * @return    The array of communication roles
   **/
  private static CommunicationRole[] parseCommunicationRoles(String str) {

    CommunicationRole[] roles = new CommunicationRole[str.length()];
    
    // Parse the roles one by one
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if ((roles[i] = CommunicationRole.getRole(ch)) == null)
        System.err.println("ERROR: Unable to parse '" + ch + "'as a communication role.");
    }

    return roles;

  }

  private static String[] splitStringWithVectors(String str) {

    ArrayList<String> list = new ArrayList<String>();

    int start = 0;
    int insideVector = 0;
    for (int i = 0; i < str.length(); i++) {

      char ch = str.charAt(i);

      switch (ch) {
        
        case '<':
          insideVector++;
          break;
        
        case '>':
          insideVector--;
          break;
        
        case ',':
          if (insideVector == 0) {
            list.add(str.substring(start, i));
            start = i + 1;
          }
          break;

      }

    }


    list.add(str.substring(start));

    return list.toArray(new String[0]);

  }

  /**
   * Simple helper method to detect whether the given String is either "T" or "t".
   * @param str   The String to parse
   * @return      Whether or not the String represents "TRUE" 
   **/
  public static boolean isTrue(String str) {
      return str.toUpperCase().equals("T");
  }

  /**
   * Simple helper method to transform a series of T's and F's into a boolean array.
   * @param str   The String to parse
   * @return      An array containing a boolean value for each character
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
   * @return      Whether or not the label is valid
   **/
  private static boolean isValidLabel(String label) {

    // Must be at least one character long
    if (label.length() < 1)
      return false;

    // All characters must be either letters, digits, or one of the allowed special characters
    for (int i = 0; i < label.length(); i++)
      if (!Character.isLetterOrDigit(label.charAt(i))
          && label.charAt(i) != ','
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
   * @return        The trimmed string (or the original if it doesn't exceed the desired length)
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