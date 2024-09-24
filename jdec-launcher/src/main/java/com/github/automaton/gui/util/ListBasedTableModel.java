/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.*;

import javax.swing.table.*;

/**
 * An implementation of {@link TableModel} that uses a {@link List} as its
 * underlying data structure. Each entry of the underlying list represents
 * a row in this model.
 * 
 * @param <T> type of data stored in the underlying list
 * 
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public abstract class ListBasedTableModel<T> extends AbstractTableModel {
    
    /**
     * Underlying data structure.
     */
    private List<T> list;

    /**
     * Constructs a new {@code ListBasedTableModel}.
     */
    protected ListBasedTableModel() {
        this.list = new ArrayList<>();
    }

    /**
     * Constructs a new {@code ListBasedTableModel} with the specified list.
     * 
     * @param list a list
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    protected ListBasedTableModel(List<T> list) {
        this.list = Objects.requireNonNull(list);
    }

    /**
     * Returns the number of rows in this model.
     *
     * @return the number of rows in this model
     */
    @Override
    public int getRowCount() {
        return list.size();
    }

    /**
     * Returns the entry stored in the underlying list at the specified index.
     * 
     * @param index an index
     * @return the entry stored at the specified index
     * 
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    protected T getEntry(int index) {
        return list.get(index);
    }

    /**
     * Returns the underlying list. Modifying the list directly
     * through this method does not notify listeners associated
     * with this model.
     * 
     * @return the underlying list
     */
    protected List<T> getList() {
        return list;
    }

}
