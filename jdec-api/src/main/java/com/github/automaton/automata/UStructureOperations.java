/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of U-Structure operations.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
public class UStructureOperations {

    /** Cached values of factorials */
    private static int[] factorial = new int[13];

    /**
     * Recursively find the factorial of the specified number.
     * 
     * @param n The number to take the factorial of, must be in the range [0,12]
     * @return The factorial value
     * 
     * @throws ArithmeticException if {@code n} is outside allowed range
     **/
    private static int factorial(int n) {

        // Error checking
        if (n < 0 || n > 12) {
            throw new ArithmeticException("Factorial value of " + n + " is outside allowed range.");
        }

        if (factorial[n] == 0) {
            // Base case
            if (n == 0)
                factorial[n] = 1;
            else
                factorial[n] = n * factorial(n - 1);
        }

        return factorial[n];
    }


    /**
     * Given the Shapley values for each coalition, and the index of a controller,
     * calculate its Shapley value.
     * NOTE: This calculation is specified in the paper 'Coalitions of the willing:
     * Decentralized discrete-event
     * control as a cooperative game', in section 3.
     * 
     * @param shapleyValues     The mappings between the coalitions and their
     *                          associated Shapley values
     * @param indexOfController The index of the controller (1-based)
     * @return The Shapley value of the specified controller
     **/
    public static double findShapleyValueForController(UStructure uStructure, Map<Set<Integer>, Integer> shapleyValues, int indexOfController) {

        int sum = 0;

        // Iterate through each coalition
        for (Map.Entry<Set<Integer>, Integer> entry : shapleyValues.entrySet()) {
            Set<Integer> coalition = entry.getKey();

            // Skip this coalition if it contains the controller
            if (coalition.contains(indexOfController))
                continue;

            Integer shapleyValueWithoutController = entry.getValue();

            // Find the Shapley value of this coalition if the controller were to be added
            Set<Integer> coalitionWithController = new HashSet<Integer>(coalition);
            coalitionWithController.add(indexOfController);
            Integer shapleyValueWithController = shapleyValues.get(coalitionWithController);

            // Add calculated value to summation
            sum += factorial(coalition.size())
                    * factorial(uStructure.getNumberOfControllers() - coalition.size() - 1)
                    * (shapleyValueWithController - shapleyValueWithoutController);

        }

        return (double) sum / (double) factorial(uStructure.getNumberOfControllers());

    }
}
