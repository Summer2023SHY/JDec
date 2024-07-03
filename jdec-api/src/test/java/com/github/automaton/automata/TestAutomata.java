/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;

import com.github.automaton.io.input.AutomatonGenerator;

@SuppressWarnings("removal")
public class TestAutomata {

    private static Logger logger = LogManager.getLogger();

    @Nested
    @DisplayName("EVENT CREATION")
    class EventCreationTest {

        @Nested
        @DisplayName("Basic Event Creation Tests")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class BasicEventCreationTest {
            Automaton a;

            /* Basic Event Creation Tests */
            @BeforeAll
            void setup() {
                logger.debug("BASIC EVENT CREATION: ");

                logger.debug("Instantiating empty automaton...");
                a = new Automaton();
            }

            @Test
            @DisplayName("Adding an event that is controllable and observable")
            @Order(1)
            public void addFirstEvent() {
                logger.debug("Adding an event that is controllable and observable...");
                int id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
                logger.debug("Ensuring that 'events' set was expanded");
                assertEquals(1, a.getEvents().size());
                logger.debug("Ensuring that the added event is observable");
                assertTrue(a.getEvent(id).isObservable(0));
                logger.debug("Ensuring that the added event is controllable");
                assertTrue(a.getEvent(id).isControllable(0));
            }

            @Test
            @DisplayName("Adding an event that is observable, but not controllable")
            @Order(2)
            public void addSecondEvent() {
                logger.debug("Adding an event that is observable, but not controllable...");
                int id = a.addEventIfNonExisting("secondEvent", new boolean[] { true }, new boolean[] { false });
                logger.debug("Ensuring that 'events' set was expanded");
                assertEquals(2, a.getEvents().size());
                logger.debug("Ensuring that the added event is observable");
                assertTrue(a.getEvent(id).isObservable(0));
                logger.debug("Ensuring that the added event is not controllable");
                assertFalse(a.getEvent(id).isControllable(0));
            }

            @Test
            @DisplayName("Adding an event that is controllable, but not observable")
            @Order(3)
            public void addThirdEvent() {
                logger.debug("Adding an event that is controllable, but not observable...");
                int id = a.addEventIfNonExisting("thirdEvent", new boolean[] { false }, new boolean[] { true });
                logger.debug("Ensuring that 'events' set was expanded");
                assertEquals(3, a.getEvents().size());
                logger.debug("Ensuring that the added event is not observable");
                assertFalse(a.getEvent(id).isObservable(0));
                logger.debug("Ensuring that the added event is controllable");
                assertTrue(a.getEvent(id).isControllable(0));
            }

            @Test
            @DisplayName("Adding an event that neither controllable, nor observable")
            @Order(4)
            public void addFourthEvent() {
                logger.debug("Adding an event that neither controllable, nor observable...");
                int id = a.addEventIfNonExisting("fourthEvent", new boolean[] { false }, new boolean[] { false });
                logger.debug("Ensuring that 'events' set was expanded");
                assertEquals(4, a.getEvents().size());
                logger.debug("Ensuring that the added event is not observable");
                assertFalse(a.getEvent(id).isObservable(0));
                logger.debug("Ensuring that the added event is not controllable");
                assertFalse(a.getEvent(id).isControllable(0));
            }

            @Test
            @DisplayName("Adding a pre-existing event")
            @Order(5)
            public void addPreExistingEvent() {
                logger.debug("Adding a pre-existing event...");
                int id = a.addEventIfNonExisting("fourthEvent", new boolean[] { false }, new boolean[] { false });
                logger.debug("Ensuring that 'events' set was not expanded");
                assertEquals(4, a.getEvents().size());
                logger.debug("Ensuring that the method returned proper negative value");
                assertEquals(-4, id);
            }

        }

        @Test
        @DisplayName("Event ID Assignment Tests")
        public void testEventIDAssignment() {
            /* Event ID Assignment Tests */

            logger.debug("EVENT ID ASSIGNMENTS: ");

            logger.debug("Instantiating empty automaton...");
            Automaton a = new Automaton();

            logger.debug("Adding an event...");
            int id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
            logger.debug("Ensuring that the event's ID is 1");
            assertEquals(1, id);

            logger.debug("Adding a second event...");
            id = a.addEventIfNonExisting("secondEvent", new boolean[] { true }, new boolean[] { true });
            logger.debug("Ensuring that the event's ID is 2");
            assertEquals(2, id);

            logger.debug("Adding a pre-existing event...");
            id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
            logger.debug("Ensuring that the method returned proper negative value");
            assertEquals(-1, id);

        }

    }

    @Nested
    @DisplayName("STATE CREATION")
    class StateCreationTest {

        @Test
        @DisplayName("Basic State Creation Tests")
        public void testStateCreation() {
            /* Basic State Creation Tests */

            logger.debug("BASIC STATE CREATION: ");

            logger.debug("Instantiating empty automaton...");
            Automaton automaton = new Automaton();

            logger.debug("Adding a state that is marked...");
            long id = automaton.addState("firstState", true, false);
            logger.debug("Ensuring that 'nStates' was incremented");
            assertEquals(1, automaton.getNumberOfStates());
            logger.debug("Ensuring that the added state exists");
            assertTrue(automaton.stateExists(id));
            logger.debug("Ensuring that the added state was not labeled the initial state");
            assertEquals(0, automaton.getInitialStateID());
            logger.debug("Ensuring that the added state has the proper label");
            assertEquals("firstState", automaton.getState(id).getLabel());
            logger.debug("Ensuring that the added state is marked");
            assertTrue(automaton.getState(id).isMarked());

            logger.debug("Adding an initial state that is unmarked...");
            id = automaton.addState("secondState", false, true);
            logger.debug("Ensuring that 'nStates' was incremented");
            assertEquals(2, automaton.getNumberOfStates());
            logger.debug("Ensuring that the added state exists");
            assertTrue(automaton.stateExists(id));
            logger.debug("Ensuring that the added state was labeled the initial state");
            assertEquals(id, automaton.getInitialStateID());
            logger.debug("Ensuring that the added state has the proper label");
            assertEquals("secondState", automaton.getState(id).getLabel(), "secondState");
            logger.debug("Ensuring that the added state is unmarked");
            assertFalse(automaton.getState(id).isMarked());
        }

