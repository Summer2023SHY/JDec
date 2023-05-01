package com.github.automaton.gui;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.github.automaton.automata.UStructure;

/**
 * Used to calculate and display the Shaley values for each coalition
 * and each controller in a popup.
 *
 * @author Micah Stairs
 * 
 * @deprecated Data being displayed through this class depends on the
 * deprecated {@link UStructure#findShapleyValues()} method, and thus, this
 * class is also deprecated and subject to removal.
 */
@Deprecated(forRemoval = true, since="1.1")
public class ShapleyValuesOutput extends JDialog {

    /* INSTANCE VARIABLES */

  private JDec gui;
  private UStructure uStructure;
  private String controllerText;
  private String coalitionText;

    /* CONSTRUCTOR */

  /**
   * Construct a dialog used to calculate and display the Shapley values.
   * NOTE: This can also be used to display the Myerson values since they work much the same.
   * @param gui               A reference to the GUI
   * @param uStructure        The U-Structure that is being worked with
   * @param title             The title of the popup box
   **/
  public ShapleyValuesOutput(JDec gui,
                             UStructure uStructure,
                             String title,
                             String controllerText,
                             String coalitionText) {

    super(gui, true);

    this.gui            = gui;
    this.uStructure     = uStructure;
    this.controllerText = controllerText;
    this.coalitionText  = coalitionText;

    addComponents();
    setGUIproperties(title);

  }

    /* METHODS */

  /**
   * Add all of the components to the window.
   **/
  private void addComponents() {

      /* Setup */

    setLayout(new BorderLayout());
    setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

      /* Calculate Shapley values */
    
    Map<Set<Integer>, Integer> shapleyValues = uStructure.findShapleyValues();
    double[] shapleyValuesByController = new double[uStructure.getNumberOfControllers()]; // 0-based
    for (int i = 0; i < shapleyValuesByController.length; i++)
      shapleyValuesByController[i] = uStructure.findShapleyValueForController(shapleyValues, i + 1);

      /* Display Shapley values */

    StringBuilder stringBuilder = new StringBuilder();
    
    stringBuilder.append(controllerText + "\n");
    for (int i = 0; i < shapleyValuesByController.length; i++)
      stringBuilder.append(String.format("\t%d: %.4f\n", i + 1, shapleyValuesByController[i]));
    
    stringBuilder.append("\n" + coalitionText + "\n");
    for (Map.Entry<Set<Integer>, Integer> entry : shapleyValues.entrySet())
      stringBuilder.append(String.format("\t%s: %d\n", entry.getKey(), entry.getValue()));

    JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(stringBuilder.toString());

    JScrollPane scrollPane = new JScrollPane(textPane);
    scrollPane.setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
    add(scrollPane, BorderLayout.CENTER);
    
      /* Add "Dismissed" Button */

    JButton dismissedButton = new JButton("Dismiss");
    dismissedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ShapleyValuesOutput.this.dispose();
      }
    });
    add(dismissedButton, BorderLayout.SOUTH);

  }

  /**
   * Set some default GUI Properties.
   * @param title The title of the pop-up box
   **/
  private void setGUIproperties(String title) {

    // Pack things in nicely
    pack();

    // Sets screen location in the center of the screen (only works after calling pack)
    setLocationRelativeTo(null);

    // Update title
    setTitle(title);

    // Show screen
    setVisible(true);

  }
    
}