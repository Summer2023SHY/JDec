import javax.swing.*;

public class NashInfoForNashEquilibriaPrompt extends NashInformationPrompt {

  /**
   * Construct a NashInfoForNashEquilibriaPrompt object.
   * @param gui     A reference to the GUI
   * @param tab     The tab that is being worked with
   * @param title   The title of the popup box
   * @param message The text for the label to be displayed at the top of the screen
   **/
  public NashInfoForNashEquilibriaPrompt(AutomataGUI gui, AutomataGUI.AutomatonTab tab, String title, String message) {

    super(gui, tab, title, message);

  }

	@Override protected void performAction() {

    // Find Nash equilibria and display results in another window
    try {
    
      new NashEquilibriaOutput(
        gui,
        uStructure,
        uStructure.nash(Crush.CombiningCosts.UNIT),
        "Nash Equilibria",
        " Here is the list of all Nash equilibria: "
      );
    
    // Display error message is the system did not satisfy observability
    } catch (DoesNotSatisfyObservabilityException e) {
      
      JOptionPane.showMessageDialog(
        null,
        "The system does not satisfy observability, which means that it does not solve the control problem. The Nash operation was aborted.",
        "Operation Failed",
        JOptionPane.ERROR_MESSAGE
      );

    }

	}

}
