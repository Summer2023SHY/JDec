package automata.gui;

/*
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 **/

// import java.io.*;
import java.util.*;
import javax.swing.*;

import automata.CommunicationData;
import automata.CommunicationRole;
import automata.PrunedUStructure;
import automata.UStructure;

/**
 * Calculates and displays the Myerson values they
 * have selected senders and recievers.
 *
 * @author Micah Stairs
 */
public class ChooseCommunicatorsForMyersonPrompt extends ChooseSendersAndRecieversPrompt {

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

    /* OVERIDDEN METHOD */
  /** {@inheritDoc} */
  @Override
  protected boolean performAction() {

    Set<CommunicationData> protocol = getProtocol();

    // Ensure that there is at least one communication in the protocol
    if (protocol.size() == 0) {
      JOptionPane.showMessageDialog(null, "There were no communications found with the specified senders and recievers", "No Communications", JOptionPane.INFORMATION_MESSAGE);
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
    new ShapleyValuesOutput(gui, prunedUStructure, "Myerson Values", "Myerson values by controller:", "Myerson values by coalition:");

    return true;

  }

  /**
   * Generate the protocol which includes all communication from the selected senders and recievers,
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
                System.out.println("DBEUG: Added!");
              }
              
            }

    } while (changesMade);    

    // Generate list of all allowed communications (including relayed communications)
    Set<CommunicationData> communications = new HashSet<CommunicationData>();

    outer: for (CommunicationData data : uStructure.getPotentialAndNashCommunications()) {
      
      int sender = data.getIndexOfSender();

      // Check for communication that isn't allowed
      for (int i = 0; i < data.roles.length; i++)
        if (data.roles[i] == CommunicationRole.RECIEVER && !selected[sender][i])
          continue outer;

      // If we got this far then we can add it
      communications.add(data);

    }

    return  communications;

  }

}
