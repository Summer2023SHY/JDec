package com.github.automaton.gui;

/*
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overridden Method
 **/

import java.util.*;
import javax.swing.*;

import org.apache.logging.log4j.*;

import com.github.automaton.automata.CommunicationData;
import com.github.automaton.automata.CommunicationRole;
import com.github.automaton.automata.PrunedUStructure;
import com.github.automaton.automata.UStructure;

/**
 * Calculates and displays the Myerson values they
 * have selected senders and receivers.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class ChooseCommunicatorsForMyersonPrompt extends ChooseSendersAndReceiversPrompt {

  private static Logger logger = LogManager.getLogger();

    /* CONSTRUCTOR */

  /**
   * Construct a ChooseCommunicatorsForMyersonPrompt object.
   * @param gui         A reference to the GUI
   * @param uStructure  The U-Structure that is being worked with
   * @param title       The title of the popup box
   * @param message     The text for the label to be displayed at the top of the screen
   **/
  public ChooseCommunicatorsForMyersonPrompt(JDec gui, UStructure uStructure, String title, String message) {

    super(gui, uStructure, title, message, "Find Myerson Values");

  }

    /* OVERRIDDEN METHOD */
  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("removal")
  protected boolean performAction() {

    Set<CommunicationData> protocol = getProtocol();

    // Ensure that there is at least one communication in the protocol
    if (protocol.size() == 0) {
      JOptionPane.showMessageDialog(null, "There were no communications found with the specified senders and receivers", "No Communications", JOptionPane.INFORMATION_MESSAGE);
      return false;
    }

    // Hide this screen, since we will not need to go back to it
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(false);
      }
    });

    // Generated the pruned U-Structure
    PrunedUStructure prunedUStructure = uStructure.applyProtocol(getProtocol(), null, null, true);

    // Display Myerson values
    // TODO: Handle use of deprecated method
    new ShapleyValuesOutput(gui, prunedUStructure, "Myerson Values", "Myerson values by controller:", "Myerson values by coalition:");

    return true;

  }

  /**
   * Generate the protocol which includes all communication from the selected senders and receivers,
   * including communications that are relayed.
   * @return  The protocol (which may or may not be feasible)
   **/
  protected Set<CommunicationData> getProtocol() {

    // Create 2-D array indicating which boxes were checked
    int nControllers = uStructure.getNumberOfControllers();
    boolean[][] selected = new boolean[nControllers][nControllers];
    for (int i = 0; i < nControllers; i++)
      for (int j = 0; j < nControllers; j++)
        selected[i][j] = checkBoxes[i][j].isSelected();

    // Select all boxes necessary to account for relayed communication
    boolean changesMade;
    do {

      changesMade = false;

      // If a->b and b->c, then a->c
      for (int a = 0; a < nControllers; a++)
        for (int b = 0; b < nControllers; b++)
          for (int c = 0; c < nControllers; c++)
            if (selected[a][b] && selected[b][c]) {
              if (a != c && !selected[a][c]) {
                changesMade = true;
                selected[a][c] = true;
                logger.debug("Added!");
              }
              
            }

    } while (changesMade);    

    // Generate list of all allowed communications (including relayed communications)
    Set<CommunicationData> communications = new HashSet<CommunicationData>();

    outer: for (CommunicationData data : uStructure.getPotentialAndNashCommunications()) {
      
      int sender = data.getIndexOfSender();

      // Check for communication that isn't allowed
      for (int i = 0; i < data.roles.length; i++)
        if (data.roles[i] == CommunicationRole.RECEIVER && !selected[sender][i])
          continue outer;

      // If we got this far then we can add it
      communications.add(data);

    }

    return  communications;

  }

}