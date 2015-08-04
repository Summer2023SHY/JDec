/**
 * NashInformationPrompt - This abstract class is used to allow a user to manipulate Nash communications,
 *                         instead of being required to do it through GUI input code, which is rather
 *                         unintuitive.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 *  -Inner Classes
 **/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public abstract class NashInformationPrompt extends JDialog {

    /* INSTANCE VARIABLES */

  protected JDec gui;
  protected JDec.AutomatonTab tab;
  protected UStructure uStructure;
  private boolean pressedNext = false;

    /* CONSTRUCTOR */

  /**
   * Construct a NashInformationPrompt object. This object disposes of itself when
   * it has finished executing the requested actions.
   * @param gui     A reference to the GUI
   * @param tab     The tab that is being worked with
   * @param title   The title of the popup box
   * @param message The text for the label to be displayed at the top of the screen
   **/
  public NashInformationPrompt(JDec gui, JDec.AutomatonTab tab, String title, String message) {

    super(gui, true); 

    this.gui = gui;
    this.tab = tab;
    uStructure = (UStructure) tab.automaton;

    // Skip the screen used to choose communication costs and probabilities if there are no communications
    if (uStructure.getSizeOfPotentialAndNashCommunications() == 0) {
    
      performAction();

    // Otherwise, prepare and show the screen    
    } else {

      addComponents(message);
      setGUIproperties(title);

    }

  }

    /* METHODS */

  /**
   * Add all of the components to the window.
   * @param message The text for the label to be displayed at the top of the screen
   **/
  private void addComponents(String message) {

      /* Setup */

    setLayout(new BorderLayout());
    setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));

      /* Add Instructions */

    add(new JLabel(message), BorderLayout.NORTH);

      /* Create table */

    // Setup
    final int N_COLUMNS = 3;
    java.util.List<CommunicationData> potentialCommunications = uStructure.getPotentialCommunications();
    java.util.List<NashCommunicationData> nashCommunications = uStructure.getNashCommunications();
    final int nCommunications = potentialCommunications.size() + nashCommunications.size();
    Object[][] tableData = new Object[nCommunications][N_COLUMNS];
    int index = 0;

    // Calculate how much probability has already been assigned
    double totalProbability = 0.0;
    for (NashCommunicationData data : nashCommunications)
      totalProbability += data.probability;
    totalProbability = Math.min(1.0, totalProbability);

    // Create array to maintain communication data information
    final CommunicationData[] communications = new CommunicationData[nCommunications];

    // Add communications to the table with default probability and cost values
    for (CommunicationData data : potentialCommunications) {

      Object[] row = new Object[N_COLUMNS];
      row[0] = data.toString(uStructure);
      row[1] = "1";

      // Calculate default probability so that it is dispersed as equally as possible, but so that
      // it also equals exactly 1.0
      double defaultProbability = (1.0 - totalProbability) / (double) (nCommunications - index);
      totalProbability += defaultProbability;
      
      row[2] = String.valueOf(defaultProbability);

      communications[index] = data;
      tableData[index++] = row;

    }

    // Add communications to the table which have pre-assigned probability and cost values
    for (NashCommunicationData data : nashCommunications) {

      Object[] row = new Object[N_COLUMNS];
      row[0] = data.toString(uStructure);
      row[1] = String.valueOf(data.cost);
      row[2] = String.valueOf(data.probability);

      communications[index] = data;
      tableData[index++] = row;

    }

    String[] columnNames = new String[] {"Communication", "Cost", "Probability"};

    final TableModel tableModel = new DefaultTableModel(tableData, columnNames) {

      // Automatially adjust entered values to reflect how they are being interpreted
      @Override public void setValueAt(Object value, int row, int column) {

        // Format the costs as non-negative doubles
        if (column == 1) {
          try {
        
            double doubleValue = Double.valueOf((String) value);
            value = String.valueOf(Math.max(0, doubleValue));

          } catch (NumberFormatException e) { }

        // Format the probabilities as doubles in the [0,1] range
        } else if (column == 2) {

          try {
          
            double doubleValue = Double.valueOf((String) value);

            if (doubleValue < 0)
              value = "0.0";
            else if (doubleValue > 1)
              value = "1.0";
            else
              value = String.valueOf(doubleValue);

          } catch (NumberFormatException e) { }

        }

        super.setValueAt(value, row, column);

      }

    };

    final JTable table = new JTable(tableModel) {
      
      // Try to make columns are wide enough to display all of the text
      @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
      }

      // Make it so that the first column cannot be edited
      @Override public boolean isCellEditable(int row, int column) {
        return column != 0;
      }

    };

    table.setCellSelectionEnabled(true);
    table.getTableHeader().setReorderingAllowed(false);
    table.getColumnModel().getColumn(1).setCellRenderer(new CostCellRenderer());
    table.getColumnModel().getColumn(2).setCellRenderer(new ProbabilityCellRenderer());

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setMaximumSize(new Dimension(JDec.PREFERRED_DIALOG_WIDTH, JDec.PREFERRED_DIALOG_HEIGHT));
    add(scrollPane, BorderLayout.CENTER);

      /* Add Next Button */

    final JButton button = new JButton("Next");
    button.addActionListener(new ActionListener() {
   
        public void actionPerformed(ActionEvent event) {

          // Don't allow the user to press this button more than once after they've entered valid input
          if (pressedNext)
            return;

          // Ensure that the user is not editing a cell (which means the typed value is not yet saved to the table)
          CellEditor cellEditor = table.getCellEditor();
          if (cellEditor != null)
            cellEditor.stopCellEditing();

          // Ensure that there are no vaidation errors
          double[] costs          = new double[nCommunications];
          double[] probabilities  = new double[nCommunications];
          double totalProbability = 0.0;
          try {
            for (int i = 0; i < nCommunications; i++) {
              costs[i] = Double.valueOf(tableModel.getValueAt(i, 1).toString());
              probabilities[i] = Double.valueOf(tableModel.getValueAt(i, 2).toString());
              totalProbability += probabilities[i];
            }
          } catch (NumberFormatException e) {
            gui.displayErrorMessage("Input Errors", "Not all entered values could be interpreted as a number. Please fix all cells highlighted in red.");
            return;
          }

          // Ensure that the column of probability adds up to exactly 1.0
          if (totalProbability != 1.0) {
            gui.displayErrorMessage("Probability Values", "The sum of the probability values must equal exactly 1. The entered sum was '" + totalProbability + "'");
            return;
          }

          // Prevent user from pressing this button more than once
          pressedNext = true;
          EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
              button.setEnabled(false);
            }
          });

          // Remove all pre-existing potential communications and Nash communications
          uStructure.removeAllPotentialCommunications();
          uStructure.removeAllNashCommunications();

          // Re-add the communications to the U-Structure (now with updated Nash communications)
          for (int i = 0; i < nCommunications; i++) {
            uStructure.addNashCommunication(
              communications[i].initialStateID,
              communications[i].eventID,
              communications[i].targetStateID,
              communications[i].roles,
              costs[i],
              probabilities[i]
            );
          }

          // Re-generate and load the GUI input code
          tab.refreshGUI();

          // Perform the action specified by the implementing class once the Nash information has been added
          performAction();

          // Dispose of this window
          NashInformationPrompt.this.dispose();

        }

      });
    add(button, BorderLayout.SOUTH);

  }

  /**
   * This action is performed once the user has validated all of the Nash communications and moves on
   * the next step.
   **/
  protected abstract void performAction();

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

  /* INNER CLASSES */

class CostCellRenderer extends DefaultTableCellRenderer {

  @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    
    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    try {

      Double.valueOf((String) value);
    
      if (isSelected)
        component.setBackground(javax.swing.UIManager.getColor("Table.selectionBackground"));
      else if (hasFocus)
        component.setBackground(javax.swing.UIManager.getColor("Table.focusCellBackground"));
      else
        component.setBackground(javax.swing.UIManager.getColor("Table.background"));  
    
    } catch (NumberFormatException e) {
      component.setBackground(Color.RED);
    }

    return component;
  
  }

}

class ProbabilityCellRenderer extends DefaultTableCellRenderer {

  @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    
    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
    try {

      Double.valueOf((String) value);
    
      if (isSelected)
        component.setBackground(javax.swing.UIManager.getColor("Table.selectionBackground"));
      else if (hasFocus)
        component.setBackground(javax.swing.UIManager.getColor("Table.focusCellBackground"));
      else
        component.setBackground(javax.swing.UIManager.getColor("Table.background"));  
    
    } catch (NumberFormatException e) {
      component.setBackground(Color.RED);
    }

    return component;
  
  }

}