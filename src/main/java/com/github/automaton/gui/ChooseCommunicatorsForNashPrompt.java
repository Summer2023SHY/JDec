package com.github.automaton.gui;

/*
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 **/

// import java.io.*;
import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.CommunicationRole;
import com.github.automaton.automata.Crush;
import com.github.automaton.automata.NashCommunicationData;
import com.github.automaton.automata.UStructure;

/**
 * Guides the user through the process of finding Nash equilibria
 * once they have selected senders and recievers.
 *
 * @author Micah Stairs
 * 
 * @deprecated Operations for Nash equilibria depend on {@link Crush}. As {@link Crush} is deprecated
   * and subject to removal, all Nash equilibria operations are deprecated.
 */
@Deprecated(since="1.1")
public class ChooseCommunicatorsForNashPrompt extends ChooseSendersAndRecieversPrompt {

    /* CONSTRUCTOR */

  /**
   * Construct a ChooseCommunicatorsForNashPrompt object.
   * @param gui         A reference to the GUI
   * @param uStructure  The U-Structure that is being worked with
   * @param title       The title of the popup box
   * @param message     The text for the label to be displayed at the top of the screen
   **/
  public ChooseCommunicatorsForNashPrompt(JDec gui, UStructure uStructure, String title, String message) {

    super(gui, uStructure, title, message, "Find Nash Equilibria");

  }

    /* OVERIDDEN METHOD */
  /** {@inheritDoc} */
  @Override
  protected boolean performAction() {

    List<Set<NashCommunicationData>> feasibleProtocols = generateFeasibleProtocols();

    // Ensure that there is at least one feasible protocol
    if (feasibleProtocols.size() == 0) {

      JOptionPane.showMessageDialog(null, "There were no feasible protocols that solve the control problem found with the specified senders and recievers\nNOTE: If you selected all possible senders and recievers, then the system does not satisfy observability.", "No Feasible Protocols", JOptionPane.INFORMATION_MESSAGE);
      return false;

    }

    // Hide this screen, since we will not need to go back to it
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(false);
      }
    });

    // Select method to combine communications (unless there are no communications)
    Crush.CombiningCosts selectedMethod = NashInfoForCrushPrompt.pickCombiningCostsMethod("How would you like to combine communication costs?", gui);
    if (selectedMethod == null)
      return true;
    
    // Find Nash equilibria and display results in another window
    new NashEquilibriaOutput(
      gui,
      uStructure,
      uStructure.findNashEquilibria(selectedMethod, feasibleProtocols),
      "Nash Equilibria",
      " Here is the list of all Nash equilibria: "
    );

    return true;

  }

  /**
   * Generate the feasible protocols based on the selected senders and recievers.
   * @return  The list of feasible protocols
   **/
  protected List<Set<NashCommunicationData>> generateFeasibleProtocols() {

    // Generate list of communications which are still allowed based on which boxes the user selected
    List<NashCommunicationData> chosenCommunications = new ArrayList<NashCommunicationData>();

    outer: for (NashCommunicationData data : uStructure.getNashCommunications()) {
      
      int sender = data.getIndexOfSender();

      // Check for communication that isn't allowed
      for (int i = 0; i < data.roles.length; i++)
        if (data.roles[i] == CommunicationRole.RECIEVER && !checkBoxes[sender][i].isSelected())
          continue outer;

      // If we got this far then we can add it
      chosenCommunications.add(data);

    }

    return  uStructure.generateAllFeasibleProtocols(chosenCommunications, true);

  }

}
