/**
 * GenerateFeasibleProtocolsPrompt - Extending the abstract class ChooseSendersAndRecieversPrompt, this
 *                                   class generates all feasible protocols given the selected senders
 *                                   and recievers.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 **/

// import java.io.*;
import javax.swing.*;
import java.util.*;

public class GenerateFeasibleProtocolsPrompt extends ChooseSendersAndRecieversPrompt {

    /* CONSTRUCTOR */

  /**
   * Construct a GenerateFeasibleProtocolsPrompt object.
   * @param gui         A reference to the GUI
   * @param uStructure  The U-Structure that is being worked with
   * @param title       The title of the popup box
   * @param message     The text for the label to be displayed at the top of the screen
   **/
  public GenerateFeasibleProtocolsPrompt(JDec gui, UStructure uStructure, String title, String message) {

    super(gui, uStructure, title, message);

  }

    /* OVERIDDEN METHOD */

  @Override protected boolean performAction() {

    // Generate list of communications which are still allowed based on which boxes the user selected
    List<CommunicationData> chosenCommunications = new ArrayList<CommunicationData>();

    outer: for (CommunicationData data : uStructure.getPotentialAndNashCommunications()) {
      
      int sender = data.getIndexOfSender();

      // Check for communication that isn't allowed
      for (int i = 0; i < data.roles.length; i++)
        if (data.roles[i] == CommunicationRole.RECIEVER && !checkBoxes[sender][i].isSelected())
          continue outer;

      // If we got this far then we can add it
      chosenCommunications.add(data);

    }

    // Print feasible protocols
    List<Set<CommunicationData>> feasibleProtocols = uStructure.generateAllFeasibleProtocols(chosenCommunications, false);

    if (feasibleProtocols.size() == 0) {

      JOptionPane.showMessageDialog(null, "There were no feasible protocols were found with the specified senders and recievers.", "No Feasible Protocols", JOptionPane.INFORMATION_MESSAGE);
      return false;

    } else {
    
      // Display results in another window
      new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols", " Here is the list of all feasible protocols: ");
      return true;
      
    }

  }

}
