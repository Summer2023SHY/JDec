/**
 * AutomatonGenerator - Abstract class used to generate automata. Automata can be generated using GUI input
 *                      code, or they can be randomly generated (with a number of specified properties).
 *
 *                      NOTE: This class could realistically be split into two unrelated classes.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Static Error Styling Properties
 *  -Random Automaton Generation
 *  -Automaton Generation from GUI Input Code
 **/

import java.awt.Color;
import java.awt.EventQueue;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public abstract class AutomatonGenerator<T> {

      /* STATIC ERROR STYLING PROPERTIES */

    private static final StyleContext styleContext = new StyleContext();
    private static final Style errorStyle, normalStyle;

    static {

      errorStyle = styleContext.addStyle("InvalidSyntax", null);
      StyleConstants.setForeground(errorStyle, Color.red);

      normalStyle = styleContext.addStyle("ValidSyntax", null);
      StyleConstants.setForeground(normalStyle, Color.black);
    
    }

    /* RANDOM AUTOMATON GENERATION */

  /**
   * Generate a random automaton with the specified properties. The generated automaton is guaranteed to
   * be both observable, controllable, and accessible, co-accessible.
   * NOTE: This process is terminated
   * @param prompt                  A reference to the prompt that started this process
   * @param headerFile              The name of the header file where the automaton will be stored
   * @param bodyFile                The name of the body file where the automaton will be stored
   * @param nEvents                 The number of events to be generated in the automaton
   * @param nStates                 The number of states to be generated in the automaton
   * @param minTransitionsPerState  The minimum number of outgoing transitions per state
   * @param maxTransitionsPerState  The maximum number of outgoing transitions per state
   * @param nControllers            The number of controllers in the automaton
   * @param nBadTransitions         The number of bad transition in the automaton
   * @param progressIndicator       The progress indicator to be updated during the generation process
   * @return                        The randomly generated automaton (or null if the process was aborted)
   **/
  public static Automaton generateRandom(RandomAutomatonPrompt prompt,
                                         File headerFile,
                                         File bodyFile,
                                         int nEvents,
                                         int nStates,
                                         int minTransitionsPerState,
                                         int maxTransitionsPerState,
                                         int nControllers,
                                         int nBadTransitions,
                                         final JLabel progressIndicator) {

    Automaton automaton = null;
    int nAttempts = 0;

    // Repeat generation until it passes both the observability and controllability tests
    while (prompt == null || !prompt.isDisposed) {

      nAttempts++;

      updateProgressIndicator(progressIndicator, "Creating empty automaton...", nAttempts);

      int nextStateLabel = 1; 
      int initialStateID = generateInt(1, nStates);

        /* Create empty automaton with capacities that should prevent the need to re-create the body file */

      automaton = new Automaton(
        null,
        null,
        nEvents,
        nStates,
        maxTransitionsPerState,
        String.valueOf(nStates).length(),
        nControllers,
        true
      );

        /* Generate events */

      updateProgressIndicator(progressIndicator, "Adding events...", nAttempts);
      for (int i = 1; i <= nEvents; i++)
        automaton.addEvent(
          generateEventLabel(i, nEvents),
          generateBooleanArray(nControllers),
          generateBooleanArray(nControllers)
        );
        
        /* Keep adding states and transitions until the automaton is entirely accessible */

      boolean isAccessible = false;
      while (!isAccessible) {

        // Generate new states
        updateProgressIndicator(progressIndicator, "Adding states...", nAttempts);
        int nPreExistingStates = (int) automaton.getNumberOfStates();
        for (int s = nPreExistingStates + 1; s <= nStates; s++)
          automaton.addStateAt(String.valueOf(nextStateLabel++), true, null, s == initialStateID, s);

        // Generate transitions
        updateProgressIndicator(progressIndicator, "Adding transitions...", nAttempts);
        for (int s = nPreExistingStates + 1; s <= nStates; s++) {

          // Choose a random number of transitions
          int nTransitions = generateInt(minTransitionsPerState, maxTransitionsPerState);

          // Add transitions
          for (int i = 0; i < nTransitions; i++)
            addTransition(automaton, s, generateInt(1, nStates));

        }

        // If possible, flip one random transition between the new states and the accessible automaton
        // NOTE: A new event will be generated for the flipped transition
        updateProgressIndicator(progressIndicator, "Flipping transitions...", nAttempts);
        if (nPreExistingStates > 0)
          outer: for (int s1 = nPreExistingStates + 1; s1 <= nStates; s1++) {

            State state1 = automaton.getState(s1);

            for (Transition transition : state1.getTransitions()) {

              int s2 = (int) transition.getTargetStateID();

              // Skip this transition if it does not lead to the accessible automaton
              if (s2 > nPreExistingStates)
                continue;

              State state2 = automaton.getState(s2);

              // We cannot flip the transition if the other state is already maxed out
              if (state2.getTransitions().size() == maxTransitionsPerState)
                continue;

              // Flip the transition
              automaton.removeTransition(s1, transition.getEvent().getID(), s2);
              addTransition(automaton, s2, s1);
              break outer;
            
            }

          }

        // Add bad transitions
        updateProgressIndicator(progressIndicator, "Marking bad transitions...", nAttempts);
        while (automaton.getBadTransitions().size() < nBadTransitions) {

          // Continue to look for a bad transition to mark until we find one
          // NOTE: As long as maxTransitionsPerState * nStates is not less than nBadTransitions, then this
          //       loop will terminate at some point
          outer: while (true) {

            int s = generateInt(1, nStates);
            List<Transition> transitions = automaton.getState(s).getTransitions();

            // Go through the transitions sequentially until we find one to mark as bad
            for (Transition transition : transitions) {

              // Mark this one as bad, as long as it isn't already a bad transition
              if (!automaton.isBadTransition(s, transition.getEvent().getID(), transition.getTargetStateID())) {
                automaton.markTransitionAsBad(s, transition.getEvent().getID(), transition.getTargetStateID());
                break outer;
              }
            
            }

            // If we have gotten this far, we should add a new transition to this state, marking it bad
            // NOTE: We cannot do this if the maximum number of transitions has already been exceeded
            if (transitions.size() < maxTransitionsPerState) {
              int targetStateID = generateInt(1, nStates);
              int eventID = addTransition(automaton, s, targetStateID);
              automaton.markTransitionAsBad(s, eventID, targetStateID);
            }

          }

        }

          /* Check for Accessibility */

        updateProgressIndicator(progressIndicator, "Checking accessibility...", nAttempts);
        Automaton accessibleAutomaton = automaton.accessible(null, null);
        if (accessibleAutomaton.getNumberOfStates() == nStates)
          isAccessible = true;

        automaton = accessibleAutomaton;

      }

        /* Test properties */

      // If the observability or controllability properties are both satisfied, then we are done
      // NOTE: The controllability test is done first since it is less expensive

      updateProgressIndicator(progressIndicator, "Checking controllability...", nAttempts);
      if (automaton.testControllability()) {
        updateProgressIndicator(progressIndicator, "Checking observability...", nAttempts);
        if (automaton.testObservability())
          break;
      }

    }

    // Return null if the prompt was closed
    if ((prompt != null && prompt.isDisposed) || automaton == null)
      return null;

      /* Duplicate the automaton into the requested files */
    
    updateProgressIndicator(progressIndicator, "Returning result...", nAttempts);
    
    if (headerFile == null || bodyFile == null)
      return automaton.duplicate();

    return automaton.duplicate(headerFile, bodyFile);

  }

  /**
   * Add a random transition to the automaton, ensuring that no duplicates are created.
   * NOTE: This method also ensures that the automaton remains deterministic.
   * @param automaton     The automaton that is being added to
   * @param stateID       The ID of the state which is having the transition added
   * @param targetStateID The ID of the state which the added transition will lead to
   * @return              The ID of the random event
   **/
  private static int addTransition(Automaton automaton, int stateID, int targetStateID) {

    int eventID;

    // Ensure that we don't produce any duplicates
    do {
      eventID = generateInt(1, automaton.getNumberOfEvents());
    } while (automaton.transitionExistsWithEvent(stateID, eventID));

    // Add the transition
    automaton.addTransition(stateID, eventID, targetStateID);

    return eventID;

  }

  /**
   * Update the progress indicator.
   * @param progressIndicator The progress indicator that is being updated
   * @param text              The new text for the progress indicator
   * @param nAttempts         The attempt number
   **/
  private static void updateProgressIndicator(final JLabel progressIndicator, final String text, final int nAttempts) {
    
    if (progressIndicator != null) {
      
      EventQueue.invokeLater(new Runnable() {
        @Override public void run() {
          progressIndicator.setText("Attempt #" + nAttempts  + ": " + text);
          progressIndicator.repaint();
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

    // It's easier to calculate if they are 0-based, not 1-based
    id--;
    maxID--;

    // Build the label character by character
    while (maxID > 0) {
      label = (char) ((id % 26) + 97) + label;
      id /= 26;
      maxID /= 26;
    }

    return label;

  }

  /**
   * Generate a random integer value in the specified range (inclusive).
   * @param min Minimum value
   * @param max Maximum value
   * @return    The randomly generated integer value       
   **/
  private static int generateInt(int min, int max) {

    return min + (int) (Math.random() * ((double) (max - min + 1)));

  }

  /**
   * Generate a random boolean value.
   * @return  The randomly generated boolean value
   **/
  private static boolean generateBoolean() {

    return generateInt(0, 1) == 1;

  }

  /**
   * Generate an array filled with random boolean values.
   * @param size  The number of boolean values to be generated
   * @return      The array of random boolean values
   **/
  private static boolean[] generateBooleanArray(int size) {

    boolean[] arr = new boolean[size];

    for (int i = 0; i < size; i++)
      arr[i] = generateBoolean();

    return arr;

  }

    /* AUTOMATON GENERATION FROM GUI INPUT CODE */

  /**
   * Generate an automaton using the given GUI input code in the form of a string.
   * @param automaton           The empty automaton in which the generated data will be inserted
   * @param eventInputText      The event input text
   * @param stateInputText      The state input text
   * @param transitionInputText The transitionsInputText
   * @return                    The generated automaton
   **/
  public static <T extends Automaton> T generateFromGUICode(T automaton,
                                                            String eventInputText,
                                                            String stateInputText,
                                                            String transitionInputText) {

    return generateFromGUICode(
      automaton,
      eventInputText,
      stateInputText,
      transitionInputText,
      null,
      null,
      null,
      null
    );

  }

  /**
   * Generate an automaton using the given GUI input code. If the text panes are not null, then
   * lines that could not be parsed are stylized red.
   * @param automaton           The empty automaton in which the generated data will be inserted
   * @param eventInputText      The event input text
   * @param stateInputText      The state input text
   * @param transitionInputText The transition input text
   * @param eventInputPane      The text pane containing the event input
   * @param stateInputPane      The text pane containing the state input
   * @param transitionInputPane The text pane containing the transition input
   * @param gui                 If non-null, then error messages will be displayed in a popup 
   * @return                    The generated automaton
   **/
  public static <T extends Automaton> T generateFromGUICode(T automaton,
                                                            String eventInputText,
                                                            String stateInputText,
                                                            String transitionInputText,
                                                            JTextPane eventInputPane,
                                                            JTextPane stateInputPane,
                                                            JTextPane transitionInputPane,
                                                            JDec gui) {
      /* Remove Old Errors */

    if (eventInputPane != null)
      eventInputPane.getStyledDocument().setCharacterAttributes(0, eventInputPane.getDocument().getLength() + 1, normalStyle, false);
    if (stateInputPane != null)
      stateInputPane.getStyledDocument().setCharacterAttributes(0, stateInputPane.getDocument().getLength() + 1, normalStyle, false);
    if (transitionInputPane != null)
      transitionInputPane.getStyledDocument().setCharacterAttributes(0, transitionInputPane.getDocument().getLength() + 1, normalStyle, false);
        
      /* Setup */

    HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events' labels to the events' ID
    HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the states' labels to the state's ID
    Automaton.Type automatonType = automaton.getType();
    boolean hasErrors = false;

      /* States */
    
    int endIndex = 0;
    for (String line : stateInputText.split("\n")) {

      int startIndex = endIndex;
      endIndex += line.length() + 1;

      String[] splitLine = splitStringWithVectors(line);
      String label = splitLine[0].trim();
      boolean marked = (splitLine.length >= 2 && isTrue(splitLine[1]));

      // Check to see if this is a duplicate state label
      if (stateMapping.get(label) != null) {
        System.err.println("ERROR: Could not store '" + line + "' as a state, since there is already a state with this label.");
        if (stateInputPane != null)
          stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, splitLine[0].length(), errorStyle, false);
        hasErrors = true;
        continue;
      }

      // Try to add the state
      if (label.length() > 0) {

        boolean isInitialState = (label.charAt(0) == '@');

        if (isInitialState) {
          
          // Ensure that there isn't already an initial state specified
          if (automaton.getInitialStateID() != 0) {
            if (stateInputPane != null)
              stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, 1, errorStyle, false);
            hasErrors = true;
            continue;
          }

          // Ensure the user didn't only have a '@' symbol as the name of the label (since '@' gets removed, we are left with an empty string)
          if (label.length() == 1) {
            System.err.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");
            if (stateInputPane != null)
              stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, splitLine[0].length(), errorStyle, false);
            hasErrors = true;
            continue;
          }

          // Remove '@' character from the label name
          label = label.substring(1);
        
        }

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.err.println("ERROR: Invalid label ('" + label + "').");
          if (stateInputPane != null)
            stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, splitLine[0].length(), errorStyle, false);
          hasErrors = true;
          continue;
        }

        long id = automaton.addState(label, marked, isInitialState);

        // Check if adding the state was unsuccessful
        if (id == 0) {
          if (gui != null)
            gui.displayErrorMessage("Error", "'" + label + "' could not be added as a state. Please ensure that the label has not exceeded " + Automaton.MAX_LABEL_LENGTH + " characters.");
          if (stateInputPane != null)
            stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
          hasErrors = true;
          continue;
        }

        // Add state
        stateMapping.put(label, id);

      // Otherwise, no text was entered for the label
      } else if (line.length() > 0) {
        System.err.println("ERROR: Could not store'" + line + "' as a state. The label must be at least 1 character long.");
        if (stateInputPane != null)
          stateInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
        hasErrors = true;    
      }

    }
    
      /* Events */

    endIndex = 0;
    for (String line : eventInputText.split("\n")) {

      int startIndex = endIndex;
      endIndex += line.length() + 1;
        
      String[] splitLine = splitStringWithVectors(line);

      if (splitLine == null) {
        System.err.println("ERROR: Could not store '" + line + "' as an event, the vectors could not be parsed properly.");
        if (eventInputPane != null)
          eventInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
        hasErrors = true;
        continue;
      }

      String label = splitLine[0].trim();

      // Check to see if this is a duplicate event label
      if (eventMapping.get(label) != null) {
        System.err.println("ERROR: Could not store '" + line + "' as an event, since there is already an event with this label.");
        if (eventInputPane != null)
          eventInputPane.getStyledDocument().setCharacterAttributes(startIndex, splitLine[0].length(), errorStyle, false);
        hasErrors = true;
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
            observable  = isTrueArray(splitLine[1]);
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
            if (eventInputPane != null)
              eventInputPane.getStyledDocument().setCharacterAttributes(startIndex + splitLine[0].length() + 1, line.length() - splitLine[0].length() - 1, errorStyle, false);
            hasErrors = true;
            continue;
          }
        }

        // Check for invalid label
        if (!isValidLabel(label)) {
          System.err.println("ERROR: Invalid label ('" + label + "').");
          if (eventInputPane != null)
            eventInputPane.getStyledDocument().setCharacterAttributes(startIndex, splitLine[0].length(), errorStyle, false);
          hasErrors = true;
          continue;
        }

        // Try to add event to automaton
        int id = automaton.addEvent(label, observable, controllable);

        // Error checking
        if (id == 0) {
          if (gui != null)
            gui.displayErrorMessage("Error", "'" + label + "' could not be added as an event. Please ensure that there are not more than " + Automaton.MAX_EVENT_CAPACITY + " events.");
          if (eventInputPane != null)
            eventInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
          hasErrors = true;
          continue;
        }


        // Add event
        eventMapping.put(label, id);

      } else if (line.length() > 0) {
        System.err.println("ERROR: Could not parse '" + line + "' as an event.");
        if (eventInputPane != null)
          eventInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
        hasErrors = true;
        continue;
      }

    }

      /* Transitions */

    endIndex = 0;
    for (String line : transitionInputText.split("\n")) {

      int startIndex = endIndex;
      endIndex += line.length() + 1;

      String[] splitLine = line.trim().split(":");

      // Ensure that all 3 required parameters are present
      String[] firstHalf = splitStringWithVectors(splitLine[0]);
      if (firstHalf.length >= 3) {

        // Get ID's of initial state, event, and target state
        Long initialStateID = stateMapping.get(firstHalf[0].trim());
        Integer eventID = eventMapping.get(firstHalf[1].trim());
        Long targetStateID = stateMapping.get(firstHalf[2]);

        // Prevent crashing by checking to see if any of the values are null (indicates that they've entered a state or event that doesn't exist)
        if (initialStateID == null || eventID == null || targetStateID == null) {
          System.err.println("ERROR: Could not store '" + line + "' as a transition due to bad state and/or event labels.");
          if (transitionInputPane != null)
            transitionInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
          hasErrors = true;
          continue;

        // Add transition
        } else {
         
          if (automaton.addTransition(initialStateID, eventID, targetStateID)) {
            if (splitLine.length > 1)
              if (!parseAndAddSpecialTransitions(automaton, automatonType, splitLine[1], new TransitionData(initialStateID, eventID, targetStateID))) {
                if (transitionInputPane != null)
                  transitionInputPane.getStyledDocument().setCharacterAttributes(splitLine[0].length() + 1, line.length() - splitLine[0].length() - 1, errorStyle, false);
                hasErrors = true;
              }
          } else {
            if (gui != null)
              gui.displayErrorMessage("Error", "'" + line + "' could not be added as a transition. Please ensure that there are not more than " + Automaton.MAX_TRANSITION_CAPACITY + " transitions.");
            if (transitionInputPane != null)
              transitionInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
            hasErrors = true;
            continue;
          }
        }
        
      } else if (line.length() > 0) {
        System.err.println("ERROR: Could not parse '" + line + "' as a transition.");
        if (transitionInputPane != null)
          transitionInputPane.getStyledDocument().setCharacterAttributes(startIndex, line.length(), errorStyle, false);
        hasErrors = true;
        continue;
      }
    }

      /* Display message if there were any errors */

    if (hasErrors && gui != null) {
      gui.displayErrorMessage("Error", "There were one or more lines of input code that were unable to be parsed.\nPlease fix all lines marked in red and then try re-generating it.");
      return null;
    }
      /* Ensure that the header file has been written to disk */
      
    automaton.writeHeaderFile();

    return automaton;
  
  }

  /**
   * Parse a string for special transitions, adding them to the automaton.
   * @param automaton     The automaton to add the special transitions to
   * @param automatonType The enum value associated with the type of the automaton
   * @param line          The text to parse
   * @param data          The transition data (IDs of the associated event and states)
   * @return              Whether or not the special transition properties could be parsed
   **/
  private static <T extends Automaton> boolean parseAndAddSpecialTransitions(T automaton, Automaton.Type automatonType, String line, TransitionData data) {

    boolean valid = true;

    String[] split = line.split(",");

    // Parse each special transition
    for (String str : split) {

      str = str.trim();
      
      // Only applies to automata
      if (automatonType == Automaton.Type.AUTOMATON) {

        if (str.equals("BAD")) {
          automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }

      }
      
      // Applies to U-Structures and pruned U-Structures
      if (automatonType == Automaton.Type.U_STRUCTURE || automatonType == Automaton.Type.PRUNED_U_STRUCTURE) {

        UStructure uStructure = (UStructure) automaton;

        if (str.equals("UNCONDITIONAL_VIOLATION")) {
          uStructure.addUnconditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }
        
        if (str.equals("CONDITIONAL_VIOLATION")) {
          uStructure.addConditionalViolation(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }
    

      }

      // Only applies to U-Structures
      if (automatonType == Automaton.Type.U_STRUCTURE) {

        UStructure uStructure = (UStructure) automaton;

        if (str.equals("INVALID_COMMUNICATION")) {
          uStructure.addInvalidCommunication(data.initialStateID, data.eventID, data.targetStateID);
          continue;
        }

        String[] parts = str.split("-");

        if (parts[0].equals("POTENTIAL_COMMUNICATION") && parts.length == 2) {
          uStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, parseCommunicationRoles(parts[1]));
          continue;
        }

      }

      // Only applies to pruned U-Structures
      if (automatonType == Automaton.Type.PRUNED_U_STRUCTURE) {

        PrunedUStructure prunedUStructure = (PrunedUStructure) automaton;
        String[] parts = str.split("-");

        if (parts[0].equals("COMMUNICATION") && parts.length == 2) {
          prunedUStructure.addPotentialCommunication(data.initialStateID, data.eventID, data.targetStateID, parseCommunicationRoles(parts[1]));
          continue;
        }

      }

      // Applies to U-Structures, pruned U-Structures, and Crushes
      if (automatonType == Automaton.Type.U_STRUCTURE || automatonType == Automaton.Type.PRUNED_U_STRUCTURE || automatonType == Automaton.Type.CRUSH) {

        UStructure uStructure = (UStructure) automaton;

        String[] parts = str.split("-");

        if (parts[0].equals("DISABLEMENT_DECISION") && parts.length == 2) {
          boolean[] controllers = new boolean[parts[1].length()];
          for (int i = 0; i < controllers.length; i++)
            controllers[i] = isTrue(parts[1].substring(i, i + 1));
          uStructure.addDisablementDecision(data.initialStateID, data.eventID, data.targetStateID, controllers);  
          continue;
        }


        if (parts[0].equals("NASH_COMMUNICATION") && parts.length == 4) {
          
          try {
            uStructure.addNashCommunication(
              data.initialStateID,
              data.eventID,
              data.targetStateID,
              parseCommunicationRoles(parts[1]),
              Double.valueOf(parts[2]),
              Double.valueOf(parts[3])
            );
            continue;
          } catch (NumberFormatException e) { }

        }

      }

      valid = false;
      System.err.println("ERROR: Unable to parse '" + str + "' as a special transition identifier.");

    } // for

    return valid;

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

  /**
   * Given a string that may contain vectors, split the string by commas (without breaking vectors).
   * @param str The string to split
   * @return    The array of split strings, or null if the number of angled brackets did not match up
   **/  
  public static String[] splitStringWithVectors(String str) {

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

    // Return null if the number of angled brackets did not match up
    if (insideVector != 0)
      return null;

    list.add(str.substring(start, str.length()));

    return list.toArray(new String[list.size()]);

  }

  /**
   * Simple helper method to detect whether the given String is either "T" or "t".
   * @param str The String to parse
   * @return    Whether or not the String represents "TRUE" 
   **/
  public static boolean isTrue(String str) {
      return str.toUpperCase().equals("T");
  }

  /**
   * Simple helper method to transform a series of T's and F's into a boolean array.
   * @param str The String to parse
   * @return    An array containing a boolean value for each character
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
   * Label must consist of only letters, digits, and/or a small set of other special characters. If the
   * label is a vector, then it must also be a valid vector.
   * NOTE: Special characters have special meaning attached to them so it is advised not to use them when
   * naming states and events.
   * @param label The label to validate
   * @return      Whether or not the label is valid
   **/
  public static boolean isValidLabel(String label) {

    // Must be at least one character long
    if (label.length() < 1)
      return false;

    boolean isVector = false;

    // All characters must be either letters, digits, or one of the allowed special characters
    for (int i = 0; i < label.length(); i++) {

      boolean valid = false;

      if (Character.isLetterOrDigit(label.charAt(i)) || label.charAt(i) == '_' || label.charAt(i) == '*' || label.charAt(i) == '\'')
        valid = true;

      if (label.charAt(i) == ',' || label.charAt(i) == '<' || label.charAt(i) == '>') {
        valid = true;
        isVector = true;
      }

      if (!valid)
        return false;

    }

    // If this label contains characters that indicate it is a vector, ensure that the vector is valid
    if (isVector) {
      LabelVector vector = new LabelVector(label);
      return vector.getSize() != -1;
    }

    return true;

  }

}