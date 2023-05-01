package com.github.automaton.gui;

/*
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 **/

import javax.swing.*;

/**
 * Initiates a call to find all Nash equilibria once the user has finished
 * choosing the cost and probability values for the Nash communications.
 *
 * @author Micah Stairs
 * 
 * @deprecated Operations for Nash equilibria depend on
 * {@link com.github.automaton.automata.Crush Crush}. As
 * {@link com.github.automaton.automata.Crush Crush} is deprecated and subject
 * to removal, all Nash equilibria operations are deprecated.
 */
@Deprecated(since="1.1")
public class NashInfoForNashEquilibriaPrompt extends NashInformationPrompt {

    /* CONSTRUCTOR */

  /**
   * Construct a NashInfoForNashEquilibriaPrompt object.
   * @param gui     A reference to the GUI
   * @param tab     The tab that is being worked with
   * @param title   The title of the popup box
   * @param message The text for the label to be displayed at the top of the screen
   **/
  public NashInfoForNashEquilibriaPrompt(JDec gui, JDec.AutomatonTab tab, String title, String message) {

    super(gui, tab, title, message);

  }

    /* OVERIDDEN METHOD */
  /** {@inheritDoc} */
	@Override
  protected void performAction() {

    // Hide this screen, since we will not need to go back to it
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(false);
      }
    });

    new ChooseCommunicatorsForNashPrompt(gui, uStructure, "Choose Senders and Receivers", " Specify whether or not a controller is allowed to send to or receive from a certain controller: ");

	}

}
