package com.github.automaton.gui.util;

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

import javax.swing.table.*;

public class AmbiguityLevelTableRowSorter extends TableRowSorter<AmbiguityLevelTable> {
    
    public AmbiguityLevelTableRowSorter() {
        super();
    }

    public AmbiguityLevelTableRowSorter(AmbiguityLevelTable table) {
        super(table);
    }

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
