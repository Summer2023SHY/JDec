import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;

public class MakeProtocolFeasiblePrompt extends JFrame {

  private AutomataGUI gui;
  private Automaton automaton;
  private java.util.List<CommunicationData> potentialCommunications;
  private JCheckBox[] checkBoxes;


    /** CONSTRUCTOR **/

  /**
   * Construct a MakeProtocolFeasiblePrompt object.
   * @param gui       A reference to the GUI which is being worked with
   * @param automaton The automaton that is being worked with
   **/
  public MakeProtocolFeasiblePrompt(AutomataGUI gui, Automaton automaton) {

    this.gui = gui;
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

      /* Add Checkboxes (in a scroll pane in case there are a lot of them) */

    Container container = new Container();
    container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
    checkBoxes = new JCheckBox[potentialCommunications.size()];

    for (int i = 0; i < potentialCommunications.size(); i++) {
      checkBoxes[i] = new JCheckBox(potentialCommunications.get(i).toString(automaton));
      container.add(checkBoxes[i]);
    }

    JScrollPane scrollPane = new JScrollPane(container) {
      @Override public Dimension getPreferredSize() {
        return new Dimension(200, 200);  
      }
    };
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
      
            // Make the protocol feasible
            File headerFile = new File("invert.hdr");
            File bodyFile = new File("invert.bdy");
            Set<CommunicationData> feasibleProtocol = automaton.makeProtocolFeasible(protocol, Automaton.invert(automaton, headerFile, bodyFile));
            
            // Display results in another window
            java.util.List<Set<CommunicationData>> list = new ArrayList<Set<CommunicationData>>();
            list.add(feasibleProtocol);
            new FeasibleProtocolOutput(gui, automaton, list, "Feasible Protocol", " Here is the feasible protocol: ");

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