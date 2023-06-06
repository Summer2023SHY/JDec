package com.github.automaton.io.json;

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

        automaton.generateInputForGUI();

        assertEquals("@0,F\n1,F\n2,F\n3,F\n4,F", automaton.getStateInput());
    }

    @Test
    @DisplayName("Automaton event tests")
    public void testEvents() {
        assertEquals(5, automaton.getNumberOfEvents());

        automaton.generateInputForGUI();

        assertEquals("a1,TF,FF\na2,TF,FF\nb1,FT,FF\nb2,FT,FF\nsigma,FF,TT", automaton.getEventInput());
    }

    @Test
    @DisplayName("Automaton transiton tests")
    public void testTransitions() {
        automaton.generateInputForGUI();

        assertEquals("0,a1,1\n0,a2,2\n1,b1,3\n1,b2,4\n2,b2,3\n2,b1,4\n3,sigma,3\n4,sigma,4:BAD", automaton.getTransitionInput());
    }
}
