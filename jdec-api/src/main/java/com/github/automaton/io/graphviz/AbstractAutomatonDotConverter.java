/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.io.graphviz;

import static guru.nidi.graphviz.model.Factory.*;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.*;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;

/**
 * Abstract implementation of an {@link AutomatonDotConverter}.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 2.1.0
 */
abstract class AbstractAutomatonDotConverter<T extends Automaton> implements AutomatonDotConverter<T> {

    private static Logger logger = LogManager.getLogger(AutomatonDotConverter.class);

    protected T automaton;

    protected AbstractAutomatonDotConverter(T automaton) {
        this.automaton = Objects.requireNonNull(automaton);
    }

    @Override
    public final boolean generateImage(String outputFileName) {
        Objects.requireNonNull(outputFileName, "Output file name cannot be null");
        /* For backwards compatibility */
        try {
            MutableGraph g = generateGraph();
            Graphviz graphviz = Graphviz.fromGraph(g);
            graphviz.render(Format.SVG_STANDALONE).toFile(
                    new File(outputFileName + FilenameUtils.EXTENSION_SEPARATOR + Format.SVG_STANDALONE.fileExtension));
            // graphviz.render(Format.PNG).toFile(new File(outputFileName +
            // FilenameUtils.EXTENSION_SEPARATOR + Format.PNG.fileExtension));
            return true;
        } catch (IOException e) {
            logger.catching(e);
            return false;
        }
    }

    /**
     * Exports this automaton in a Graphviz-exportable format
     * 
     * @param outputFileName name of the exported file
     * @param format         file format to export with
     * @return the exported file
     * @throws IllegalArgumentException if {@code outputFileName} has a file
     *                                  extension that is not consistent with
     *                                  {@code format}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IOException              If I/O error occurs
     * @since 1.1
     **/
    public final File export(String outputFileName, Format format) throws IOException {
        Objects.requireNonNull(format);

        File destFile;

        if (FilenameUtils.getExtension(Objects.requireNonNull(outputFileName)).isEmpty()) {
            destFile = new File(outputFileName + FilenameUtils.EXTENSION_SEPARATOR + format.fileExtension);
        } else if (Objects.equals(FilenameUtils.getExtension(outputFileName), format.fileExtension)) {
            destFile = new File(outputFileName);
        } else {
            throw new IllegalArgumentException(
                    String.format("\"%s\" does not have the expected extension %s", outputFileName,
                            format.fileExtension));
        }

        /* Generate image */

        MutableGraph g = generateGraph();
        Graphviz.fromGraph(g).render(format).toFile(destFile);

        return destFile;

    }

    /**
     * Exports this automaton to a file
     * 
     * @param file the destination file to export this automaton to
     * @return the exported file
     * 
     * @throws NullPointerException     if argument is {@code null}
     * @throws IllegalArgumentException if argument is using an unsupported
     *                                  file extension
     * @throws IOException              if an I/O error occurs
     * 
     * @since 2.0
     **/
    public final File export(File file) throws IOException {

        Format format = Format.valueOf(
                FilenameUtils.getExtension(
                        Objects.requireNonNull(file).getName()).toUpperCase());

        /* Generate image */

        MutableGraph g = generateGraph();
        Graphviz.fromGraph(g).render(format).toFile(file);

        return file;
    }

    /**
     * Generate a graph that represents this automaton
     * 
     * @return a Graphviz graph that represents this automaton
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public MutableGraph generateGraph() {
        MutableGraph g = mutGraph().setDirected(true);
        g.graphAttrs().add(
                Color.TRANSPARENT.background(),
                GraphAttr.splines(GraphAttr.SplineMode.POLYLINE),
                Attributes.attr("nodesep", 0.5),
                Rank.sep(2),
                Attributes.attr("overlap", "scale"));
        g = g.nodeAttrs().add(Shape.CIRCLE, Style.BOLD, Attributes.attr("constraint", false));

        /* Mark special transitions */

        Map<String, Attributes<? extends ForLink>> additionalEdgeProperties = new HashMap<String, Attributes<? extends ForLink>>();
        addAdditionalLinkProperties(additionalEdgeProperties);

        /* Draw all states and their transitions */

