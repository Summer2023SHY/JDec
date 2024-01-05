/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import com.github.automaton.automata.CommunicationData;
import com.github.automaton.automata.NashCommunicationData;
import com.github.automaton.automata.UStructure;

/**
 * Used to allow a user to manipulate Nash communications,
 * instead of being required to do it through GUI input code, which is rather
 * unintuitive.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
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
    int index = 0;

    // Calculate how much probability has already been assigned
    double totalProbability = 0d;
    for (NashCommunicationData data : nashCommunications)
      totalProbability += data.probability;
    totalProbability = Math.min(1d, totalProbability);

    // Create arrays to maintain communication data information
    final CommunicationData[] communications = new CommunicationData[nCommunications];
    final double[] costs = new double[nCommunications];
    final double[] probabilities = new double[nCommunications];

    // Add communications to the table with default probability and cost values
    for (CommunicationData data : potentialCommunications) {

      costs[index] = 1d;

      // Calculate default probability so that it is dispersed as equally as possible, but so that
      // it also equals exactly 1.0
      double defaultProbability = (1d - totalProbability) / (double) (nCommunications - index);
      totalProbability += defaultProbability;
      
      probabilities[index] = defaultProbability;

      communications[index] = data;
      
      index++;

    }

    // Add communications to the table which have pre-assigned probability and cost values
    for (NashCommunicationData data : nashCommunications) {

      costs[index] = data.cost;
      probabilities[index] = data.probability;

      communications[index] = data;
      index++;

    }

    final TableModel tableModel = new AbstractTableModel() {

      private static final List<String> columnNames = List.of("Communication", "Cost", "Probability");

      @Override
      public String getColumnName(int column) {
        return columnNames.get(column);
      }

      @Override
      public int findColumn(String columnName) {
        return columnNames.indexOf(columnName);
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
          case 0:
            return String.class;
          case 1:
          case 2:
            return Double.TYPE;
          default:
            throw new IndexOutOfBoundsException(columnIndex);
        }
      }

      @Override
      public int getColumnCount() {
        return N_COLUMNS;
      }

      @Override
      public int getRowCount() {
        return nCommunications;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
          case 0:
            return communications[rowIndex].toString(uStructure);
          case 1:
            return costs[rowIndex];
          case 2:
            return probabilities[rowIndex];
          default:
            throw new IndexOutOfBoundsException(columnIndex);
        }
      }

      // Automatically adjust entered values to reflect how they are being interpreted
      @Override
      public void setValueAt(Object value, int row, int column) {
        try {
          double doubleValue = Double.valueOf((String) value);
          switch (column) {
            case 1:
              // Format the costs as non-negative doubles
              costs[row] = Math.max(0, doubleValue);
              return;
            case 2:
              // Format the probabilities as doubles in the [0,1] range
              probabilities[row] = NashCommunicationData.PROBABILITY_RANGE.fit(doubleValue);
              return;
            default:
              break;
          }
        } catch (NumberFormatException e) {
        }

      }

    };

    final JTable table = new JTable(tableModel) {
      
      // Try to make columns are wide enough to display all of the text
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
      }

      // Make it so that the first column cannot be edited
      @Override
      public boolean isCellEditable(int row, int column) {
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
        @Override
        public void actionPerformed(ActionEvent event) {

          // Don't allow the user to press this button more than once after they've entered valid input
          if (pressedNext)
            return;

          // Ensure that the user is not editing a cell (which means the typed value is not yet saved to the table)
          CellEditor cellEditor = table.getCellEditor();
          if (cellEditor != null)
            cellEditor.stopCellEditing();

          // Ensure that there are no validation errors
          double[] costs          = new double[nCommunications];
          double[] probabilities  = new double[nCommunications];
          double totalProbability = 0d;
          try {
            for (int i = 0; i < nCommunications; i++) {
              costs[i] = (Double) tableModel.getValueAt(i, 1);
              probabilities[i] = (Double) tableModel.getValueAt(i, 2);
              totalProbability += probabilities[i];
            }
          } catch (ClassCastException | NumberFormatException e) {
            gui.displayErrorMessage("Input Errors", "Not all entered values could be interpreted as a number. Please fix all cells highlighted in red.");
            return;
          }

          // Ensure that the column of probability adds up to exactly 1.0
          if (totalProbability != 1d) {
            gui.displayErrorMessage("Probability Values", "The sum of the probability values must equal exactly 1. The entered sum was '" + totalProbability + "'");
            return;
          }

          // Prevent user from pressing this button more than once
          pressedNext = true;
          EventQueue.invokeLater(() -> {
            button.setEnabled(false);
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
   * to the next step.
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

    setLocationRelativeTo(gui);

      /* Update title */

    setTitle(title);

      /* Show screen */

    setVisible(true);

  }

  /* INNER CLASSES */

  private static class CostCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      try {

        Double.valueOf((String) value);
      
        if (isSelected)
          component.setBackground(UIManager.getColor("Table.selectionBackground"));
        else if (hasFocus)
          component.setBackground(UIManager.getColor("Table.focusCellBackground"));
        else
          component.setBackground(UIManager.getColor("Table.background"));  
      
      } catch (NumberFormatException e) {
        component.setBackground(Color.RED);
      }

      return component;
    
    }

  }

  private static class ProbabilityCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      
      try {

        Double.valueOf((String) value);
      
        if (isSelected)
          component.setBackground(UIManager.getColor("Table.selectionBackground"));
        else if (hasFocus)
          component.setBackground(UIManager.getColor("Table.focusCellBackground"));
        else
          component.setBackground(UIManager.getColor("Table.background"));  
      
      } catch (NumberFormatException e) {
        component.setBackground(Color.RED);
      }

      return component;

    }

  }
}
