/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import static com.github.automaton.gui.util.AmbiguityLevelTableRowSorter.*;

import java.util.*;

import javax.swing.table.*;

/**
 * A {@link TableRowSorter} implementation for {@link ControlConfigTable}s.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class ControlConfigTableRowSorter extends TableRowSorter<ControlConfigTable> {

    /**
     * Constructs a new {@code ControlConfigTableRowSorter}.
     */
    public ControlConfigTableRowSorter() {
        super();
    }

    /**
     * Constructs a new {@code ControlConfigTableRowSorter} using the
     * specified table as the underlying {@link ControlConfigTable}.
     * 
     * @param table
     */
    public ControlConfigTableRowSorter(ControlConfigTable table) {
        super(table);
    }

    /**
     * Returns the {@link Comparator} for the specified column.
     * 
     * @return the comparator for the specified column
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    @Override
    public Comparator<String> getComparator(int column) {
        switch (column) {
            case ControlConfigTable.STATE_COLUMN:
                return STATE_LABEL_COMPARATOR;
            case ControlConfigTable.STATE_TYPE_COLUMN:
                return STATE_TYPE_COMPARATOR;
            case ControlConfigTable.ILLEGAL_CONFIG_COLUMN:
                return (a, b) -> {
                    if (Objects.equals(a, b))
                        return 0;
                    else if (a.equals("Yes"))
                        return 1;
                    else if (b.equals("Yes"))
                        return -1;
                    else
                        throw new IllegalArgumentException();
                };
            default:
                throw new IndexOutOfBoundsException("Column index out of bounds: " + column);
        }
    }
}
