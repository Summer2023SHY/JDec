package com.github.automaton.automata;

/* 
 * Copyright (C) 2016 Micah Stairs
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;

import com.github.automaton.io.input.AutomatonGenerator;

@DisplayName("Helper Method Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HelperMethodTest {

    static Logger logger = LogManager.getLogger();

    Automaton automaton;
    List<List<String>> labelSequences;

    @Test
    @DisplayName("Liu's Thesis - findCounterExample()")
    @Order(1)
    public void testFindCounterExample() {

        logger.debug("Instantiating an automaton...");
        automaton = AutomatonGenerator.generateFromGUICode(
                new Automaton(2),
                "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
                "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
        );

        logger.debug("Taking the U-Structure of the automaton...");
        UStructure uStructure = automaton.synchronizedComposition();

        logger.debug("Finding the counter-example...");
        labelSequences = uStructure.findCounterExample(true);
        logger.debug("Ensuring that the 0th sequence is correct");
        assertIterableEquals(List.of("b", "a", "o"), labelSequences.get(0));
        logger.debug("Ensuring that the 1st sequence is correct");
        assertIterableEquals(List.of("a", "b", "o"), labelSequences.get(1));
        logger.debug("Ensuring that the 2nd sequence is correct");
        assertIterableEquals(List.of("a", "b", "o"), labelSequences.get(2));
    }

    @Test
    @DisplayName("Liu's Thesis - acceptsCounterExample()")
    @Order(2)
    public void testAcceptsCounterExample() {
        /* acceptsCounterExample() Tests */

        logger.debug("Ensuring that the original automaton can accept the counter-example");
        assertEquals(-1, automaton.acceptsCounterExample(labelSequences));
        labelSequences.add(List.of("b", "o", "o"));
        logger.debug("Adding a bad sequence to the list...", 3);

        logger.debug("Ensuring that the original automaton can no longer accept the counter-example");
        assertEquals(11, automaton.acceptsCounterExample(labelSequences));
    }

    @Test
    @DisplayName("createCombinedIDWithOrderedSet() Tests")
    public void testCreateCombinedIDWithOrderedSet() {
        /* createCombinedIDWithOrderedSet() Tests */

        logger.debug("Combining IDs - combineIDs()");

        List<Long> list = List.of(4L, 2L, 7L);
        logger.debug("Ensuring that {4,2,7} with a max ID of 7 maps to 279");
        assertEquals(279, Automaton.combineIDs(list, 7));

        logger.debug("Separating IDs - separateIDs()");
        logger.debug("Ensuring that 279 with a max ID of 7 maps back to {4,2,7}");
        assertIterableEquals(list, Automaton.separateIDs(279, 7));
    }

    @Test
    @DisplayName("Splitting Strings that contain Vectors - splitStringWithVectors()")
    public void testSplitStringWithVectors() {
        /* splitStringWithVectors() Tests */

        logger.debug("Splitting a string with no vectors");
        assertArrayEquals(new String[] { "One", "Two" }, AutomatonGenerator.splitStringWithVectors("One,Two"));
        logger.debug("Splitting a string containing vectors");
        assertArrayEquals(new String[] { "<A,B>", "C", "<D,E,F>" },
                AutomatonGenerator.splitStringWithVectors("<A,B>,C,<D,E,F>"));
        logger.debug("Splitting a string with mismatched angled brackets (expecting null)");
        assertNull(AutomatonGenerator.splitStringWithVectors("<A,B>,C>"));
    }

    @Test
    @DisplayName("Pareto Ranks - getParetoRanks()")
    public void testParetoHelperMethod() {

        /* Pareto Helper Method Tests */

        int[] x = { 1, 2, 3, 3, 5, 5, 7, 8 };
        int[] y = { 2, 6, 2, 7, 5, 2, 4, 1 };
        List<Integer> expectedIndexes = List.of(4, 2, 3, 1, 1, 2, 1, 1);
        logger.debug("Ensuring that the Pareto ranks could be generated");
        assertIterableEquals(expectedIndexes, Arrays.asList(getParetoRanks(x, y)));
    }

    private static Integer[] getParetoRanks(int[] objective1, int[] objective2) {

        if (objective1.length != objective2.length)
          return null;
    
        int nIndividuals = objective1.length;
        Integer[] ranks = new Integer[nIndividuals];
        boolean[] isAssigned = new boolean[nIndividuals];
        int nRanksAssigned = 0;
        int currentRank = 1;
    
        // Repeat process until all individuals have been assigned a rank
        while (nRanksAssigned < nIndividuals) {
    
            /* Find the next pareto front, assigning those individuals the proper rank */
    
          List<Integer> paretoFront = getParetoFront(objective1, objective2, isAssigned);
    
          for (int i = 0; i < paretoFront.size(); i++) {
            ranks[paretoFront.get(i)] = currentRank;
            isAssigned[paretoFront.get(i)] = true;
          }
    
          currentRank++;
          nRanksAssigned += paretoFront.size();
    
        }
    
        return ranks;
    
      }
    
      /**
       *
       * @param alreadyUsed An array of booleans indicating which individuals have already been used, and are
       *                    no longer eligible to be considered as part of the pareto front.
       *                    NOTE: To consider all individuals, pass an array filled with false values
       **/
      private static ArrayList<Integer> getParetoFront(int[] objective1, int[] objective2, boolean[] alreadyUsed) {
    
        // Error checking
        if (objective1.length != objective2.length || objective1.length != alreadyUsed.length)
          return null;
    
        // Setup
        int nIndividuals = objective1.length;
        ArrayList<Integer> individualsInFront = new ArrayList<Integer>();
    
        // Build pareto front
        for (int i = 0; i < nIndividuals; i++) {
    
          // Skip this individual if they cannot be part of this pareto front
          if (alreadyUsed[i])
            continue;
    
          List<Integer> individualsToRemoveFromFront = new ArrayList<Integer>();
    
          boolean partOfFront = true;
    
          for (int other : individualsInFront) {
    
            if (paretoDominates(objective1[i], objective2[i], objective1[other], objective2[other]))
              individualsToRemoveFromFront.add(other);
            
            else if (paretoDominates(objective1[other], objective2[other], objective1[i], objective2[i])) {
              partOfFront = false;
              break;
            }
    
          }
    
          for (Integer individual : individualsToRemoveFromFront)
            individualsInFront.remove(individual);
    
          if (partOfFront)
            individualsInFront.add(i);
    
        }
    
        return individualsInFront;
    
      }
    
      private static boolean paretoDominates(int x1, int y1, int x2, int y2) {
    
        // It does not pareto dominate if it is weaker in some way
        if (x1 < x2 || y1 < y2)
          return false;
    
        // It does not pareto dominate if they are equal in every respect
        if (x1 == x2 && y1 == y2)
          return false;
    
        // Otherwise, it must pareto dominate
        return true;
    
      }}