        @Test
        @DisplayName("State ID Assignment Tests")
        public void testStateIDAssignment() {
            /* State ID Assignment Tests */

            logger.debug("STATE ID ASSIGNMENTS: ");

            logger.debug("Instantiating empty automaton...");
            Automaton automaton = new Automaton();

            logger.debug("Adding a state...");
            long id = automaton.addState("firstState", true, true);
            logger.debug("Ensuring that the state's ID is 1");
            assertEquals(1, id);

            logger.debug("Adding a second state...");
            id = automaton.addState("secondState", true, true);
            logger.debug("Ensuring that the state's ID is 2");
            assertEquals(2, id);

        }

    }

    @Nested
    @DisplayName("GUI INPUT")
    class GuiInputTest {

        /* Basic GUI Input Tests */

        @Test
        @DisplayName("Simple GUI input code")
        public void basicInputTest1() {
            Automaton automaton = AutomatonGenerator.generateFromGUICode(
                    new Automaton(),
                    "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
                    "e,T\nf,F", // States
                    "e,a,f\nf,b,e" // Transitions
            );
            logger.debug("Ensuring the event input was saved and loaded correctly");
            assertMultiLineEquals("a,T,T\nb,T,F\nc,F,T\nd,F,F", automaton.getEventInput());
            logger.debug("Ensuring the state input was saved and loaded correctly");
            assertMultiLineEquals("e,T\nf,F", automaton.getStateInput());
            logger.debug("Ensuring the transition input was saved and loaded correctly");
            assertMultiLineEquals("e,a,f\nf,b,e", automaton.getTransitionInput());
        }

        @Test
        @DisplayName("GUI input code with duplicate labels, omitted optional parameters, and an initial state")
        public void basicInputTest2() {
            Automaton automaton = AutomatonGenerator.generateFromGUICode(
                    new Automaton(),
                    "a\nb,F,F\na,F,F\nb", // Events
                    "@c\nc,F", // States
                    StringUtils.EMPTY // Transitions
            );
            logger.debug("Ensuring the event input was saved and loaded correctly");
            assertMultiLineEquals("a,T,T\nb,F,F", automaton.getEventInput());
            logger.debug("Ensuring the state input was saved and loaded correctly");
            assertMultiLineEquals("@c,F", automaton.getStateInput());
            logger.debug("Ensuring the transition input was saved and loaded correctly");
            assertTrue(automaton.getTransitionInput().isEmpty());
        }

    }

