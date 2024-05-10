/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.*;

import javax.swing.table.*;

import com.github.automaton.automata.AmbiguityData;

/**
 * A {@link TableModel} representation of a {@link List list} of
 * {@link AmbiguityData}.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class AmbiguityLevelTable extends AbstractTableModel {

    /**
     * Fixed number of columns that {@code AmbiguityLevelTable}s have.
     */
    public static final int NUM_COLUMNS = 5;

    /** Column index for states */
    public static final int STATE_COLUMN = 0;
    /** Column label for states */
    public static final String STATE_COLUMN_NAME = "State";
    /** Column index for types of state */
    public static final int STATE_TYPE_COLUMN = 1;
    /** Column label for types of state */
    public static final String STATE_TYPE_COLUMN_NAME = "Type";
    /** Column index for events */
    public static final int EVENT_COLUMN = 2;
    /** Column label for events */
    public static final String EVENT_COLUMN_NAME = "Event";
    /** Column index for controllers */
    public static final int CONTROLLER_COLUMN = 3;
    /** Column label for controllers */
    public static final String CONTROLLER_COLUMN_NAME = "Controller";
    /** Column index for ambiguity levels */
    public static final int AMB_LEVEL_COLUMN = 4;
    /** Column label for ambiguity levels */
    public static final String AMB_LEVEL_COLUMN_NAME = "Ambiguity Level";

    /** String representation for enablement states */
    static final String ENABLEMENT_STR_REPR = "Enablement";
    /** String representation for disablement states */
    static final String DISABLEMENT_STR_REPR = "Disablement";

    /** The underlying ambiguity data list */
    private List<AmbiguityData> dataList;

    /**
     * Constructs a new {@code AmbiguityLevelTable}.
     * 
     * @param data a list of {@link AmbiguityData}
     * @throws NullPointerException if argument is {@code null}
     */
    public AmbiguityLevelTable(List<AmbiguityData> data) {
        this.dataList = Objects.requireNonNull(data);
    }

    /**
     * Returns the number of rows in this table.
     *
     * @return the number of rows in this table
     */
    @Override
    public int getRowCount() {
        return dataList.size();
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
            case STATE_COLUMN:
                return STATE_COLUMN_NAME;
            case STATE_TYPE_COLUMN:
                return STATE_TYPE_COLUMN_NAME;
            case EVENT_COLUMN:
                return EVENT_COLUMN_NAME;
            case CONTROLLER_COLUMN:
                return CONTROLLER_COLUMN_NAME;
            case AMB_LEVEL_COLUMN:
                return AMB_LEVEL_COLUMN_NAME;
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
            case STATE_COLUMN_NAME:
                return STATE_COLUMN;
            case STATE_TYPE_COLUMN_NAME:
                return STATE_TYPE_COLUMN;
            case EVENT_COLUMN_NAME:
                return EVENT_COLUMN;
            case CONTROLLER_COLUMN_NAME:
                return CONTROLLER_COLUMN;
            case AMB_LEVEL_COLUMN_NAME:
                return AMB_LEVEL_COLUMN;
            default:
                return -1;
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
            case STATE_COLUMN:
            case STATE_TYPE_COLUMN:
            case EVENT_COLUMN:
                return String.class;
            case CONTROLLER_COLUMN:
            case AMB_LEVEL_COLUMN:
                return Integer.TYPE;
            default:
                throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
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
        AmbiguityData dataEntry = dataList.get(rowIndex);
        switch (columnIndex) {
            case STATE_COLUMN:
                return dataEntry.state().getLabel();
            case STATE_TYPE_COLUMN:
                return dataEntry.isEnablement() ? "Enablement" : "Disablement";
            case EVENT_COLUMN:
                return dataEntry.event().getLabel();
            case CONTROLLER_COLUMN:
                return dataEntry.controller();
            case AMB_LEVEL_COLUMN:
                return dataEntry.ambLevel() == AmbiguityData.MAX_AMB_LEVEL ? "infinity"
                        : dataEntry.ambLevel();
            default:
                throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
        }
    }

}
