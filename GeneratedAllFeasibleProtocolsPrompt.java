/**
 * GeneratedAllFeasibleProtocolsPrompt - This class is used to display a pop-up which prompts the user to
 *                                       decide which controllers are allowed to be senders and recievers,
 *                                       and then go on to generate all applicable feasible protocols,
 *                                       displaying them in another window.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class GeneratedAllFeasibleProtocolsPrompt extends JDialog {

    /* INSTANCE VARIABLES */

  private JDec gui;
  private UStructure uStructure;
  private JCheckBox[][] checkBoxes;

  private boolean buttonPressed = false;

    /* CONSTRUCTOR */

  /**
   * Construct a GeneratedAllFeasibleProtocolsPrompt object.
   * @param gui         A reference to the GUI which is being worked with
   * @param uStructure  The UStructure that is being worked with
   **/
  public GeneratedAllFeasibleProtocolsPrompt(JDec gui, UStructure uStructure) {

    super(gui, true);

    this.gui = gui;
    this.uStructure = uStructure;

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

    final JButton button = new JButton("Generate All");
    button.addActionListener(new ActionListener() {
 
        public void actionPerformed(ActionEvent e) {

            if (buttonPressed)
              return;

            buttonPressed = true;

            // Needs to be called on the event dispatch thread, or it will not update in time
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                button.setEnabled(false);
              }
            });

            // Start this process in a new thread because it will take a while
            new Thread() {
              @Override public void run() {
                
                // Generate list of communications which are still allowed based on which boxes the user selected
                java.util.List<CommunicationData> chosenCommunications = new ArrayList<CommunicationData>();

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
                java.util.List<Set<CommunicationData>> feasibleProtocols = uStructure.generateAllFeasibleProtocols(chosenCommunications, false);

                if (feasibleProtocols.size() == 0) {

                  JOptionPane.showMessageDialog(null, "There were no feasible protocols were found with the specified senders and recievers.", "No Feasible Protocols", JOptionPane.INFORMATION_MESSAGE);
            
                } else {
                
                  // Display results in another window
                  new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols", " Here is the list of all feasible protocols: ");

                  // Dispose of this window
                  GeneratedAllFeasibleProtocolsPrompt.this.dispose();

                }

              }  
            }.start();
        }

    });
    add(button);

  }

    /* METHODS */

  /**
   * Create a container with a 2-D grid of checkboxes, with each column and row corresponding to a specific
   * controller.
   * NOTE: This method stores a reference to each JCheckBox object in a 2-D array instance variable.
   * @return  The container of checkboxes
   **/
  private Container createCheckBoxGrid() {

    Container container = new Container();

    int nControllers = uStructure.getNumberOfControllersBeforeUStructure();
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