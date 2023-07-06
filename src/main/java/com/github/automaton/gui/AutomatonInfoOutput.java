package com.github.automaton.gui;

/* 
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import org.apache.commons.lang3.tuple.Pair;

import com.github.automaton.automata.*;

/**
 * Displays some basic information about an automaton.
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.0
 * 
 * @see Automaton#getNumberOfStates()
 * @see Automaton#getNumberOfEvents()
 * @see Automaton#getNumberOfControllers()
 * @see Automaton#getNumberOfTransitions()
 */
public class AutomatonInfoOutput extends JDialog {

    /** Reference to the {@link JDec} instance that owns this popup */
    private JDec gui;
    private Automaton automaton;
    private AutomatonInfoTable infoTable;
    private JTable dataTable;

    /**
     * Constructs a new {@code AutomatonInfoOutput}.
     * 
     * @param gui       a reference to the {@link JDec} instance that owns
     *                  this popup
     * @param title     the title for this popup
     * @param automaton the automaton to extract data from
     */
    public AutomatonInfoOutput(JDec gui, String title, Automaton automaton) {
        super(gui, true);
        this.gui = gui;
        this.automaton = Objects.requireNonNull(automaton);
        setResizable(false);
        buildComponents();
        setGUIproperties(title);
        pack();
    }

    /**
     * Builds the GUI components of this {@code AutomatonInfoOutput}.
     */
    private void buildComponents() {
        infoTable = new AutomatonInfoTable(automaton);
        dataTable = new JTable(infoTable);
        dataTable.setDragEnabled(false);
        Box box = Box.createVerticalBox();
        box.add(dataTable);
        box.add(new JScrollPane(dataTable));
        add(box);

    }

    /**
     * Set some default GUI Properties.
     * 
     * @param title The title of the pop-up box
     **/
    private void setGUIproperties(final String title) {

        /*
         * This method is mostly copied from
         * AutomataExplorer.setGUIProperties(String)
         */

        SwingUtilities.invokeLater(() -> {

            /*
             * Sets screen location in the center of the screen
             * (only works after calling pack)
             */

            setLocationRelativeTo(gui);

            /* Update title */

            setTitle(title);

            /* Show screen */

            setVisible(true);

        });

    }

    /**
     * {@link TableModel} wrapper for {@link Automaton automata}.
     */
    private static class AutomatonInfoTable extends AbstractTableModel {

        /**
         * Fixed number of columns that {@code AmbiguityLevelTable}s have.
         */
        static final int NUM_COLUMNS = 2;

        static final int PROPERTY_COLUMN = 0;
        static final String PROPERTY_COLUMN_NAME = "Property";
        static final int VALUE_COLUMN = 1;
        static final String VALUE_COLUMN_NAME = "Value";

        private Automaton automaton;
        private List<Pair<String, Long>> automatonInfo = new ArrayList<>();

        public AutomatonInfoTable(Automaton automaton) {
            this.automaton = Objects.requireNonNull(automaton);
            automatonInfo.add(Pair.of("#States", this.automaton.getNumberOfStates()));
            automatonInfo.add(Pair.of("#Events", Long.valueOf(this.automaton.getNumberOfEvents())));
            automatonInfo.add(Pair.of("#Controllers", Long.valueOf(this.automaton.getNumberOfControllers())));
            automatonInfo.add(Pair.of("#Transitions", this.automaton.getNumberOfTransitions()));
        }

        /**
         * Returns the number of columns in this table.
         * 
         * @return the number of columns in this table
         * 
         * @see #NUM_COLUMNS
         */
        @Override
        public int getColumnCount() {
            return NUM_COLUMNS;
        }

        /**
         * Returns the number of rows in this table.
         * 
         * @return the number of rows in this table
         */
        @Override
        public int getRowCount() {
            return automatonInfo.size();
        }

        /**
         * Returns the name of the column at the specified index.
         * 
         * @param columnIndex the index of the column
         * 
         * @return the name of the column at the specified index
         * 
         * @throws IndexOutOfBoundsException if argument is out of bounds
         */
        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case PROPERTY_COLUMN:
                    return PROPERTY_COLUMN_NAME;
                case VALUE_COLUMN:
                    return VALUE_COLUMN_NAME;
                default:
                    throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
            }
        }

        /**
         * Returns the class that represents the type of data
         * stored in the specified column.
         * 
         * @param columnIndex the index of the column
         * @return the class that represents the type of data in the
         *         specified column
         * @throws IndexOutOfBoundsException if argument is out of bounds
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case PROPERTY_COLUMN:
                    return String.class;
                case VALUE_COLUMN:
                    return Long.TYPE;
                default:
                    throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
            }
        }

        /**
         * Returns the index of the column given its name.
         * 
         * @return the index of the column with the specified name, or {@code -1}
         *         if there is no column with the matching name
         */
        @Override
        public int findColumn(String columnName) {
            switch (columnName) {
                case PROPERTY_COLUMN_NAME:
                    return PROPERTY_COLUMN;
                case VALUE_COLUMN_NAME:
                    return VALUE_COLUMN;
                default:
                    return -1;
            }
        }

        /**
         * Returns the value for the cell at the specified position.
         * 
         * @param rowIndex    the row to be queried
         * @param columnIndex the column to be queried
         * @return the value stored at the specified position
         * 
         * @throws IndexOutOfBoundsException if either one of the arguments
         *                                   is out of bounds
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Pair<String, Long> property = automatonInfo.get(rowIndex);
            switch (columnIndex) {
                case PROPERTY_COLUMN:
                    return property.getLeft();
                case VALUE_COLUMN:
                    return property.getValue();
                default:
                    throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
            }
        }

    }
}
