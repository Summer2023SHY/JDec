/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * A skeletal implementation of a {@link ComboBoxModel} that uses a
 * {@link List} as the underlying data structure.
 * 
 * @param <T> the type of the elements of this model
 * 
 * @author Sung Ho Yoon
 * @since 2.2.0
 */
public abstract class AbstractComboBoxModel<T> extends AbstractListModel<T> implements ComboBoxModel<T> {

    private List<T> list;
    private int selectedIndex = 0;

    /**
     * Constructs a new {@code AbstractComboBoxModel} from the
     * specified array.
     * 
     * @param arr an array of elements
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    protected AbstractComboBoxModel(T[] arr) {
        this.list = Arrays.asList(arr);
    }

    /**
     * Constructs a new {@code AbstractComboBoxModel} from the
     * specified list.
     * 
     * @param list a list of elements
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    protected AbstractComboBoxModel(List<T> list) {
        this.list = Objects.requireNonNull(list);
    }

    /**
     * Returns the value at the specified index.
     * 
     * @param index an index
     * 
     * @return the element at the specified index
     * 
     * @throws IndexOutOfBoundsException if argument is out of bounds
     */
    @Override
    public T getElementAt(int index) {
        return list.get(index);
    }

    /**
     * Returns the selected item.
     * 
     * @return the selected item
     */
    @Override
    public T getSelectedItem() {
        if (getSize() == 0 || selectedIndex == -1)
            return null;
        return getElementAt(selectedIndex);
    }

    /**
     * Returns the length of this model.
     * 
     * @return the length of this model
     */
    @Override
    public int getSize() {
        return list.size();
    }

    /**
     * Sets the selected item.
     * 
     * @param anItem an item
     */
    @Override
    public void setSelectedItem(Object anItem) {
        int newIndex = list.indexOf(anItem);
        if (this.selectedIndex != newIndex) {
            this.selectedIndex = newIndex;
            fireContentsChanged(this, -1, -1);
        }
    }

}
