/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utility methods for building power sets.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class PowerSetUtils {

    /** Private constructor. */
    private PowerSetUtils() {
    }

    /**
     * Generates a list of all possible sets in the power set which contain the
     * required elements.
     * 
     * @param <T>              the type of data stored in the set
     * @param setElements      the list of elements in the set
     * @param requiredElements the set of elements which must be included in each
     *                         generated set
     * 
     * @throws IllegalArgumentException if {@code requiredElements} is not a subset
     *                                  of {@code setElements}
     * @throws NullPointerException     if either one of the arguments is
     *                                  {@code null}
     */
    public static <T> List<Set<T>> powerSetSubset(List<T> setElements, Set<T> requiredElements) {

        Objects.requireNonNull(setElements);
        Objects.requireNonNull(requiredElements);
        for (T required : requiredElements) {
            if (!setElements.contains(required))
                throw new IllegalArgumentException("setElements does not contain \"" + required + "\"");
        }

        List<Set<T>> results = new ArrayList<>();
        List<T> copyOfMasterList = new ArrayList<T>(setElements);
        copyOfMasterList.removeAll(requiredElements);

        powerSetHelper(results, copyOfMasterList, new HashSet<T>(requiredElements), 0);
        return results;

    }

    /**
     * Generates the power set of a set.
     * 
     * @param <T>         the type of data stored in the set
     * @param setElements the list of elements in the set
     * 
     * @throws NullPointerException if argument is {@code null}
     **/
    public static <T> List<Set<T>> powerSet(List<T> setElements) {

        Objects.requireNonNull(setElements);
        List<Set<T>> results = new ArrayList<>();
        powerSetHelper(results, setElements, new HashSet<T>(), 0);
        return results;

    }

    /**
     * A method used to help generate the power set.
     * 
     * @param <T>            the type of data stored in the set
     * @param results        a list of sets where all of the sets in the
     *                       power set will be stored
     * @param masterList     the original list of elements in the set
     * @param elementsChosen the elements chosen so far
     * @param index          the current index in the master list
     **/
    private static <T> void powerSetHelper(List<Set<T>> results,
            List<T> masterList,
            Set<T> elementsChosen,
            int index) {

        /* Base case */

        if (index == masterList.size()) {
            results.add(elementsChosen);
            return;
        }

        /* Recursive case */

        Set<T> includingElement = new HashSet<T>();
        Set<T> notIncludingElement = new HashSet<T>();

        for (T e : elementsChosen) {
            includingElement.add(e);
            notIncludingElement.add(e);
        }

        includingElement.add(masterList.get(index));

        // Recursive calls
        powerSetHelper(results, masterList, includingElement, index + 1);
        powerSetHelper(results, masterList, notIncludingElement, index + 1);

    }
}
