/**
 * NashEquilibriaOutput - This class is used to display a pop-up which contains a list of Nash equilibria.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Private Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class NashEquilibriaOutput extends JFrame {

    /** PRIVATE INSTANCE VARIABLES **/

  private AutomataGUI gui;
  private UStructure uStructure;
  private java.util.List<Set<NashCommunicationData>> nashEquilibria;
  private JTextPane[] detailedText;

    /** CONSTRUCTOR **/

  /**NashEquilibriaOutput object.
   * @param gui               A reference to the GUI which will be recieving requests for new tabs
   * @param uStructure        The U-Structure that is being worked with
   * @param nashEquilibria    The list of protocols that are feasible
   * @param title             The title of the popup box
   * @param message           The text for the label to be displayed at the top of the screen
   **/
  public NashEquilibriaOutput(AutomataGUI gui, UStructure uStructure, java.util.List<Set<NashCommunicationData>> nashEquilibria, String title, String message) {

    this.gui = gui;
    this.uStructure = uStructure;
    this.nashEquilibria = nashEquilibria;

    addComponents(message);

    setGUIproperties(title);

  }

    /** METHODS **/

  /**
   * Add all of the components to the window.
   * @param message The text for the label to be displayed at the top of the screen
   **/
  private void addComponents(String message) {

      /* Setup */

    setLayout(new BorderLayout());
    setMaximumSize(new Dimension(500, 500));

      /* Add Instructions */

    add(new JLabel(message), BorderLayout.NORTH);

      /* Display Nash equilibria */

    Container outerContainer = new Container();
    outerContainer.setLayout(new BoxLayout(outerContainer, BoxLayout.PAGE_AXIS));
    detailedText = new JTextPane[nashEquilibria.size()];

    for (int i = 0; i < nashEquilibria.size(); i++) {

      final Set<NashCommunicationData> equilibrium = nashEquilibria.get(i);

      // Format title text differently if there is only one equilibrium
      if (nashEquilibria.size() == 1)
        outerContainer.add(new JLabel(String.format(
          "Nash equilibrium has %d communications.",
          equilibrium.size()
        )));
      else
        outerContainer.add(new JLabel(String.format(
          "Nash equilibrium #%d has %d communications.",
          i + 1,
          equilibrium.size()
        )));

      // Add text to a text pane and make it so that the user cannot edit it
      StringBuilder text = new StringBuilder();
      for (CommunicationData data : equilibrium)
        text.append(data.toString(uStructure) + "\n");
      detailedText[i] = new JTextPane();
      detailedText[i].setText(text.toString());
      detailedText[i].setEditable(false);

      // Build scroll pane
      Container innerContainer = new Container();
      innerContainer.setLayout(new BoxLayout(innerContainer, BoxLayout.PAGE_AXIS));
      innerContainer.add(detailedText[i]);
      JScrollPane innerScrollPane = new JScrollPane(innerContainer);
      innerScrollPane.removeMouseWheelListener(innerScrollPane.getMouseWheelListeners()[0]);
      outerContainer.add(innerScrollPane);

    }

    JScrollPane outerScrollPane = new JScrollPane(outerContainer);
    outerScrollPane.setMaximumSize(new Dimension(500, 500));
    add(outerScrollPane, BorderLayout.CENTER);

  }

  /**
   * Set some default GUI Properties.
   * @param title The title of the pop-up box
   **/
  private void setGUIproperties(String title) {

      /* Pack things in nicely */

    pack();

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle(title);

      /* Show screen */

    setVisible(true);

  }
    
}