/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util.bipartite;

import static guru.nidi.graphviz.model.Factory.*;

import java.util.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.collections4.ListValuedMap;

import com.github.automaton.automata.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.*;

/**
 * Provides methods for exporting bipartite graph(s) that are used for
 * observability tests.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class BipartiteGraphExport {

    public static final List<Color> COLORS = List.of(
        Color.ORANGE, Color.BLUE, Color.INDIGO, Color.MAGENTA, Color.VIOLET, Color.rgb(0xDC9C34)
    );

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

    private static class BipartiteGraphEdge {
        public final String startLabel;
        public final String endLabel;
        public final String edgeLabel;

        BipartiteGraphEdge(String startLabel, String endLabel, String edgeLabel) {
            if (startLabel.compareTo(endLabel) > 0) {
                this.startLabel = endLabel;
                this.endLabel = startLabel;
            }
            else if (startLabel.compareTo(endLabel) < 0) {
                this.startLabel = startLabel;
                this.endLabel = endLabel;
            }
            else {
                this.startLabel = this.endLabel = startLabel;
            }
            this.edgeLabel = edgeLabel;
        }

        BipartiteGraphEdge(State startState, State endState, String edgeLabel) {
            this(startState.getLabel(), endState.getLabel(), edgeLabel);
        }

        @Override
        public int hashCode() {
            String[] startEndLabels = { this.startLabel, this.endLabel };
            Arrays.sort(startEndLabels);
            return Objects.hash(startEndLabels[0], startEndLabels[1], this.edgeLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            else if (obj instanceof BipartiteGraphEdge otherEdge) {
                if (!Objects.equals(this.edgeLabel, otherEdge.edgeLabel))
                    return false;
                else if (Objects.equals(this.startLabel, otherEdge.startLabel)
                        && Objects.equals(this.endLabel, otherEdge.endLabel))
                    return true;
                else
                    return (Objects.equals(this.startLabel, otherEdge.endLabel)
                            && Objects.equals(this.endLabel, otherEdge.startLabel));
            } else
                return false;
        }
    }

    public static MutableGraph generateBipartiteGraph(Automaton automaton, String eventLabel) {
        return generateBipartiteGraph(automaton, eventLabel, COLORS);
    }

    public static MutableGraph generateBipartiteGraph(Automaton automaton, String eventLabel, List<Color> colors) {
        Objects.requireNonNull(automaton);
        Objects.requireNonNull(eventLabel);
        if (eventLabel.isEmpty())
            throw new IllegalArgumentException();
        final var event = automaton.getEvent(eventLabel);
        if (!BooleanUtils.or(event.isControllable()))
            throw new IllegalArgumentException(String.format("\"%s\" is not controllable", eventLabel));
        var graphs = AutomataOperations.generateBipartiteGraph(automaton);
        var graph = graphs.get(event);

        MutableGraph g = mutGraph().setDirected(false);
        g.graphAttrs().add(
                GraphAttr.splines(GraphAttr.SplineMode.LINE),
                Rank.sep(2));

        Set<BipartiteGraphEdge> edgeSet = new HashSet<>();
        Map<String, MutableNode> nodeMap = Collections.synchronizedMap(new HashMap<>());

        graph.keySet().forEach(state -> {
            MutableNode node = mutNode(state.getLabel());
            nodeMap.put(state.getLabel(), node);
            if (state.isIllegalConfiguration()) {
                node.add(Shape.DOUBLE_CIRCLE);
            } else {
                node.add(Shape.CIRCLE);
            }
            if (state.isEnablementState()) {
                node.add(Color.GREEN3);
                g.add(node);
            } else if (state.isDisablementState()) {
                node.add(Color.RED);
                g.add(node);
            }
        });

        for (var keyState : graph.keySet()) {
            var allNeighborStates = graph.get(keyState);
            for (int i = 0; i < allNeighborStates.size(); i++) {
                var neighborStates = allNeighborStates.get(i);
                for (State targetState : neighborStates) {
                    var workingEdge = new BipartiteGraphEdge(keyState, targetState, Integer.toString(i + 1));
                    if (!edgeSet.contains(workingEdge)) {
                        edgeSet.add(workingEdge);
                    }
                }
                
            }
        }

        for (var edge : edgeSet) {
            var sourceNode = nodeMap.get(edge.startLabel);
            var targetNode = nodeMap.get(edge.endLabel);
            targetNode.addTo(g);
            Link l = sourceNode.linkTo(targetNode);
            //l.add(Label.of((edge.edgeLabel)));
            l.add(colors.get(Integer.parseInt(edge.edgeLabel) - 1));
            sourceNode.links().add(l);
            //g.links().add(l);
        }

        return g;
    }
}
