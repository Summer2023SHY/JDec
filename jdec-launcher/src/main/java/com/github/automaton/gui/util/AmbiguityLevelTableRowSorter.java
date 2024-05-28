/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.*;

import javax.swing.table.*;

/**
 * A {@link TableRowSorter} implementation for {@link AmbiguityLevelTable}s.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class AmbiguityLevelTableRowSorter extends TableRowSorter<AmbiguityLevelTable> {

    /**
     * Constructs a new {@code AmbiguityLevelTableRowSorter}.
     */
    public AmbiguityLevelTableRowSorter() {
        super();
    }

    /**
     * Constructs a new {@code AmbiguityLevelTableRowSorter} using the
     * specified table as the underlying {@link AmbiguityLevelTable}.
     * @param table
     */
    public AmbiguityLevelTableRowSorter(AmbiguityLevelTable table) {
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
    public Comparator<?> getComparator(int column) {
        switch (column) {
            case AmbiguityLevelTable.STATE_COLUMN:
                return new Comparator<String>() {
                    @Override
                    public int compare(String a, String b) {
                        if (Objects.equals(a, b)) return 0;
                        String[] aLabelComponents = a.split("-");
                        String[] bLabelComponents = b.split("-");
                        String[] aStateLabels = aLabelComponents[0].split("_");
                        String[] bStateLabels = bLabelComponents[0].split("_");
                        int aDuplicateCode = aLabelComponents.length == 1 ? 0 : Integer.valueOf(aLabelComponents[1]);
                        int bDuplicateCode = bLabelComponents.length == 1 ? 0 : Integer.valueOf(bLabelComponents[1]);
                        if (aStateLabels.length != aStateLabels.length) {
                            throw new IllegalArgumentException();
                        } else if (Arrays.equals(aStateLabels, bStateLabels)) {
                            return aDuplicateCode - bDuplicateCode;
                        }
                        for (int i = 0; i < aStateLabels.length; i++) {
                            int ai = Integer.valueOf(aStateLabels[i]);
                            int bi = Integer.valueOf(bStateLabels[i]);
                            if (ai != bi) {
                                return ai - bi;
                            }
                        }
                        return 0;
                    }
                };
            case AmbiguityLevelTable.STATE_TYPE_COLUMN:
                return new Comparator<String>() {
                    @Override
                    public int compare(String a, String b) {
                        if (Objects.equals(a, b)) return 0;
                        else if (a.equals(AmbiguityLevelTable.ENABLEMENT_STR_REPR)) return 1;
                        else if (b.equals(AmbiguityLevelTable.ENABLEMENT_STR_REPR)) return -1;
                        else throw new IllegalArgumentException();
                    }
                };
            case AmbiguityLevelTable.EVENT_COLUMN:
                return Comparator.<String>naturalOrder();
            case AmbiguityLevelTable.CONTROLLER_COLUMN:
            case AmbiguityLevelTable.AMB_LEVEL_COLUMN:
                return Comparator.<Integer>naturalOrder();
            default:
                throw new IndexOutOfBoundsException("Column index out of bounds: " + column);
        }
    }
}
