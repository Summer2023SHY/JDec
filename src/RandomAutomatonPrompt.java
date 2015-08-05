/**
 * RandomAutomatonPrompt - Displays a popup box which allows the user to specify the properties of a random
 *                         automaton that they would like generated. The appropriate actions are also
 *                         triggered when the user presses the "Generate" button in the popup.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS
 *  -Class Constants
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class RandomAutomatonPrompt extends JDialog {

    /* CLASS CONSTANTS */

  // Default values
  private static int nControllersDefault     = 1;
  private static int nEventsDefault          = 4;
  private static int nStatesDefault          = 10;
  private static int minTransitionsDefault   = 1;
  private static int maxTransitionsDefault   = 3;
  private static int nBadTransitionsDefault  = 1;
  // private static boolean observableDefault   = true;
  // private static boolean controllableDefault = true;

    /* INSTANCE VARIABLES */

  private JDec gui;
  private JSpinner nControllers, nEvents, nStates, minTransitions, maxTransitions, nBadTransitions;
  // private JCheckBox observableCheckBox, controllableCheckBox;

    /* CONSTRUCTOR */

  /**
   * Construct a RandomAutomatonPrompt object.
   * @param gui A reference to the GUI which will be recieving the generated automaton request.
   **/
  public RandomAutomatonPrompt(JDec gui) {

    super(gui, true);        

    this.gui = gui;

    addComponents();
    setGUIproperties();

  }

    /* METHODS */

  /**
   * Add all of the components to the window.
   **/
  private void addComponents() {

      /* Setup */

    setLayout(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

      /* Number of controllers */

    JLabel nControllersLabel = new JLabel(" # Controllers:");
    c.gridx = 0; c.gridy = 0;
    add(nControllersLabel, c);

    nControllers = new JSpinner(new SpinnerNumberModel(nControllersDefault, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
    c.gridx = 1; c.gridy = 0;
    add(nControllers, c);

      /* Number of events */

    JLabel nEventsLabel = new JLabel(" # Events:");
    c.gridx = 0; c.gridy = 1;
    add(nEventsLabel, c);

    nEvents = new JSpinner(new SpinnerNumberModel(nEventsDefault, 0, Automaton.MAX_EVENT_CAPACITY, 1));
    c.gridx = 1; c.gridy = 1;
    add(nEvents, c);

      /* Number of states */

    JLabel nStatesLabel = new JLabel(" # States:");
    c.gridx = 0; c.gridy = 2;
    add(nStatesLabel, c);

    nStates = new JSpinner(new SpinnerNumberModel(nStatesDefault, 1, 1000, 1)); // The observability test is too expensive to deal with more states
    c.gridx = 1; c.gridy = 2;
    add(nStates, c);

      /* Number of transitions */

    JLabel minTransitionsLabel = new JLabel(" Min. # Transitions per State:");
    c.gridx = 0; c.gridy = 3;
    add(minTransitionsLabel, c);

    minTransitions = new JSpinner(new SpinnerNumberModel(minTransitionsDefault, 0, Automaton.MAX_TRANSITION_CAPACITY, 1));
    c.gridx = 1; c.gridy = 3;
    add(minTransitions, c);

    JLabel maxTransitionsLabel = new JLabel(" Max. # Transitions per State:");
    c.gridx = 0; c.gridy = 4;
    add(maxTransitionsLabel, c);

    maxTransitions = new JSpinner(new SpinnerNumberModel(maxTransitionsDefault, 1, Automaton.MAX_TRANSITION_CAPACITY, 1));
    c.gridx = 1; c.gridy = 4;
    add(maxTransitions, c);

      /* Bad transitions */

    JLabel nBadTransitionsLabel = new JLabel(" # Bad Transitions:");
    c.gridx = 0; c.gridy = 5;
    add(nBadTransitionsLabel, c);

    nBadTransitions = new JSpinner(new SpinnerNumberModel(nBadTransitionsDefault, 0, Integer.MAX_VALUE, 1));
    c.gridx = 1; c.gridy = 5;
    add(nBadTransitions, c);

      /* Observability property */

    // JLabel observableLabel = new JLabel(" Observable:");
    // c.gridx = 0; c.gridy = 6;
    // add(observableLabel, c);

    // observableCheckBox = new JCheckBox();
    // observableCheckBox.setSelected(observableDefault);
    // c.gridx = 1; c.gridy = 6;
    // add(observableCheckBox, c);

      /* Controllability property */

    // JLabel controllableLabel = new JLabel(" Controllable:");
    // c.gridx = 0; c.gridy = 7;
    // add(controllableLabel, c);

    // controllableCheckBox = new JCheckBox();
    // controllableCheckBox.setSelected(controllableDefault);
    // c.gridx = 1; c.gridy = 7;
    // add(controllableCheckBox, c);

      /* Progress bar */

    final JProgressBar progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setString("0%");
    progressBar.setStringPainted(true);
    progressBar.setVisible(false);
    c.gridx = 0; c.gridy = 8; c.gridwidth = 2;
    add(progressBar, c);

      /* Cancel button */

    final JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        RandomAutomatonPrompt.this.dispose();
      }
    });
    c.gridx = 0; c.gridy = 8; c.gridwidth = 1;
    add(cancelButton, c);

      /* Generate button */

    final JButton generateButton = new JButton("Generate");
    generateButton.addActionListener(new ActionListener() {
 
      public void actionPerformed(ActionEvent e) {

        final int nEventsValue         = (Integer) nEvents.getValue();
        final int nStatesValue         = (Integer) nStates.getValue();
        final int minTransitionsValue  = (Integer) minTransitions.getValue();
        final int maxTransitionsValue  = (Integer) maxTransitions.getValue();
        final int nControllersValue    = (Integer) nControllers.getValue();
        final int nBadTransitionsValue = (Integer) nBadTransitions.getValue();

        // Ensure that the min. # of transition is not larger than the max. # of transitions
        if (minTransitionsValue > maxTransitionsValue) {
          gui.displayErrorMessage("Invalid Input", "The minimum number of transitions per state cannot be larger than the maximum number of transitions per state.");
          return;
        }

        if (nBadTransitionsValue > nStatesValue * maxTransitionsValue) {
          gui.displayErrorMessage("Invalid Input", "The number of bad transitions cannot be larger than the number of states multiplied by the maximum number of transitions per state.");
          return;
        }

        if (maxTransitionsValue > nEventsValue) {
          gui.displayErrorMessage("Invalid Input", "The maximum number of transitions per state cannot be larger than the number of events since we are dealing with deterministic automata.");
          return;
        }

        progressBar.setVisible(true);
        cancelButton.setVisible(false);
        generateButton.setVisible(false);
        RandomAutomatonPrompt.this.pack();

        // Create a new thread since this can be a long task
        new Thread() {
            public void run() {
              gui.generateRandomAutomaton(
                nEventsDefault         = nEventsValue,
                nStatesDefault         = nStatesValue,
                minTransitionsDefault  = minTransitionsValue,
                maxTransitionsDefault  = maxTransitionsValue,
                nControllersDefault    = nControllersValue,
                nBadTransitionsDefault = nBadTransitionsValue,
                // observableDefault      = observableCheckBox.isSelected(),
                // controllableDefault    = controllableCheckBox.isSelected(),
                progressBar
              );
              RandomAutomatonPrompt.this.dispose();
            }
        }.start();

      }

    });
    c.gridx = 1; c.gridy = 8;
    add(generateButton, c);

  }

  /**
   * Set some default GUI Properties.
   **/
  private void setGUIproperties() {

      /* Pack things in nicely */

    pack();
    
      /* Make it so that the user can not resize the popup box */

    setResizable(false);

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle("Generate Random Automaton");

      /* Show screen */

    setVisible(true);

  }
    
}