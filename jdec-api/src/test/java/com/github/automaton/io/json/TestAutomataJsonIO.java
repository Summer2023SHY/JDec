/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.json;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;

import org.junit.jupiter.api.*;

import com.github.automaton.automata.*;
import com.github.automaton.io.AutomatonIOAdapter;

@DisplayName("JSON I/O")
public class TestAutomataJsonIO {

    Automaton automaton;

    @BeforeEach
    public void testJsonInput() throws IOException {
        try {
            AutomatonIOAdapter jsonAdapter = new AutomatonJsonFileAdapter(new File("aut/fig-1.json"));
            automaton = jsonAdapter.getAutomaton();
        } catch (IOException e) {
            abort(e.getMessage());
        }
    }

    @Test
    @DisplayName("Automaton property tests")
    public void testAutomatonType() {
        assertEquals(Automaton.class, automaton.getClass());
        assertEquals(Automaton.Type.AUTOMATON, automaton.getType());

        assertEquals(2, automaton.getNumberOfControllers());
    }

    @Test
    @DisplayName("Automaton state tests")
    public void testStates() {
        assertEquals(1, automaton.getInitialStateID());
        assertEquals(5, automaton.getNumberOfStates());

        assertEquals("@0,F\n1,F\n2,F\n3,F\n4,F", automaton.getStateInput());
    }

    @Test
    @DisplayName("Automaton event tests")
    public void testEvents() {
        assertEquals(5, automaton.getNumberOfEvents());

        assertEquals("a1,TF,FF\na2,TF,FF\nb1,FT,FF\nb2,FT,FF\nsigma,FF,TT", automaton.getEventInput());
    }

    @Test
    @DisplayName("Automaton transiton tests")
    public void testTransitions() {
        assertEquals("0,a1,1\n0,a2,2\n1,b1,3\n1,b2,4\n2,b2,3\n2,b1,4\n3,sigma,3\n4,sigma,4:BAD", automaton.getTransitionInput());
    }
}
