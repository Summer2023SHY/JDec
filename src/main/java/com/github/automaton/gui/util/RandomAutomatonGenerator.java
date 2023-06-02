package com.github.automaton.gui.util;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.awt.EventQueue;
import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.Automaton;
import com.github.automaton.automata.State;
import com.github.automaton.automata.Transition;
import com.github.automaton.gui.RandomAutomatonPrompt;

/**
 * Utility class used to generate random automata (with a number of specified properties).
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.1
 * @revised 2.0
 */
public class RandomAutomatonGenerator {
  /** Private constructor */
  private RandomAutomatonGenerator() {}

    /* RANDOM AUTOMATON GENERATION */

  /**
   * Generate a random automaton with the specified properties. The generated automaton is guaranteed to
   * be both observable, controllable, and accessible, co-accessible.
   * NOTE: This process is terminated
   * @param prompt                  A reference to the prompt that started this process
   * @param nEvents                 The number of events to be generated in the automaton
   * @param nStates                 The number of states to be generated in the automaton
   * @param minTransitionsPerState  The minimum number of outgoing transitions per state
   * @param maxTransitionsPerState  The maximum number of outgoing transitions per state
   * @param nControllers            The number of controllers in the automaton
   * @param nBadTransitions         The number of bad transition in the automaton
   * @param progressIndicator       The progress indicator to be updated during the generation process
   * @return                        The randomly generated automaton (or {@code null} if the process was aborted)
   * 
   * @since 2.0
   **/
  public static Automaton generateRandom(RandomAutomatonPrompt prompt,
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

      automaton = new Automaton(nControllers);

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
        updateProgressIndicator(progressIndicator, "Flipping a transition...", nAttempts);
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
        Automaton accessibleAutomaton = automaton.accessible();
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
    
    //if (headerFile == null || bodyFile == null)
    //  return automaton;

    return automaton;

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

}
