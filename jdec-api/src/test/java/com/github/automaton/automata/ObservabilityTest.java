/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.github.automaton.io.input.AutomatonGenerator;
import com.github.automaton.io.json.AutomatonJsonFileAdapter;

@DisplayName("Inference Observability Test")
public class ObservabilityTest {

    @ParameterizedTest(name = "Test {index}")
    @MethodSource
    @Disabled
    @DisplayName("Test Observable Automata")
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testObservableAutomata(Automaton automaton) {
        assertTrue(automaton.testObservability());
    }

    @ParameterizedTest(name = "Test {index}")
    @MethodSource("testObservableAutomata")
    @DisplayName("Test Ambiguity Levels for Observable Automata")
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testAmbiguityLevel(Automaton automaton) {
        Pair<Boolean, OptionalInt /* List<AmbiguityData> */> result = automaton.testObservability(true);
        assertTrue(result.getLeft());

        assertDoesNotThrow(() -> {
            assertTrue(result.getRight().isPresent());
            assertTrue(result.getRight().getAsInt() != Integer.MAX_VALUE);
        });
        
    }

    static Automaton[] testObservableAutomata() throws IOException {
        AutomatonJsonFileAdapter fig6Adapter = new AutomatonJsonFileAdapter(new File("aut/fig-6.json"));
        return new Automaton[] {
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        """
                                alpha,TF,TF
                                beta,FT,FT
                                gamma,FF,TT""", // Events
                        """
                                @0,F
                                1,F
                                2,F
                                3,F
                                4,F
                                5,F
                                6,F
                                7,F
                                8,F""", // States
                        """
                                0,beta,1
                                0,alpha,2
                                1,gamma,3
                                1,beta,4:BAD
                                2,gamma,5:BAD
                                2,beta,6
                                4,gamma,7:BAD
                                6,gamma,8:BAD""" // Transitions
                ),
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        """
                                alpha,TF,FF
                                beta,FT,FF
                                gamma,FF,TT
                                delta,TF,FF
                                mu,TT,FF""", // Events
                        """
                                @0,F
                                1,F
                                2,F
                                3,F
                                4,F
                                5,F
                                6,F
                                7,F
                                8,F""", // States
                        """
                                0,gamma,1
                                0,mu,2
                                0,beta,3
                                0,alpha,4
                                2,gamma,5
                                3,beta,2
                                3,gamma,7:BAD
                                3,delta,6
                                4,gamma,7:BAD
                                6,gamma,8""" // Transitions
                ),
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        """
                                a1,TF,FF
                                a2,TF,FF
                                a3,TF,FF
                                a4,TF,FF
                                b1,FT,FF
                                b2,FT,FF
                                b3,FT,FF
                                b4,FT,FF
                                sigma,FF,TT""", // Events
                        """
                                @0,F
                                1,F
                                2,F
                                3,F
                                4,F
                                5,F
                                6,F
                                7,F
                                8,F
                                9,F
                                10,F
                                11,F
                                12,F""", // States
                        """
                                0,a1,1
                                0,a2,2
                                0,a3,3
                                0,a4,4
                                1,b1,5
                                1,b2,6
                                2,b2,7
                                2,b3,8
                                3,b3,9
                                3,b4,10
                                4,b4,11
                                4,b1,12
                                5,sigma,5
                                6,sigma,6:BAD
                                7,sigma,7
                                8,sigma,8:BAD
                                9,sigma,9
                                10,sigma,10:BAD
                                11,sigma,11:BAD
                                12,sigma,12""" // Transitions
                ),
                fig6Adapter.getAutomaton()
        };
    }

    @ParameterizedTest(name = "Test {index}")
    @MethodSource
    @DisplayName("Test Unobservable Automata")
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testUnobservableAutomata(Automaton automaton) {
        assertFalse(automaton.testObservability());
    }

    static Automaton[] testUnobservableAutomata() throws IOException {
        AutomatonJsonFileAdapter fig1Adapter = new AutomatonJsonFileAdapter(new File("aut/fig-1.json"));
        return new Automaton[] {
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        """
                                a1,TF,FF
                                a2,TF,FF
                                b1,FT,FF
                                b2,FT,FF
                                sigma,FF,TT""", // Events

                        """
                                @1,F
                                2,F
                                3,F
                                4,F
                                5,F
                                6,F
                                7,F""", // States

                        """
                                1,a1,2
                                1,a2,3
                                2,b1,4
                                2,b2,5
                                3,b1,6
                                3,b2,7
                                4,sigma,4
                                5,sigma,5:BAD
                                6,sigma,6:BAD
                                7,sigma,7""" // Transitions
                ),
                fig1Adapter.getAutomaton()
        };
    }

}
