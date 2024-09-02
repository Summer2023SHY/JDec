/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.incremental;

import java.util.Set;

import com.github.automaton.automata.Automaton;

/**
 * A generator interface for {@link ComponentIterable}s.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
@FunctionalInterface
public interface ComponentIterableGenerator {
    public ComponentIterable generate(Set<Automaton> plants, Set<Automaton> specs);
}
