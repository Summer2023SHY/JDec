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
                        "alpha,TF,TF\n" + //
                                "beta,FT,FT\n" + //
                                "gamma,FF,TT", // Events
                        "@0,F\n" + //
                                "1,F\n" + //
                                "2,F\n" + //
                                "3,F\n" + //
                                "4,F\n" + //
                                "5,F\n" + //
                                "6,F\n" + //
                                "7,F\n" + //
                                "8,F", // States
                        "0,beta,1\n" + //
                                "0,alpha,2\n" + //
                                "1,gamma,3\n" + //
                                "1,beta,4:BAD\n" + //
                                "2,gamma,5:BAD\n" + //
                                "2,beta,6\n" + //
                                "4,gamma,7:BAD\n" + //
                                "6,gamma,8:BAD" // Transitions
                ),
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        "alpha,TF,FF\n" + //
                                "beta,FT,FF\n" + //
                                "gamma,FF,TT\n" + //
                                "delta,TF,FF\n" + //
                                "mu,TT,FF", // Events
                        "@0,F\n" + //
                                "1,F\n" + //
                                "2,F\n" + //
                                "3,F\n" + //
                                "4,F\n" + //
                                "5,F\n" + //
                                "6,F\n" + //
                                "7,F\n" + //
                                "8,F", // States
                        "0,gamma,1\n" + //
                                "0,mu,2\n" + //
                                "0,beta,3\n" + //
                                "0,alpha,4\n" + //
                                "2,gamma,5\n" + //
                                "3,beta,2\n" + //
                                "3,gamma,7:BAD\n" + //
                                "3,delta,6\n" + //
                                "4,gamma,7:BAD\n" + //
                                "6,gamma,8" // Transitions
                ),
                AutomatonGenerator.generateFromGUICode(
                        new Automaton(2),
                        "a1,TF,FF\n" + //
                                "a2,TF,FF\n" + //
                                "a3,TF,FF\n" + //
                                "a4,TF,FF\n" + //
                                "b1,FT,FF\n" + //
                                "b2,FT,FF\n" + //
                                "b3,FT,FF\n" + //
                                "b4,FT,FF\n" + //
                                "sigma,FF,TT", // Events
                        "@0,F\n" + //
                                "1,F\n" + //
                                "2,F\n" + //
                                "3,F\n" + //
                                "4,F\n" + //
                                "5,F\n" + //
                                "6,F\n" + //
                                "7,F\n" + //
                                "8,F\n" + //
                                "9,F\n" + //
                                "10,F\n" + //
                                "11,F\n" + //
                                "12,F", // States
                        "0,a1,1\n" + //
                                "0,a2,2\n" + //
                                "0,a3,3\n" + //
                                "0,a4,4\n" + //
                                "1,b1,5\n" + //
                                "1,b2,6\n" + //
                                "2,b2,7\n" + //
                                "2,b3,8\n" + //
                                "3,b3,9\n" + //
                                "3,b4,10\n" + //
                                "4,b4,11\n" + //
                                "4,b1,12\n" + //
                                "5,sigma,5\n" + //
                                "6,sigma,6:BAD\n" + //
                                "7,sigma,7\n" + //
                                "8,sigma,8:BAD\n" + //
                                "9,sigma,9\n" + //
                                "10,sigma,10:BAD\n" + //
                                "11,sigma,11:BAD\n" + //
                                "12,sigma,12" // Transitions
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
                        "a1,TF,FF\n" +
                                "a2,TF,FF\n" +
                                "b1,FT,FF\n" +
                                "b2,FT,FF\n" +
                                "sigma,FF,TT", // Events

                        "@1,F\n" +
                                "2,F\n" +
                                "3,F\n" +
                                "4,F\n" +
                                "5,F\n" +
                                "6,F\n" +
                                "7,F", // States

                        "1,a1,2\n" +
                                "1,a2,3\n" +
                                "2,b1,4\n" +
                                "2,b2,5\n" +
                                "3,b1,6\n" +
                                "3,b2,7\n" +
                                "4,sigma,4\n" +
                                "5,sigma,5:BAD\n" +
                                "6,sigma,6:BAD\n" +
                                "7,sigma,7" // Transitions
                ),
                fig1Adapter.getAutomaton()
        };
    }

}
