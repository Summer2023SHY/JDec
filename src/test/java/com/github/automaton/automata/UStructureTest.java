package com.github.automaton.automata;

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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("U Structure Property Test")
public class UStructureTest {
    @ParameterizedTest
    @MethodSource({"com.github.automaton.automata.ObservabilityTest#testObservableAutomata", "com.github.automaton.automata.ObservabilityTest#testUnobservableAutomata"})
    public void testRelabeling(Automaton automaton) {
        UStructure origUStructure = automaton.synchronizedComposition();
        UStructure relabel1 = origUStructure.relabelConfigurationStates();
        UStructure relabel2 = relabel1.relabelConfigurationStates();
        
        assertTrue(relabel1.getNumberOfStates() == relabel2.getNumberOfStates());
        assertTrue(relabel1.getNumberOfTransitions() == relabel2.getNumberOfTransitions());
    }
}
