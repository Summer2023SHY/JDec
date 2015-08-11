/**
 * NashInfoForNashEquilibriaPrompt - Extending the abstract class NashInformationPrompt, this class initiates
 *                                   a call to find all Nash equilibria once the user has finished choosing
 *                                   the cost and probability values for the Nash communications.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 **/

import javax.swing.*;

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

	@Override protected void performAction() {

    // Hide this screen, since we will not need to go back to it
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(false);
      }
    });

    new ChooseCommunicatorsForNashPrompt(gui, uStructure, "Choose Senders and Receivers", " Specify whether or not a controller is allowed to send to or receive from a certain controller: ");

	}

}
