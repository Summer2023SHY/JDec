import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class GeneratedAllFeasibleProtocolsPrompt extends JFrame {

  private AutomataGUI gui;
  private Automaton automaton;
  private JCheckBox[][] checkBoxes;


    /** CONSTRUCTOR **/

  /**
   * Construct a GeneratedAllFeasibleProtocolsPrompt object.
   * @param gui       A reference to the GUI which is being worked with
   * @param automaton The automaton that is being worked with
   **/
  public GeneratedAllFeasibleProtocolsPrompt(AutomataGUI gui, Automaton automaton) {

    this.gui = gui;
    this.automaton = automaton;

    addComponents();

    setGUIproperties();

  }

    /** METHODS **/

  /**
   * Add all of the components to the window.
   **/
  private void addComponents() {

      /* Setup */

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

      /* Add Instructions */

    add(new JLabel(" Specify whether or not a controller is allowed to send to or receive from a certain controller: "));

      /* Add Checkboxes */

    add(createCheckBoxGrid());

      /* Add Button */

    JButton button = new JButton("Generate All");
    button.addActionListener(new ActionListener() {
 
        public void actionPerformed(ActionEvent e) {
      
            // Generate list of communications which are still allowed based on which boxes the user selected
            java.util.List<CommunicationData> chosenCommunications = new ArrayList<CommunicationData>();
            outer: for (CommunicationData data : automaton.getPotentialCommunications()) {
              
              int sender = data.getIndexOfSender();

              // Check for communication that isn't allowed
              for (int i = 0; i < data.roles.length; i++)
                if (data.roles[i] == CommunicationRole.RECIEVER && !checkBoxes[sender][i].isSelected())
                  continue outer;

              // If we got this far then we can add it
              chosenCommunications.add(data);

            }

            // Print feasible protocols
            java.util.List<Set<CommunicationData>> feasibleProtocols = automaton.generateAllFeasibleProtocols(chosenCommunications);

            // Display results in another window
            new FeasibleProtocolOutput(gui, automaton, feasibleProtocols, "Feasible Protocols", " Here is the list of all feasible protocols: ");

            // Dispose of this window
            GeneratedAllFeasibleProtocolsPrompt.this.dispose();
        }

    });
    add(button);

  }

  private Container createCheckBoxGrid() {

    Container container = new Container();

    int nControllers = automaton.calculateNumberOfControllersBeforeUStructure();
    checkBoxes = new JCheckBox[nControllers][nControllers];

    container.setLayout(new GridLayout(nControllers + 1, nControllers + 1));

    // Add titles at the top of each column
    container.add(new JLabel("S\\R", SwingConstants.CENTER));
    for (int x = 1; x <= nControllers; x++)
      container.add(new JLabel(String.valueOf(x), SwingConstants.CENTER));

    // For each row
    for (int y = 0; y < nControllers; y++) {

      // Add title for this row
      container.add(new JLabel(String.valueOf(y + 1), SwingConstants.CENTER));

      // For each column
      for (int x = 0; x < nControllers; x++) {
        checkBoxes[y][x] = new JCheckBox();
        checkBoxes[y][x].setEnabled(x != y);
        checkBoxes[y][x].setSelected(x != y);
        checkBoxes[y][x].setHorizontalAlignment(SwingConstants.CENTER);
        container.add(checkBoxes[y][x]);
      }

    }

    return container;

  }

  /**
   * Set some default GUI Properties.
   **/
  private void setGUIproperties() {

      /* Pack things in nicely */

    pack();
    
      /* Don't allow the screen to resize */

    setResizable(false);

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle("Generate All Feasible Protocols");

      /* Show screen */

    setVisible(true);

  }
    
}