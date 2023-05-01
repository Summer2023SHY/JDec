package com.github.automaton.gui;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.Automaton;
import com.github.automaton.automata.State;
import com.github.automaton.automata.Transition;

/**
 * Used to display a pop-up that allows the user to interactively explore the specified automaton.
 *
 * @author Micah Stairs
 */
public class AutomataExplorer extends JDialog {

    /* INSTANCE VARIABLES */

  private JDec gui;
  private Automaton automaton;
  private Automaton invertedAutomaton;
  private Long currentStateID;
  private State currentState;

  private JScrollPane scrollPane;

    /* CONSTRUCTOR */

  /**
   * Construct a FeasibleProtocolOutput object.
   * @param gui               A reference to the GUI
   * @param automaton         The automaton that is being worked with
   * @param title             The title of the popup box
   **/
  public AutomataExplorer(JDec gui,
                          Automaton automaton,
                          String title) {

    super(gui, true);
    if (automaton == null) {
      this.dispose();
      throw new IllegalArgumentException("Automaton to explore cannot be null");
    }
    else if (automaton.getInitialStateID() == 0L) {
      this.dispose();
      throw new IllegalArgumentException("Automaton cannot be explored");
    }
    this.gui = gui;
    this.automaton = automaton;
    invertedAutomaton = automaton.invert();

    setGUIproperties(title);

    changeState(automaton.getInitialStateID());

  }

    /* METHODS */

  private JPanel getTransitionPanel() {

    JPanel transitionPanel = new JPanel();

    // Outgoing transitions
    transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
    transitionPanel.add(new JLabel("Outgoing transitions: "));
    for (Transition t : currentState.getTransitions()) {
      final JButton button = new JButton(t.toString());
      final long targetStateID = t.getTargetStateID();
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          changeState(targetStateID);
          SwingUtilities.invokeLater(new Runnable() {
            public void run() { button.setEnabled(false); }
          });
        }
      });
      transitionPanel.add(button);
    }

    // Incoming transitions
    State currentStateInverted = invertedAutomaton.getState(currentStateID);
    transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
    transitionPanel.add(new JLabel("Incoming transitions: "));
    for (Transition t : currentStateInverted.getTransitions()) {
      final JButton button = new JButton(t.toString());
      final long targetStateID = t.getTargetStateID();
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          changeState(targetStateID);
          SwingUtilities.invokeLater(new Runnable() {
            public void run() { button.setEnabled(false); }
          });
        }
      });
      transitionPanel.add(button);
    }

    transitionPanel.add(new JSeparator(SwingConstants.HORIZONTAL));

    return transitionPanel;

  }

  /**
   * Add all of the components to the window.
   **/
  private void changeState(Long newStateID) {

    if (scrollPane != null)
      remove(scrollPane);

    currentStateID = newStateID;
    currentState = automaton.getState(currentStateID);
    
      /* Setup */
   
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

      /* Add label to hold current state information */

    JLabel currentStateLabel = new JLabel("<html><b>" + currentState.toString() + "</b><br></html>");
    panel.add(currentStateLabel, BorderLayout.NORTH);

      /* Add section for transitions */

    JPanel transitionPanel = getTransitionPanel();
    transitionPanel.setLayout(new BoxLayout(transitionPanel, BoxLayout.Y_AXIS));
    panel.add(transitionPanel, BorderLayout.CENTER);

      /* Set up Button panel */

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

      /* Add Jump To State button */

    JButton jumpToStateButton = new JButton("Jump To State");
    jumpToStateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String stateLabel = JOptionPane.showInputDialog("Enter the state's label:", "");
        for (long s = 1; s <= automaton.getNumberOfStates(); s++) {
          if (stateLabel.equals(automaton.getState(s).getLabel())) {
            changeState(s);
            return;
          }
        }
        JOptionPane.showMessageDialog(gui, "The state '" + stateLabel + "' was unable to be found.");
      }
    });
    buttonPanel.add(jumpToStateButton);

      /* Add dismiss button */

    JButton dismissedButton = new JButton("Dismiss");
    dismissedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        AutomataExplorer.this.dispose();
      }
    });
    buttonPanel.add(dismissedButton);

    panel.add(buttonPanel, BorderLayout.SOUTH);

    scrollPane = new JScrollPane(panel);
    add(scrollPane);

    revalidate();
    pack();

  }

  /**
   * Set some default GUI Properties.
   * @param title The title of the pop-up box
   **/
  private void setGUIproperties(final String title) {

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {

          /* Sets screen location in the center of the screen (only works after calling pack) */

        setLocationRelativeTo(null);

          /* Update title */

        setTitle(title);

          /* Show screen */

        setVisible(true);

      }
    });

  }
    
}