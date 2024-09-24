/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.*;

import javax.swing.table.*;

import com.github.automaton.automata.*;

/**
 * A {@link TableModel} representation of a {@link List list} of
 * control configurations.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class ControlConfigTable extends ListBasedTableModel<State> {

    public static final int NUM_COLUMNS = 3;

    /** Column index for states */
    public static final int STATE_COLUMN = 0;
    /** Column label for states */
    public static final String STATE_COLUMN_NAME = "State";
    /** Column index for types of state */
    public static final int STATE_TYPE_COLUMN = 1;
    /** Column label for types of state */
    public static final String STATE_TYPE_COLUMN_NAME = "Type";
    /** Column index for illegal configs */
    public static final int ILLEGAL_CONFIG_COLUMN = 2;
    /** Column label for illegal configs */
    public static final String ILLEGAL_CONFIG_COLUMN_NAME = "Illegal config?";

    private String event;
    private UStructure uStructure;

    /**
     * Constructs a new {@code ControlConfigTable}.
     * 
     * @param uStructure a UStructure
     * @param event an event label
     */
    public ControlConfigTable(UStructure uStructure, String event) {
        this.uStructure = Objects.requireNonNull(uStructure);
        this.event = Objects.requireNonNull(event);
        buildList();
    }

    private void buildList() {
        var enablementStates = uStructure.getEnablementStates(event);
        var disablementStates = uStructure.getDisablementStates(event);

        for (var enablementState : enablementStates) {
            getList().add(enablementState);
        }
        for (var disablementState : disablementStates) {
            getList().add(disablementState);
        }
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
    public String getColumnName(int column) {
        switch (column) {
            case STATE_COLUMN:
                return STATE_COLUMN_NAME;
            case STATE_TYPE_COLUMN:
                return STATE_TYPE_COLUMN_NAME;
            case ILLEGAL_CONFIG_COLUMN:
                return ILLEGAL_CONFIG_COLUMN_NAME;
            default:
                throw new IndexOutOfBoundsException("Column index out of bounds: " + column);
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
            case ILLEGAL_CONFIG_COLUMN_NAME:
                return ILLEGAL_CONFIG_COLUMN;
            default:
                return -1;
        }
    }

    /**
     * Returns the class that represents the type of data
     * stored in the specified column.
     * 
     * @param columnIndex the index of the column
     * @return the {@code String.class}
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    @Override
    public Class<String> getColumnClass(int columnIndex) {
        if (columnIndex < 0 || columnIndex > NUM_COLUMNS)
            throw new IndexOutOfBoundsException("Column index out of bounds: " + columnIndex);
        return String.class;
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
    public String getValueAt(int rowIndex, int columnIndex) {
        State entry = getEntry(rowIndex);
        switch (columnIndex) {
            case STATE_COLUMN:
                return entry.getLabel();
            case STATE_TYPE_COLUMN:
                return entry.isEnablementStateOf(event) ? AmbiguityLevelTable.ENABLEMENT_STR_REPR : AmbiguityLevelTable.DISABLEMENT_STR_REPR;
            case ILLEGAL_CONFIG_COLUMN:
                return entry.isIllegalConfigurationOf(event) ? "Yes" : "No";
            default:
                throw new IndexOutOfBoundsException(columnIndex);
        }
    }

}


