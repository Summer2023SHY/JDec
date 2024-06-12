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
@SuppressWarnings("removal")
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

        assertArrayEquals(new String[] {"@0,F", "1,F", "2,F", "3,F", "4,F"}, automaton.getStateInput().split(System.lineSeparator()));
    }

    @Test
    @DisplayName("Automaton event tests")
    public void testEvents() {
        assertEquals(5, automaton.getNumberOfEvents());

        assertArrayEquals(new String[] {"a1,TF,FF", "a2,TF,FF", "b1,FT,FF", "b2,FT,FF", "sigma,FF,TT"}, automaton.getEventInput().split(System.lineSeparator()));
    }

    @Test
    @DisplayName("Automaton transiton tests")
    public void testTransitions() {
        assertArrayEquals(new String[] {"0,a1,1", "0,a2,2", "1,b1,3", "1,b2,4", "2,b2,3", "2,b1,4", "3,sigma,3", "4,sigma,4:BAD"}, automaton.getTransitionInput().split(System.lineSeparator()));
    }
}