        for (State state : automaton.getStates()) {

            String stateLabel = formatStateLabel(state);
            MutableNode sourceNode = mutNode(Long.toString(state.getID()));
            addAdditionalNodeProperties(state, sourceNode);

            // Draw state
            g = g.add(sourceNode.add(Attributes.attr("peripheries", state.isMarked() ? 2 : 1), Label.of(stateLabel)));

            // Find and draw all of the special transitions
            List<Transition> transitionsToSkip = new ArrayList<Transition>();
            for (Transition t : state.getTransitions()) {

                State targetState = automaton.getState(t.getTargetStateID());

                // Check to see if this transition has additional properties (meaning it's a
                // special transition)
                String key = stateLabel + " " + t.getEvent().getID() + " " + formatStateLabel(targetState);
                Attributes<? extends ForLink> properties = additionalEdgeProperties.get(key);

                if (properties != null) {

                    transitionsToSkip.add(t);

                    MutableNode targetNode = mutNode(Long.toString(targetState.getID()));
                    targetNode.addTo(g);
                    if (!Objects.equals(properties.get("color"), "transparent")) {
                        Link l = sourceNode.linkTo(targetNode);
                        l.add(Label.of(t.getEvent().getLabel()));
                        l.add(properties);
                        sourceNode.links().add(l);
                    }
                }
            }

            // Draw all of the remaining (normal) transitions
            for (Transition t1 : state.getTransitions()) {

                // Skip it if this was already taken care of (grouped into another transition
                // going to the same target state)
                if (transitionsToSkip.contains(t1))
                    continue;

                // Start building the label
                String label = t1.getEvent().getLabel();
                transitionsToSkip.add(t1);

                // Look for all transitions that can be grouped with this one
                for (Transition t2 : state.getTransitions()) {

                    // Skip it if this was already taken care of (grouped into another transition
                    // going to
                    // the same target state)
                    if (transitionsToSkip.contains(t2))
                        continue;

                    // Check to see if both transitions lead to the same event
                    if (t1.getTargetStateID() == t2.getTargetStateID()) {
                        label += "," + t2.getEvent().getLabel();
                        transitionsToSkip.add(t2);
                    }

                }

                // Add transition
                MutableNode targetNode = mutNode(Long.toString(t1.getTargetStateID()));
                targetNode.addTo(g);
                Link l = sourceNode.linkTo(targetNode);
                l.add(Label.of(label));
                sourceNode.links().add(l);
            }

            if (automaton.getInitialStateID() > 0) {
                MutableNode startNode = mutNode(StringUtils.EMPTY).add(Shape.PLAIN_TEXT);
                MutableNode initNode = mutNode(Long.toString(automaton.getInitialStateID()));
                Link init = startNode.linkTo(initNode);
                init.add(Color.BLUE);
                startNode.links().add(init);
                startNode.addTo(g);
            }
        }

        return g;
    }

    /**
     * This helper method is used to get a state's label, breaking vectors into
     * multiple lines.
     * 
     * @param state The state in which the label is being taken from
     * @return The formatted state label
     */
    private String formatStateLabel(State state) {

        String label = state.getLabel();
        LabelVector labelVector = new LabelVector(label);
        int size = labelVector.getSize();

        if (size == -1)
            return label;

        StringBuilder stringBuilder = new StringBuilder();

        for (String indexedLabel : labelVector)
            stringBuilder.append(indexedLabel + "\\n");

        return stringBuilder.toString();

    }

    /**
     * Add any additional node properties applicable to this automaton type, which
     * is used in the graph generation.
     * 
     * @param state State in this automaton that corresponds to the node in the
     *              graph
     * @param node  Node in graph to add properties to
     */
    protected abstract void addAdditionalNodeProperties(State state, MutableNode node);

    /**
     * Add any additional edge properties applicable to this automaton type, which
     * is used in the graph generation.
     * <p>
     * EXAMPLE: This is used to color potential communications blue.
     * 
     * @param map The mapping from edges to additional properties
     **/
    protected abstract void addAdditionalLinkProperties(Map<String, Attributes<? extends ForLink>> map);

    /**
     * Helper method used to create a key for the additional edge properties map.
     * 
     * @param data The relevant transition data
     * @return A string used to identify this particular transition
     **/
    protected String createKey(TransitionData data) {
        return formatStateLabel(automaton.getState(data.initialStateID)) + StringUtils.SPACE
                + data.eventID + StringUtils.SPACE
                + formatStateLabel(automaton.getState(data.targetStateID));
    }

    /**
     * Helper method used to append a value to the pre-existing value of a
     * particular key in a map.
     * If the key was not previously in the map, then the value is simply added.
     * 
     * @param map   The relevant map
     * @param key   The key which is mapped to a value that is being appending to
     * @param value The attribute to be added
     */
    protected static void combineAttributesInMap(Map<String, Attributes<? extends ForLink>> map, String key,
            Attributes<? extends ForLink> value) {
        if (map.containsKey(key))
            map.put(key, Attributes.attrs(map.get(key), value));
        else
            map.put(key, value);
    }

}
