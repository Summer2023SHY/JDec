import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class NashInformationPrompt extends JFrame {

    /* INSTANCE VARIABLES */

  private AutomataGUI gui;
  private UStructure uStructure;

    /* CONSTRUCTOR */

  /**
   * Construct a NashInformationPrompt object.
   * @param gui         A reference to the GUI
   * @param uStructure  The U-Structure that is being worked with
   * @param title       The title of the popup box
   * @param message     The text for the label to be displayed at the top of the screen
   **/
  public NashInformationPrompt(AutomataGUI gui, UStructure uStructure, String title, String message) {

    this.gui = gui;
    this.uStructure = uStructure;

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

    final int N_COLUMNS = 3;
    java.util.List<CommunicationData> potentialCommunications = uStructure.getPotentialCommunications();
    java.util.List<NashCommunicationData> nashCommunications = uStructure.getNashCommunications();
    Object[][] tableData = new Object[potentialCommunications.size() + nashCommunications.size()][N_COLUMNS];
    int index = 0;

    for (CommunicationData data : potentialCommunications) {

      Object[] row = new Object[N_COLUMNS];
      row[0] = data.toString(uStructure);
      row[1] = "1";
      row[2] = "0.1"; // TO-DO: Calculate default value

      tableData[index++] = row;

    }

    for (NashCommunicationData data : nashCommunications) {

      Object[] row = new Object[N_COLUMNS];
      row[0] = data.toString(uStructure);
      row[1] = data.cost;
      row[2] = data.probability;

      tableData[index++] = row;

    }

    String[] columnNames = new String[] {"Communication", "Cost (Integer)", "Probability (Double)"};

    TableModel tableModel = new DefaultTableModel(tableData, columnNames) {

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

    JTable table = new JTable(tableModel) {
      
      // Make sure columns are wide enough to display all of the text
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