/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util.bipartite;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.ListValuedMap;

import com.github.automaton.automata.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Provides methods for exporting bipartite graph(s) that are used for
 * observability tests.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class BipartiteGraphExport {

    /** Private constructor */
    private BipartiteGraphExport() {
    }

    /**
     * Generates bipartite graph(s) of an automaton and returns it as a
     * JSON object
     * 
     * @param automaton an automaton
     * @return JSON object that stores bipartite graph(s)
     * 
     * @throws NullPointerException if argument is {@code null}
     */
    public static JsonObject generateBipartiteGraphJson(Automaton automaton) {
        Objects.requireNonNull(automaton);
        var graphs = AutomataOperations.generateBipartiteGraph(automaton);
        JsonObject graphJsonObject = new JsonObject();
        for (var graphEntry : graphs.entrySet()) {
            Event graphEvent = graphEntry.getKey();
            ListValuedMap<State, Set<State>> graph = graphEntry.getValue();
            JsonObject graphJson = new JsonObject();
            for (var state : graph.keySet()) {
                var edges = graph.get(state);
                JsonArray edgesJson = new JsonArray();
                for (int controller = 0; controller < edges.size(); controller++) {
                    JsonArray edgeJson = new JsonArray();
                    for (State s : edges.get(controller)) {
                        edgeJson.add(s.getLabel());
                    }
                    edgesJson.add(edgeJson);
                }
                graphJson.add(state.getLabel(), edgesJson);
            }
            graphJsonObject.add(graphEvent.getLabel(), graphJson);
        }
        return graphJsonObject;
    }
}
