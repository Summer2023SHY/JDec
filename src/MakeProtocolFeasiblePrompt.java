/**
 * MakeProtocolFeasiblePrompt - This class is used to display a pop-up which prompts the user to
 *                              decide which communications are allowed to be in protocol, and
 *                              then go on to generate all applicable feasible protocols, displaying
 *                              them in another window.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class MakeProtocolFeasiblePrompt extends JDialog {

    /* INSTANCE VARIABLES */

  private JDec gui;
  private UStructure uStructure;
  private List<CommunicationData> communications;
  private JCheckBox[] checkBoxes;

    /* CONSTRUCTOR */

  /**
   * Construct a MakeProtocolFeasiblePrompt object.
   * @param gui         A reference to the GUI which is being worked with
   * @param uStructure  The uStructure that is being worked with
   **/
  public MakeProtocolFeasiblePrompt(JDec gui, UStructure uStructure) {

    super(gui, true);

    this.gui = gui;
    this.uStructure = uStructure;
    communications = uStructure.getPotentialAndNashCommunications();

    addComponents();

    setGUIproperties();

  }

    /* METHODS */

  /**
   * Add all of the components to the window.
   **/
  private void addComponents() {

      /* Setup */

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

      /* Add Instructions */

    add(new JLabel(" Select the communications to would like to include in your protocol: "));

      /* Add Checkboxes (in a scroll pane in case there are a lot of them) */

    Container container = new Container();
    container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
    checkBoxes = new JCheckBox[communications.size()];

    for (int i = 0; i < communications.size(); i++) {
      checkBoxes[i] = new JCheckBox(communications.get(i).toString(uStructure));
      container.add(checkBoxes[i]);
    }

    JScrollPane scrollPane = new JScrollPane(container);
    scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
    add(scrollPane);

      /* Add Button */

    JButton button = new JButton("Make Protocol Feasible");
    button.addActionListener(new ActionListener() {
 
      public void actionPerformed(ActionEvent e) {

        // Start this process in a new thread because it will take a while
        new Thread() {
          @Override public void run() {

            // Create list of selected communications
            Set<CommunicationData> protocol = new HashSet<CommunicationData>();
            for (int i = 0; i < checkBoxes.length; i++)
              if (checkBoxes[i].isSelected())
                protocol.add(communications.get(i));
  
            // Find all feasible protocols which include the chosen communications
            List<Set<CommunicationData>> feasibleProtocols = uStructure.makeProtocolFeasible(protocol);

            if (feasibleProtocols.size() == 0) {

                  gui.displayMessage("No Feasible Protocols", "The specified protocol could not be made into a feasible protocol by adding communications.", JOptionPane.INFORMATION_MESSAGE);
            
            } else {

              // Hide this window
              EventQueue.invokeLater(new Runnable() {
                @Override public void run() {
                  MakeProtocolFeasiblePrompt.this.setVisible(false);
                }
              });
            
              // Display results in another window
              new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols", " Here are the feasible protocols: ");

              // Dispose of this window
              MakeProtocolFeasiblePrompt.this.dispose();

            }
          }

        }.start();

      }

    });
    add(button);

  }

  /**
   * Set some default GUI Properties.
   **/
  private void setGUIproperties() {

      /* Pack things in nicely */

    pack();

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle("Make Protocol Feasible");

      /* Show screen */

    setVisible(true);

  }
    
}