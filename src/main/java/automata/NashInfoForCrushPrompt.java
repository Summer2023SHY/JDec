package automata;
/**
 * NashInfoForCrushPrompt - Extending the abstract class NashInformationPrompt, this class helps the user
 *                          generate a Crush structure once they have finished choosing the cost and
 *                          probability values for the Nash communications.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overidden Method
 *  -Method
 **/

import java.io.*;
import javax.swing.*;

public class NashInfoForCrushPrompt extends NashInformationPrompt {

    /* CONSTRUCTOR */

  /**
   * Construct a NashInformationForNashEquilibriaPrompt object.
   * @param gui     A reference to the GUI
   * @param tab     The tab that is being worked with
   * @param title   The title of the popup box
   * @param message The text for the label to be displayed at the top of the screen
   **/
  public NashInfoForCrushPrompt(JDec gui, JDec.AutomatonTab tab, String title, String message) {

    super(gui, tab, title, message);

  }

    /* OVERIDDEN METHOD */

  @Override protected void performAction() {

    // Hide this popup, and ask the user how they would like to combine the communication costs
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(false);
      }
    });
    
    // Select method to combine communications (unless there are no communications)
    Crush.CombiningCosts selectedMethod = null;
    if (uStructure.getSizeOfPotentialAndNashCommunications() > 0) {
      selectedMethod = pickCombiningCostsMethod("How would you like to combine communication costs?", gui);
      if (selectedMethod == null)
        return;
    }

    // Select controller to take the Crush with respect to
    int selectedController = gui.pickController("Which component would you like to take the crush with respect to?", true);
    if (selectedController == -1)
      return;

    // Get temporary files to store the Crush in
    String fileName = gui.getTemporaryFileName();
    File headerFile = new File(fileName + ".hdr");
    File bodyFile = new File(fileName + ".bdy");

    // Create new tab with the generated crush
    gui.createTab(uStructure.crush(headerFile, bodyFile, selectedController, null, selectedMethod));

  }

    /* METHOD */

  /**
   * Allow the user to select a method to combine costs.
   * @param str   The message to display
   * @param frame The application's frame (if null, then this dialog will not be modal)
   * @return      The enum value associated with the selected method (or null if nothing was selected)
   **/
  public static Crush.CombiningCosts pickCombiningCostsMethod(String str, JFrame frame) {

    // Create list of options
    String[] options = new String[] {"Maximum", "Sum", "Average"};

    // Display prompt to user
    String choice = (String) JOptionPane.showInputDialog(
      frame,
      str,
      "Choose Method",
      JOptionPane.PLAIN_MESSAGE,
      null,
      options,
      options[0]
    );

    if (choice == null)
      return null;

      /* Return associated enum value */
    
    switch (choice) {

      case "Maximum":
        return Crush.CombiningCosts.MAX;
        
      case "Sum":
        return Crush.CombiningCosts.SUM;

      case "Average":
        return Crush.CombiningCosts.AVERAGE;

      default:
        return null;

    }

  }

}
