import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class MakeProtocolFeasiblePrompt extends JFrame {

  Automaton automaton;
  List<CommunicationData> potentialCommunications;
  JCheckBox[] checkBoxes;


    /** CONSTRUCTOR **/

  /**
   * Construct a MakeProtocolFeasiblePrompt object.
   **/
  public MakeProtocolFeasiblePrompt(Automaton automaton) {

    this.automaton = automaton;
    potentialCommunications = automaton.getPotentialCommunications();

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

      /* Add Checkboxes */

    checkBoxes = new JCheckBox[potentialCommunications.size()];

    for (int i = 0; i < potentialCommunications.size(); i++) {
      checkBoxes[i] = new JCheckBox(potentialCommunications.get(i).toString(automaton));
      add(checkBoxes[i]);
    }

      /* Add Button */

    JButton button = new JButton("Make Protocol Feasible");
    button.addActionListener(new ActionListener() {
 
        public void actionPerformed(ActionEvent e) {
            
            // Create list of selected communications
            Set<CommunicationData> protocol = new HashSet<CommunicationData>();
            for (int i = 0; i < checkBoxes.length; i++)
              if (checkBoxes[i].isSelected())
                protocol.add(potentialCommunications.get(i));
      
            // Make the protocol feasible
            automaton.makeProtocolFeasible(protocol, Automaton.invert(automaton));

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
    
      /* Don't allow the screen to resize */

    setResizable(false);

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle("Make Protocol Feasible");

      /* Show screen */

    setVisible(true);

  }
    
}