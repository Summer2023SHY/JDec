import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;

public class MakeProtocolFeasiblePrompt extends JFrame {

  private AutomataGUI gui;
  private UStructure uStructure;
  private java.util.List<CommunicationData> potentialCommunications;
  private JCheckBox[] checkBoxes;


    /** CONSTRUCTOR **/

  /**
   * Construct a MakeProtocolFeasiblePrompt object.
   * @param gui         A reference to the GUI which is being worked with
   * @param uStructure  The uStructure that is being worked with
   **/
  public MakeProtocolFeasiblePrompt(AutomataGUI gui, UStructure uStructure) {

    this.gui = gui;
    this.uStructure = uStructure;
    potentialCommunications = uStructure.getPotentialCommunications();

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

    add(new JLabel(" Select the communications to would like to include in your protocol: "));

      /* Add Checkboxes (in a scroll pane in case there are a lot of them) */

    Container container = new Container();
    container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
    checkBoxes = new JCheckBox[potentialCommunications.size()];

    for (int i = 0; i < potentialCommunications.size(); i++) {
      checkBoxes[i] = new JCheckBox(potentialCommunications.get(i).toString(uStructure));
      container.add(checkBoxes[i]);
    }

    JScrollPane scrollPane = new JScrollPane(container);
    scrollPane.setMaximumSize(new Dimension(200, 200));
    add(scrollPane);

      /* Add Button */

    JButton button = new JButton("Make Protocol Feasible");
    button.addActionListener(new ActionListener() {
 
        public void actionPerformed(ActionEvent e) {
            
            // Create list of selected communications
            Set<CommunicationData> protocol = new HashSet<CommunicationData>();
            for (int i = 0; i < checkBoxes.length; i++)
              if (checkBoxes[i].isSelected())
                protocol.add(potentialCommunications.get(i));
      
            // Find all feasible protocols which include the chosen communications
            java.util.List<Set<CommunicationData>> feasibleProtocols = uStructure.makeProtocolFeasible(protocol);
            
            // Display results in another window
            new FeasibleProtocolOutput(gui, uStructure, feasibleProtocols, "Feasible Protocols", " Here are the feasible protocols: ");

            // Dispose of this window
            MakeProtocolFeasiblePrompt.this.dispose();
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