package com.github.automaton.gui;

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

import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.CommunicationData;
import com.github.automaton.automata.CommunicationRole;
import com.github.automaton.automata.UStructure;

/**
 * Generates all feasible protocols given the selected senders and receivers.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class GenerateFeasibleProtocolsPrompt extends ChooseSendersAndReceiversPrompt {

    /* CONSTRUCTOR */

  /**
   * Construct a GenerateFeasibleProtocolsPrompt object.
   * @param gui         A reference to the GUI
   * @param uStructure  The U-Structure that is being worked with
   * @param title       The title of the popup box
   * @param message     The text for the label to be displayed at the top of the screen
   **/
  public GenerateFeasibleProtocolsPrompt(JDec gui, UStructure uStructure, String title, String message) {

    super(gui, uStructure, title, message, "Generate All");

  }

    /* OVERRIDDEN METHOD */

  /** {@inheritDoc} */
  @Override
  protected boolean performAction() {

    List<Set<CommunicationData>> feasibleProtocols = generateFeasibleProtocols();

    if (feasibleProtocols.size() == 0) {

      gui.displayMessage("No Feasible Protocols", "There were no feasible protocols found with the specified senders and receivers.", JOptionPane.INFORMATION_MESSAGE);
      return false;

    } else {
    
      // Hide this screen, since we will not need to go back to it
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          setVisible(false);
        }
      });

      // Display results in another window
      new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols", " Here is the list of all feasible protocols: ");
      return true;
      
    }

  }

    /* METHOD */

  /**
   * Generate the feasible protocols based on the selected senders and receivers.
   * @return  The list of feasible protocols
   **/
  protected List<Set<CommunicationData>> generateFeasibleProtocols() {

    // Generate list of communications which are still allowed based on which boxes the user selected
    List<CommunicationData> chosenCommunications = new ArrayList<CommunicationData>();

    outer: for (CommunicationData data : uStructure.getPotentialAndNashCommunications()) {
      
      int sender = data.getIndexOfSender();

      // Check for communication that isn't allowed
      for (int i = 0; i < data.roles.length; i++)
        if (data.roles[i] == CommunicationRole.RECEIVER && !checkBoxes[sender][i].isSelected())
          continue outer;

      // If we got this far then we can add it
      chosenCommunications.add(data);

    }

    return  uStructure.generateAllFeasibleProtocols(chosenCommunications, false);

  }

}