    @Nested
    @DisplayName("AUTOMATA STANDARD OPERATIONS")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AutomataStandardOperationsTest {

        Automaton fig2_12;

        @Test
        @DisplayName("Co-Accessible Operation Tests")
        @Order(1)
        public void testCoAccessibleOperation() {
            /* Co-Accessible Operation Tests */

            logger.debug("CO-ACCESSIBLE OPERATION: ");

            logger.debug("Instantiating automaton from Figure 2.1...");
            fig2_12 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T\ng,T,T", // Events
                    "@zero,F\none,F\ntwo,T\nthree,F\nfour,F\nfive,F\nsix,F", // States
                    "zero,a,one\none,a,three\none,b,two\none,g,five\ntwo,g,zero\nthree,b,four\nfour,g,four\nfour,a,three\nsix,a,three\nsix,b,two" // Transitions
            );

            logger.debug(
                    "Taking the co-accessible part of Figure 2.12 (and comparing the result to the automaton in Figure 2.13a)...");
            Automaton result = fig2_12.coaccessible();

            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("a,T,T\nb,T,T\ng,T,T", result.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals("@zero,F\none,F\ntwo,T\nsix,F", result.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals("zero,a,one\none,b,two\ntwo,g,zero\nsix,b,two", result.getTransitionInput());
        }

        @Test
        @DisplayName("Trim Operation Tests")
        @Order(2)
        public void testTrimOperation() {
            /* Trim Operation Tests */

            logger.debug("TRIM OPERATION: ");

            logger.debug(
                    "Trimming the automaton in Figure 2.12 (and comparing the result to the automaton in Figure 2.13b)...");
            Automaton result = fig2_12.trim();

            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("a,T,T\nb,T,T\ng,T,T", result.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals("@zero,F\none,F\ntwo,T", result.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals("zero,a,one\none,b,two\ntwo,g,zero", result.getTransitionInput());
        }

        @Nested
        @DisplayName("Complement Operation Tests")
        @Order(3)
        class ComplementOperationTest {
            /* Complement Operation Tests */

            @Test
            @DisplayName("Taking the complement of an automaton")
            public void testComplement1() {

                Automaton result;

                logger.debug("Instantiating an automaton...");
                Automaton complementExample = AutomatonGenerator.generateFromGUICode(
                        new Automaton(3),
                        "a1,TFF,FFF\na2,TFF,FFF\nb1,FTF,FFF\nb2,FTF,FFF\nc1,FFT,FFF\nc2,FFT,FFF\no,FFF,TTT", // Events
                        "@0,F\n1,F\n2,F\n3,F\n4,F\n5,F\n6,F\n7,F\n8,F\n9,F\n10,F\n11,F\n12,F\n13,F\n14,F\n15,F\n16,F\n17,F\n18,F\n19,F", // States
                        "0,a1,4\n0,b2,3\n0,b1,2\n0,c1,1\n1,b2,6\n1,a2,5\n2,a1,7\n3,c2,8\n4,b1,9\n5,b1,10\n6,a1,11\n7,c2,12\n8,a2,13\n9,c1,14\n10,o,15\n11,o,16\n12,o,17\n13,o,18:BAD\n14,o,19:BAD" // Transitions
                );

                logger.debug("Taking the complement of the automaton...");

                try {

                    result = complementExample.complement();
                    logger.debug("Ensuring the events are correct");
                    assertMultiLineEquals(
                            "a1,TFF,FFF\na2,TFF,FFF\nb1,FTF,FFF\nb2,FTF,FFF\nc1,FFT,FFF\nc2,FFT,FFF\no,FFF,TTT",
                            result.getEventInput());
                    logger.debug("Ensuring the states are correct");
                    assertMultiLineEquals(
                            "@0,T\nDump State,F\n1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T\n8,T\n9,T\n10,T\n11,T\n12,T\n13,T\n14,T\n15,T\n16,T\n17,T\n18,T\n19,T",
                            result.getStateInput());
                    logger.debug("Ensuring the transitions are correct");
                    assertMultiLineEquals(
                            """
                                    0,a1,4\n0,b2,3\n0,b1,2\n0,c1,1\n0,a2,Dump State\n0,c2,Dump State\n0,o,Dump State
                                    1,b2,6\n1,a2,5\n1,a1,Dump State\n1,b1,Dump State\n1,c1,Dump State\n1,c2,Dump State\n1,o,Dump State
                                    2,a1,7\n2,a2,Dump State\n2,b1,Dump State\n2,b2,Dump State\n2,c1,Dump State\n2,c2,Dump State\n2,o,Dump State
                                    3,c2,8\n3,a1,Dump State\n3,a2,Dump State\n3,b1,Dump State\n3,b2,Dump State\n3,c1,Dump State\n3,o,Dump State
                                    4,b1,9\n4,a2,Dump State\n4,a1,Dump State\n4,b2,Dump State\n4,c1,Dump State\n4,c2,Dump State\n4,o,Dump State
                                    5,b1,10\n5,a2,Dump State\n5,a1,Dump State\n5,b2,Dump State\n5,c1,Dump State\n5,c2,Dump State\n5,o,Dump State
                                    6,a1,11\n6,a2,Dump State\n6,b1,Dump State\n6,b2,Dump State\n6,c1,Dump State\n6,c2,Dump State\n6,o,Dump State
                                    7,c2,12\n7,a1,Dump State\n7,a2,Dump State\n7,b1,Dump State\n7,b2,Dump State\n7,c1,Dump State\n7,o,Dump State
                                    8,a2,13\n8,a1,Dump State\n8,c2,Dump State\n8,b1,Dump State\n8,b2,Dump State\n8,c1,Dump State\n8,o,Dump State
                                    9,c1,14\n9,a1,Dump State\n9,a2,Dump State\n9,b1,Dump State\n9,b2,Dump State\n9,c2,Dump State\n9,o,Dump State
                                    10,o,15\n10,a1,Dump State\n10,a2,Dump State\n10,b1,Dump State\n10,b2,Dump State\n10,c2,Dump State\n10,c1,Dump State
                                    11,o,16\n11,a1,Dump State\n11,a2,Dump State\n11,b1,Dump State\n11,b2,Dump State\n11,c2,Dump State\n11,c1,Dump State
                                    12,o,17\n12,a1,Dump State\n12,a2,Dump State\n12,b1,Dump State\n12,b2,Dump State\n12,c2,Dump State\n12,c1,Dump State
                                    13,o,18:BAD\n13,a1,Dump State\n13,a2,Dump State\n13,b1,Dump State\n13,b2,Dump State\n13,c2,Dump State\n13,c1,Dump State
                                    14,o,19:BAD\n14,a1,Dump State\n14,a2,Dump State\n14,b1,Dump State\n14,b2,Dump State\n14,c2,Dump State\n14,c1,Dump State
                                    15,o,Dump State\n15,a1,Dump State\n15,a2,Dump State\n15,b1,Dump State\n15,b2,Dump State\n15,c2,Dump State\n15,c1,Dump State
                                    16,o,Dump State\n16,a1,Dump State\n16,a2,Dump State\n16,b1,Dump State\n16,b2,Dump State\n16,c2,Dump State\n16,c1,Dump State
                                    17,o,Dump State\n17,a1,Dump State\n17,a2,Dump State\n17,b1,Dump State\n17,b2,Dump State\n17,c2,Dump State\n17,c1,Dump State
                                    18,o,Dump State\n18,a1,Dump State\n18,a2,Dump State\n18,b1,Dump State\n18,b2,Dump State\n18,c2,Dump State\n18,c1,Dump State
                                    19,o,Dump State\n19,a1,Dump State\n19,a2,Dump State\n19,b1,Dump State\n19,b2,Dump State\n19,c2,Dump State\n19,c1,Dump State\n""",
                            result.getTransitionInput());

                } catch (OperationFailedException e) {
                    fail(e);
                }
            }

            @Test
            @DisplayName("Taking the complement of an automaton which will not need a dump state")
            public void testComplement2() {

                Automaton result;

                logger.debug("Instantiating an automaton...");
                Automaton complementExample2 = AutomatonGenerator.generateFromGUICode(
                        new Automaton(1),
                        "a,T,F\nb,T,T", // Events
                        "0,T\n1,F", // States
                        "0,a,1\n0,b,0\n1,a,0\n1,b,0" // Transitions
                );

                logger.debug("Taking the complement of the automaton which will not need a dump state...");

                try {

                    result = complementExample2.complement();
                    logger.debug("Ensuring the events are correct");
                    assertMultiLineEquals("a,T,F\nb,T,T", result.getEventInput());
                    logger.debug("Ensuring the states are correct");
                    assertMultiLineEquals("0,F\n1,T", result.getStateInput());
                    logger.debug("Ensuring the transitions are correct");
                    assertMultiLineEquals("0,a,1\n0,b,0\n1,a,0\n1,b,0", result.getTransitionInput());

                } catch (OperationFailedException e) {

                    fail(e);
                }
            }
        }

        @Test
        @DisplayName("Intersection Operation Tests")
        @Order(4)
        public void testIntersectionOperation() {

            /* Intersection Operation Tests */
            Automaton result;

            logger.debug("INTERSECTION OPERATION: ");

            logger.debug("Instantiating automaton from Figure 2.1...");
            Automaton fig2_1 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T\ng,T,T", // Events
                    "@x,T\ny,F\nz,T", // States
                    "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y" // Transitions
            );
            logger.debug("Instantiating automaton from Figure 2.2...");
            Automaton fig2_2 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T", // Events
                    "@zero,F\none,T", // States
                    "zero,b,zero\nzero,a,one\none,a,one\none,b,zero" // Transitions
            );

            logger.debug(
                    "Taking the intersection of Figure 2.1 and Figure 2.2 (and comparing the result to the first automaton in Figure 2.15)...");

            try {
                result = AutomataOperations.intersection(fig2_1, fig2_2);
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals("a,T,T\nb,T,T", result.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertMultiLineEquals("@x_zero,F\nx_one,T", result.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertMultiLineEquals("x_zero,a,x_one\nx_one,a,x_one", result.getTransitionInput());
            } catch (IncompatibleAutomataException e) {
                fail(e);
            }

            logger.debug("Instantiating automaton from Figure 2.13(b)...");
            Automaton fig2_13b = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T\ng,T,T", // Events
                    "@zero,F\none,F\ntwo,T", // States
                    "zero,a,one\none,b,two\ntwo,g,zero" // Transitions
            );

            logger.debug(
                    "Taking the intersection of Figure 2.2 and Figure 2.13(b) (and comparing the result to the second automaton in Figure 2.15)...");

            try {
                result = AutomataOperations.intersection(fig2_2, fig2_13b);
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals("a,T,T\nb,T,T", result.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertMultiLineEquals("@zero_zero,F\none_one,F\nzero_two,F", result.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertMultiLineEquals("zero_zero,a,one_one\none_one,b,zero_two", result.getTransitionInput());
            } catch (IncompatibleAutomataException e) {
                fail(e);
            }

            logger.debug("Instantiating the first automaton from Figure 2.20...");
            Automaton fig2_20a = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a1\na2\nb\nr", // Events
                    "@x1,F\nx2,F\nx3,T", // States
                    "x1,a1,x2\nx1,a2,x2\nx2,b,x3\nx3,r,x1" // Transitions
            );

            logger.debug("Instantiating the second automaton from Figure 2.20...");
            Automaton fig2_20b = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a1\nb\nc1\nr\na2\nc2", // Events
                    "@y1,F\ny2,F\ny3,F\ny4,F\ny5,F\ny6,F", // States
                    "y1,a1,y2\ny2,b,y4\ny4,r,y1\ny4,c1,y6\ny6,r,y1\ny1,a2,y3\ny3,b,y5\ny5,c2,y6\ny5,r,y1" // Transitions
            );

            logger.debug(
                    "Taking the intersection of the first two automata in Figure 2.20 (and comparing the result to the third automaton in Figure 2.20)...");
            try {
                result = AutomataOperations.intersection(fig2_20a, fig2_20b);
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals("a1,T,T\na2,T,T\nb,T,T\nr,T,T", result.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertMultiLineEquals("@x1_y1,F\nx2_y2,F\nx2_y3,F\nx3_y4,F\nx3_y5,F", result.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertMultiLineEquals(
                        "x1_y1,a1,x2_y2\nx1_y1,a2,x2_y3\nx2_y2,b,x3_y4\nx2_y3,b,x3_y5\nx3_y4,r,x1_y1\nx3_y5,r,x1_y1",
                        result.getTransitionInput());
            } catch (IncompatibleAutomataException e) {
                fail(e);
            }
        }

        @Test
        @DisplayName("Union Operation Tests")
        @Order(5)
        public void testUnionOperation() {
            /* Union Operation Tests */

            logger.debug("UNION OPERATION: ");

            logger.debug("Instantiating automaton from Figure 2.1...");
            Automaton fig2_1 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T\ng,T,T", // Events
                    "@x,T\ny,F\nz,T", // States
                    "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y" // Transitions
            );
            logger.debug("Instantiating automaton from Figure 2.2...");
            Automaton fig2_2 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T", // Events
                    "@zero,F\none,T", // States
                    "zero,b,zero\nzero,a,one\none,a,one\none,b,zero" // Transitions
            );

            logger.debug(
                    "Taking the union of Figure 2.1 and Figure 2.2 (and comparing the result to the automaton in Figure 2.16)...");

            Automaton result;

            try {
                result = AutomataOperations.union(fig2_1, fig2_2);
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals("a,T,T\nb,T,T\ng,T,T", result.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertMultiLineEquals("@x_zero,F\ny_zero,F\nz_zero,F\nx_one,T\ny_one,F\nz_one,T",
                        result.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertMultiLineEquals(
                        "x_zero,a,x_one\nx_zero,g,z_zero\ny_zero,b,y_zero\ny_zero,a,x_one\nz_zero,b,z_zero\nz_zero,a,y_one\nz_zero,g,y_zero\nx_one,a,x_one\nx_one,g,z_one\ny_one,b,y_zero\ny_one,a,x_one\nz_one,b,z_zero\nz_one,a,y_one\nz_one,g,y_one",
                        result.getTransitionInput());
            } catch (IncompatibleAutomataException e) {
                fail(e);
            }

            logger.debug("Instantiating the first automaton from Figure 2.17...");
            Automaton fig2_17a = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,T\nb,T,T\nc,T,T", // Events
                    "@one,T\ntwo,F", // States
                    "one,c,one\none,a,two\ntwo,b,two" // Transitions
            );

            logger.debug("Instantiating the second automaton from Figure 2.17...");
            Automaton fig2_17b = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "b,T,T\na,T,T\nd,T,T", // Events
                    "@A,T\nB,F", // States
                    "A,b,A\nA,a,B\nB,d,B" // Transitions
            );

            logger.debug("Instantiating the third automaton from Figure 2.17...");
            Automaton fig2_17c = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "c,T,T\nb,T,T\na,T,T", // Events
                    "@D,T\nE,F", // States
                    "D,c,D\nD,b,E\nE,a,E" // Transitions
            );

            logger.debug(
                    "Taking the union of the three automata in Figure 2.17 (and comparing the result to the automaton described in Example 2.17)...");

            try {
                result = AutomataOperations.union(AutomataOperations.union(fig2_17a, fig2_17b), fig2_17c);
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals("a,T,T\nb,T,T\nc,T,T\nd,T,T", result.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertEquals("@one_A_D,T", result.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertEquals("one_A_D,c,one_A_D", result.getTransitionInput());
            } catch (IncompatibleAutomataException e) {
                fail(e);
            }
        }
    }

    @Nested
    @Disabled
    @DisplayName("AUTOMATA SPECIAL OPERATIONS")
    class AutomataSpecialOperationsTest {

        UStructure uStructure;
        Automaton synchronizedCompositionExample = AutomatonGenerator.generateFromGUICode(
                new Automaton(2),
                "a,TF,TF\nb,FT,FT\no,TT,TF", // Events
                "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
        );

        @BeforeEach
        void setupCounter() {
            uStructure = synchronizedCompositionExample.synchronizedComposition();

        }

        @Test
        public void testSynchronizedCompositionOperation() {
            /* Synchronized Composition Operation Tests */

            logger.debug("SYNCHRONIZED COMPOSITION OPERATION: ");

            logger.debug("Instantiating an automaton...");
            Automaton synchronizedCompositionExample = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "a,TF,TF\nb,FT,FT\no,TT,TF", // Events
                    "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                    "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
            );

            logger.debug("Taking the U-Structure (expecting no conditional violations)...");
            UStructure uStructure = synchronizedCompositionExample.synchronizedComposition();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TF",
                    uStructure.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals(
                    "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7",
                    uStructure.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:UNCONDITIONAL_VIOLATION\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TF",
                    uStructure.getTransitionInput());

            logger.debug("Instantiating a simple automaton with a self-loop...");
            Automaton automatonSelfLoop = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,F,T", // Events
                    "@1,T", // States
                    "1,a,1" // Transitions
            );

            logger.debug("Instantiating an automaton...");
            synchronizedCompositionExample = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
                    "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                    "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
            );

            logger.debug("Taking the U-Structure of the automaton...");
            uStructure = synchronizedCompositionExample.synchronizedComposition();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT",
                    uStructure.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals(
                    "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7",
                    uStructure.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT",
                    uStructure.getTransitionInput());

            logger.debug("Taking the U-Structure of the automaton...");
            UStructure uStructureSelfLoop = automatonSelfLoop.synchronizedComposition();
            uStructureSelfLoop.generateInputForGUI();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,*>,F,F\n<*,a>,F,T", uStructureSelfLoop.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertEquals("@1_1", uStructureSelfLoop.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals("1_1,<a,*>,1_1\n1_1,<*,a>,1_1", uStructureSelfLoop.getTransitionInput());

            logger.debug("Instantiating a more complex automaton with a self-loop...");
            Automaton automatonSelfLoopExtended = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "a,TF,TT\nb,FT,FT", // Events
                    "@1,T\n2,T", // States
                    "1,b,2\n1,a,1" // Transitions
            );

            logger.debug("Taking the synchronized composition of the automaton...");
            UStructure uStructureSelfLoopExtended = automatonSelfLoopExtended.synchronizedComposition();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,a,*>,TF,TF\n<*,*,a>,FF,FT\n<*,b,*>,FF,FF\n<b,*,b>,FT,FT",
                    uStructureSelfLoopExtended.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals("@1_1_1\n1_2_1\n2_1_2\n2_2_2", uStructureSelfLoopExtended.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1_1,<b,*,b>,2_1_2\n1_1_1,<a,a,*>,1_1_1\n1_1_1,<*,b,*>,1_2_1\n1_1_1,<*,*,a>,1_1_1\n1_2_1,<b,*,b>,2_2_2\n1_2_1,<*,*,a>,1_2_1\n2_1_2,<*,b,*>,2_2_2",
                    uStructureSelfLoopExtended.getTransitionInput());

            logger.debug("Instantiating an automaton which brings out the special observability case...");
            Automaton automaton = AutomatonGenerator.generateFromGUICode(
                    new Automaton(1),
                    "a,T,F\nb,F,F\no,F,T", // Events
                    "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                    "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
            );

            logger.debug("Taking the synchronized composition of the automaton...");
            UStructure uStructure2 = automaton.synchronizedComposition();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,a>,T,F\n<b,*>,F,F\n<*,b>,F,F\n<o,*>,F,F\n<*,o>,F,T", uStructure2.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals(
                    "@1_1\n1_3\n2_2\n2_4\n2_5\n2_6\n2_7\n3_1\n3_3\n4_2\n4_4\n4_5\n4_6\n4_7\n5_2\n5_4\n5_5\n5_6\n5_7\n6_2\n6_4\n6_5\n6_6\n6_7\n7_2\n7_4\n7_5\n7_6\n7_7",
                    uStructure2.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1,<a,a>,2_2\n1_1,<b,*>,3_1\n1_1,<*,b>,1_3\n1_3,<a,a>,2_5\n1_3,<b,*>,3_3\n2_2,<b,*>,4_2\n2_2,<*,b>,2_4\n2_4,<b,*>,4_4\n2_4,<*,o>,2_6\n2_5,<b,*>,4_5\n2_5,<*,o>,2_7\n2_6,<b,*>,4_6\n2_7,<b,*>,4_7\n3_1,<a,a>,5_2\n3_1,<*,b>,3_3\n3_3,<a,a>,5_5\n4_2,<o,*>,6_2\n4_2,<*,b>,4_4\n4_4,<o,*>,6_4\n4_4,<*,o>,4_6\n4_5,<o,*>,6_5\n4_5,<*,o>,4_7\n4_6,<o,*>,6_6\n4_7,<o,*>,6_7\n5_2,<o,*>,7_2\n5_2,<*,b>,5_4\n5_4,<o,*>,7_4:UNCONDITIONAL_VIOLATION\n5_4,<*,o>,5_6:UNCONDITIONAL_VIOLATION\n5_5,<o,*>,7_5\n5_5,<*,o>,5_7\n5_6,<o,*>,7_6:UNCONDITIONAL_VIOLATION\n5_7,<o,*>,7_7\n6_2,<*,b>,6_4\n6_4,<*,o>,6_6\n6_5,<*,o>,6_7\n7_2,<*,b>,7_4\n7_4,<*,o>,7_6:UNCONDITIONAL_VIOLATION\n7_5,<*,o>,7_7",
                    uStructure2.getTransitionInput());
        }

        @Test
        @DisplayName("Add Communications Operation Tests")
        public void testCommunicationOperation() {
            /* Add Communications Operation Tests */

            logger.debug("ADD COMMUNICATIONS OPERATION: ");

            logger.debug(
                    "Add communications to the automaton generated by synchronized composition (Test case for GitHub Issue #9)...");
            UStructure addCommunications = null;

            try {

                addCommunications = uStructure.addCommunications();
                logger.debug("Ensuring the events are correct");
                assertMultiLineEquals(
                        "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF",
                        addCommunications.getEventInput());
                logger.debug("Ensuring the states are correct");
                assertMultiLineEquals(
                        "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7",
                        addCommunications.getStateInput());
                logger.debug("Ensuring the transitions are correct");
                assertMultiLineEquals(
                        "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:POTENTIAL_COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:POTENTIAL_COMMUNICATION-SR\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:POTENTIAL_COMMUNICATION-RS\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:POTENTIAL_COMMUNICATION-SR\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:POTENTIAL_COMMUNICATION-RS\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:POTENTIAL_COMMUNICATION-RS\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:POTENTIAL_COMMUNICATION-SR\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:POTENTIAL_COMMUNICATION-SR\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT",
                        addCommunications.getTransitionInput());

            } catch (NullPointerException e) {

                fail(e);

            }

            logger.debug(
                    "Add communications to the same automaton as above (but this time generated by GUI input code)...");
            logger.debug("Instantiating a U-Structure...");
            UStructure synchronizedComposition = AutomatonGenerator.generateFromGUICode(
                    new UStructure(2),
                    "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT", // Events
                    "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7", // States
                    "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT" // Transitions
            );
            addCommunications = synchronizedComposition.addCommunications();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals(
                    "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF",
                    addCommunications.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals(
                    "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7",
                    addCommunications.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:POTENTIAL_COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:POTENTIAL_COMMUNICATION-SR\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:POTENTIAL_COMMUNICATION-RS\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:POTENTIAL_COMMUNICATION-SR\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:POTENTIAL_COMMUNICATION-RS\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:POTENTIAL_COMMUNICATION-RS\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:POTENTIAL_COMMUNICATION-SR\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:POTENTIAL_COMMUNICATION-SR\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT",
                    addCommunications.getTransitionInput());
        }

        @Test
        @DisplayName("Feasible Protocol Operations Tests")
        public void testFeasibleProtocolOperations() {
            /* Feasible Protocol Operations Tests */
            logger.debug("Instantiating a U-Structure...");
            UStructure synchronizedComposition = AutomatonGenerator.generateFromGUICode(
                    new UStructure(2),
                    "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT", // Events
                    "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7", // States
                    "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT" // Transitions
            );

            logger.debug(
                    "Add communications to the automaton generated by synchronized composition (Test case for GitHub Issue #9)...");
            UStructure addCommunications = synchronizedComposition.addCommunications();

            logger.debug("FEASIBLE PROTOCOL OPERATIONS: ");

            logger.debug("Generate all feasible protocols in the automaton generated above...");
            List<Set<CommunicationData>> feasibleProtocols = addCommunications
                    .generateAllFeasibleProtocols(addCommunications.getPotentialCommunications(), false);
            logger.debug("Ensuring that there are 8 feasible protocols");
            assertEquals(8, feasibleProtocols.size());

            List<String> protocolsToString = protocolsToString(addCommunications, feasibleProtocols);
            logger.debug("Ensuring that protocol #1 is in the list");
            assertTrue(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n"));
            logger.debug("Ensuring that protocol #2 is in the list");
            assertTrue(protocolsToString
                    .contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n"));
            logger.debug("Ensuring that protocol #3 is in the list");
            assertTrue(protocolsToString
                    .contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #4 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #5 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n"));
            logger.debug("Ensuring that protocol #6 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<a,a,a>,2_2_2 (SR)\n1_3_1,<a,a,a>,2_5_2 (SR)\n3_1_3,<a,a,a>,5_2_5 (SR)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #7 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #8 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<a,a,a>,2_2_2 (SR)\n1_3_1,<a,a,a>,2_5_2 (SR)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_1_3,<a,a,a>,5_2_5 (SR)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));

            logger.debug("Generate smallest feasible protocols in the automaton generated above...");
            List<Set<CommunicationData>> smallestFeasibleProtocols = addCommunications
                    .generateSmallestFeasibleProtocols(addCommunications.getPotentialCommunications());
            logger.debug("Ensuring that there is 1 smallest feasible protocol");
            assertEquals(1, smallestFeasibleProtocols.size());
            logger.debug("Ensuring that the protocol is correct");
            assertTrue(protocolsToString(addCommunications, smallestFeasibleProtocols)
                    .contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n"));

            logger.debug("Generating the pruned automaton for the feasible protocol with 2 communications...");
            uStructure = addCommunications.applyProtocol(smallestFeasibleProtocols.get(0), true);
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals(
                    "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF",
                    uStructure.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals("@1_1_1\n2_2_2\n2_4_2\n3_3_3\n3_3_5\n4_2_4\n4_4_4\n5_5_3\n5_5_5\n6_6_6\n7_7_7",
                    uStructure.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    "1_1_1,<b,b,b>,3_3_3:COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:COMMUNICATION-SR\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_5,<a,a,*>,5_5_5\n4_2_4,<*,b,*>,4_4_4\n4_4_4,<o,o,o>,6_6_6\n5_5_3,<*,*,a>,5_5_5\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT",
                    uStructure.getTransitionInput());

            logger.debug("Try to make a protocol containing 1 communication feasible...");
            Set<CommunicationData> smallestProtocol = smallestFeasibleProtocols.get(0);
            smallestProtocol.remove(smallestProtocol.iterator().next());
            feasibleProtocols = addCommunications.makeProtocolFeasible(smallestProtocol);
            logger.debug("Ensuring that there are 6 feasible protocols");
            assertEquals(6, feasibleProtocols.size());
            logger.debug("Ensuring that protocol #1 is in the list");
            assertTrue(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n"));
            logger.debug("Ensuring that protocol #2 is in the list");
            assertTrue(protocolsToString
                    .contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n"));
            logger.debug("Ensuring that protocol #3 is in the list");
            assertTrue(protocolsToString
                    .contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #4 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));
            logger.debug("Ensuring that protocol #5 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n"));
            logger.debug("Ensuring that protocol #6 is in the list");
            assertTrue(protocolsToString.contains(
                    "1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n"));

            logger.debug("Try to make a protocol containing 7 communications feasible...");
            Set<CommunicationData> protocol = new HashSet<CommunicationData>(
                    addCommunications.getPotentialCommunications());
            protocol.remove(protocol.iterator().next());
            feasibleProtocols = addCommunications.makeProtocolFeasible(protocol);
            logger.debug("Ensuring that there are no feasible protocols");
            assertEquals(0, feasibleProtocols.size());
        }

    }

    @Nested
    @DisplayName("SPECIAL TRANSITIONS")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SpecialTransitionsTest {

        Automaton automaton;
        UStructure uStructure;

        @BeforeEach
        void setup() {
            automaton = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "a,TF,FF\nb,FT,FF\nc,TT,FT", // Events
                    "@0,F\n1,F\n2,F\n3,F\n4,F", // States
                    "0,a,1\n0,b,2\n1,c,3\n2,c,4:BAD" // Transitions
            );
        }

        @Test
        @DisplayName("Synchronized composition test")
        @Order(1)
        public void testSynchronizedCompositionOperation() {
            uStructure = automaton.synchronizedComposition();
            logger.debug("Ensuring the events are correct");
            assertMultiLineEquals("<a,a,*>,TF,FF\n<b,*,b>,FT,FF\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<c,c,c>,TT,FT",
                    uStructure.getEventInput());
            logger.debug("Ensuring the states are correct");
            assertMultiLineEquals("@0_0_0\n0_0_1\n0_2_0\n0_2_1\n1_1_0\n1_1_1\n2_0_2\n2_2_2\n3_3_3\n4_4_4",
                    uStructure.getStateInput());
            logger.debug("Ensuring the transitions are correct");
            assertMultiLineEquals(
                    """
                        0_0_0,<a,a,*>,1_1_0
                        0_0_0,<b,*,b>,2_0_2
                        0_0_0,<*,b,*>,0_2_0
                        0_0_0,<*,*,a>,0_0_1
                        1_1_0,<*,*,a>,1_1_1
                        2_0_2,<*,b,*>,2_2_2
                        0_2_0,<b,*,b>,2_2_2
                        0_2_0,<*,*,a>,0_2_1
                        0_0_1,<a,a,*>,1_1_1
                        0_0_1,<*,b,*>,0_2_1
                        1_1_1,<c,c,c>,3_3_3:CONDITIONAL_VIOLATION
                        2_2_2,<c,c,c>,4_4_4:UNCONDITIONAL_VIOLATION""",
                    uStructure.getTransitionInput());
        }

    }

    @Nested
    @DisplayName("TESTING FOR AUTOMATA PROPERTIES")
    class AutomataPropertiesTest {

        @Test
        @DisplayName("Controllability Tests")
        public void controllabilityTest() {
            /* Controllability Tests */

            logger.debug("Instantiating automaton...");
            Automaton a = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "c,TF,TF\nb,TF,TF\na,TF,TF", // Events
                    "@1,T\n2,F", // States
                    "1,c,1\n1,b,2:BAD\n2,a,2" // Transitions
            );
            logger.debug("Ensuring that the automaton is controllable");
            assertTrue(a.testControllability());

            logger.debug("Instantiating automaton...");
            a = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    "c,TF,TF\nb,TF,TF\na,TF,FF", // Events
                    "@1,T\n2,F", // States
                    "1,c,1:BAD\n1,b,2\n2,a,2:BAD" // Transitions
            );
            logger.debug("Ensuring that the automaton is not controllable");
            assertFalse(a.testControllability());
        }

    }

    @Nested
    @DisplayName("EXCEPTION HANDLING")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("Incompatible Event Test")
        public void testIncompatibleEvent() {

            /* IncompatibleAutomataException Tests */

            logger.debug("IncompatibleAutomataException Tests: ");

            logger.debug("Instantiating an automaton...");
            Automaton automaton1 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(),
                    "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
                    StringUtils.EMPTY, // States
                    StringUtils.EMPTY // Transitions
            );

            logger.debug("Instantiating a second automaton (with an incompatible event)...");
            Automaton automaton2 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(),
                    "a,T,T\nc,T,T\ne,T,F", // Events
                    StringUtils.EMPTY, // States
                    StringUtils.EMPTY // Transitions
            );

            assertThrows(IncompatibleAutomataException.class, () -> {
                logger.debug("Taking the union of the two instantiated automata...");
                AutomataOperations.union(automaton1, automaton2);
            }, "IncompatibleAutomataException not raised");

        }

        @Test
        @DisplayName("Test Different Number of Controllers")
        public void testIncompatibleNumControllers() {

            logger.debug("Instantiating an automaton...");
            Automaton automaton1 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(),
                    "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
                    StringUtils.EMPTY, // States
                    StringUtils.EMPTY // Transitions
            );

            logger.debug("Instantiating a third automaton (with different number of controllers)...");
            Automaton automaton3 = AutomatonGenerator.generateFromGUICode(
                    new Automaton(2),
                    StringUtils.EMPTY, // Events
                    StringUtils.EMPTY, // States
                    StringUtils.EMPTY // Transitions
            );

            assertThrows(IncompatibleAutomataException.class, () -> {
                logger.debug("Taking the union of the first and third instantiated automata...");
                AutomataOperations.union(automaton1, automaton3);
            }, "IncompatibleAutomataException not raised");
        }
    }

    private static List<String> protocolsToString(UStructure uStructure, List<Set<CommunicationData>> protocols) {

        List<String> list = new ArrayList<String>();

        for (Set<CommunicationData> protocol : protocols) {

            // Put each communication as a string into a list
            List<String> communications = new ArrayList<String>();
            for (CommunicationData data : protocol)
                communications.add(data.toString(uStructure) + StringUtils.LF);

            // Sort the list, so that the it is always in alphabetical order (meaning the
            // test cases are more consistent)
            Collections.sort(communications);

            // Put together the sorted strings
            StringBuilder stringBuilder = new StringBuilder();
            for (String str : communications)
                stringBuilder.append(str);

            list.add(stringBuilder.toString());

        }

        return list;

    }

    private static List<String> equilibriaToString(UStructure uStructure, List<Set<NashCommunicationData>> equilibria) {

        List<String> list = new ArrayList<String>();

        for (Set<NashCommunicationData> equilibrium : equilibria) {

            // Put each communication as a string into a list
            List<String> communications = new ArrayList<String>();
            for (NashCommunicationData data : equilibrium)
                communications.add(data.toNashString(uStructure) + StringUtils.LF);

            // Sort the list, so that the it is always in alphabetical order (meaning the
            // test cases are more consistent)
            Collections.sort(communications);

            // Put together the sorted strings
            StringBuilder stringBuilder = new StringBuilder();
            for (String str : communications)
                stringBuilder.append(str);

            list.add(stringBuilder.toString());

        }

        return list;

    }

    private static void assertMultiLineEquals(String expected, String actual) {
        assertStringArrayEqualsIgnoreOrder(expected.split("\r?\n"), actual.split("\r?\n"));
    }

    private static void assertStringArrayEqualsIgnoreOrder(String[] expected, String[] actual) {
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertIterableEquals(Arrays.asList(expected), Arrays.asList(actual),
                String.format("Expected %s but was %s", Arrays.toString(expected), Arrays.toString(actual)));
    }

}
