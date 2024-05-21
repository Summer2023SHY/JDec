/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.util;

import java.math.BigInteger;
import java.util.*;

import com.github.automaton.automata.Automaton;

/**
 * A collection of ID calculation methods.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class IDUtil {

    /** Private constructor. */
    private IDUtil() {
    }

    /**
     * Given two state IDs (the order matters) and their respective automatons,
     * create a unique combined ID.
     * 
     * <p>
     * The reasoning behind this formula is analogous to the following: if
     * you have a table with N rows and M columns,
     * every cell is guaranteed to have a different combination of row and
     * column indexes.
     * 
     * @param id1    The state ID from the first automaton
     * @param first  The first automaton
     * @param id2    The state ID from the second automaton
     * @param second The second automaton
     * @return The combined ID
     * @throws ArithmeticException if the ID combination result overflows a
     *                             {@code long}
     */
    public static long combineTwoIDs(long id1, Automaton first, long id2, Automaton second) {

        return Math.addExact((id2 - 1) * first.getNumberOfStates(), id1);

    }

    /**
     * Given a list of IDs and a maximum possible ID, create a unique combined ID.
     * 
     * <p>
     * The order of the list matters. This method does not sort the list internally.
     * 
     * @param list  The list of IDs
     * @param maxID The largest possible value that could appear in the list
     *              (usually {@link Automaton#nStates})
     * 
     * @return The unique combined ID
     * 
     * @throws ArithmeticException if the ID combination result overflows a
     *                             {@code long}
     */
    public static long combineIDs(List<Long> list, long maxID) {

        long combinedID = 0;

        for (long id : list) {

            combinedID *= maxID + 1;
            combinedID += id;

            // Check for overflow
            if (combinedID < 0)
                throw new ArithmeticException(
                        "Overflow in Automaton.combineIDs() method. Consider using Automaton.combineBigIDs() method instead.");

        }

        return combinedID;

    }

    /**
     * Given a list of IDs and the largest possible value that could appear in the
     * list, create a unique combined ID using a {@link BigInteger}.
     * 
     * <p>
     * The order of the list matters. This method does not sort the list internally.
     * 
     * @param list  The list of IDs
     * @param maxID The largest possible value that could appear in the list
     *              (usually {@link Automaton#nStates})
     * 
     * @return The unique combined ID
     */
    public static BigInteger combineBigIDs(List<Long> list, long maxID) {

        BigInteger bigMaxID = BigInteger.valueOf(maxID);
        BigInteger maxIDPlusOne = bigMaxID.add(BigInteger.ONE);

        BigInteger combinedID = BigInteger.ZERO;

        for (long id : list)
            combinedID = combinedID.multiply(maxIDPlusOne).add(BigInteger.valueOf(id));

        return combinedID;

    }

    

    /**
     * Returns the original IDs as a list, given the combined ID and the maximum
     * possible value of IDs.
     * 
     * @param combinedID The combined ID
     * @param maxID      The largest possible value to be used as an ID
     * 
     * @return The original list of IDs
     */
    public static List<Long> separateIDs(long combinedID, long maxID) {

        List<Long> list = new ArrayList<Long>();

        while (combinedID > 0) {

            list.add(0, combinedID % (maxID + 1));
            combinedID /= (maxID + 1);

        }

        return list;

    }
}
