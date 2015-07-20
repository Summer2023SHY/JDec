import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class NashInformationPrompt extends JFrame {

    /* INSTANCE VARIABLES */

  private AutomataGUI gui;
  private AutomataGUI.AutomatonTab tab;
  private UStructure uStructure;

    /* CONSTRUCTOR */

  /**
   * Construct a NashInformationPrompt object.
   * @param gui     A reference to the GUI
   * @param tab     The tab that is being worked with
   * @param title   The title of the popup box
   * @param message The text for the label to be displayed at the top of the screen
   **/
  public NashInformationPrompt(AutomataGUI gui, AutomataGUI.AutomatonTab tab, String title, String message) {

    this.gui = gui;
    this.tab = tab;
    uStructure = (UStructure) tab.automaton;

    addComponents(message);

    setGUIproperties(title);

  }

    /* METHODS */

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

      /* Create table */

    // Setup
    final int N_COLUMNS = 3;
    java.util.List<CommunicationData> potentialCommunications = uStructure.getPotentialCommunications();
    java.util.List<NashCommunicationData> nashCommunications = uStructure.getNashCommunications();
    final int nCommunications = potentialCommunications.size() + nashCommunications.size();
    Object[][] tableData = new Object[nCommunications][N_COLUMNS];
    int index = 0;

    // Calculate what the default probability should be
    double totalProbability = 0.0;
    for (NashCommunicationData data : nashCommunications)
      totalProbability += data.probability;
    totalProbability = Math.min(1.0, totalProbability);
    double defaultProbability = (1.0 - totalProbability) / (double) nCommunications;

    // Create array to maintain communication data information
    final CommunicationData[] communications = new CommunicationData[nCommunications];

    // Add communications to the table with default probability and cost values
    for (CommunicationData data : potentialCommunications) {

      Object[] row = new Object[N_COLUMNS];
      row[0] = data.toString(uStructure);
      row[1] = "1";
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

    String[] columnNames = new String[] {"Communication", "Cost (Integer)", "Probability (Double)"};

    final TableModel tableModel = new DefaultTableModel(tableData, columnNames) {

      // Automatially adjust entered values to reflect how they are being interpreted
      @Override public void setValueAt(Object value, int row, int column) {

        // Format the costs as integers
        if (column == 1) {
          try {
        
            int intValue = Double.valueOf((String) value).intValue();
            value = String.valueOf(Math.max(0, intValue));

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
    scrollPane.setMaximumSize(new Dimension(500, 500));
    add(scrollPane, BorderLayout.CENTER);

      /* Add Next Button */

    JButton button = new JButton("Next");
    button.addActionListener(new ActionListener() {
   
        public void actionPerformed(ActionEvent event) {

          // Ensure that the user is not editing a cell (which means the typed value is not yet saved to the table)
          CellEditor cellEditor = table.getCellEditor();
          if (cellEditor != null)
            cellEditor.stopCellEditing();

          // Ensure that there are no vaidation errors
          int[] costs = new int[nCommunications];
          double[] probabilities = new double[nCommunications];
          double totalProbability = 0.0;
          try {
            for (int i = 0; i < nCommunications; i++) {
              costs[i] = Integer.valueOf(tableModel.getValueAt(i, 1).toString());
              probabilities[i] = Double.valueOf(tableModel.getValueAt(i, 2).toString());
              totalProbability += probabilities[i];
            }
          } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Not all entered values could be interpreted as a number. Please fix all cells highlighted in red.", "Input Errors", JOptionPane.ERROR_MESSAGE);
            return;
          }

          // Ensure that the column of probability adds up to exactly 1.0
          if (totalProbability != 1.0) {
            JOptionPane.showMessageDialog(null, "The sum of the probability values must equal exactly 1. The entered sum was '" + totalProbability + "'", "Probability Values", JOptionPane.ERROR_MESSAGE);
            return;
          }

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

        }

      });
    add(button, BorderLayout.SOUTH);

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

class CostCellRenderer extends DefaultTableCellRenderer {

  @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    
    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    try {

      Double.valueOf((String) value).intValue();
    
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