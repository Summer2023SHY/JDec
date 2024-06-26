/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Used to vectorize a communication protocol into multiple components, based on
 * the
 * index of the sending controller.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class ProtocolVector {

    /* INSTANCE VARIABLES */

    private NashCommunicationData[][] communications;
    private double[] value;
    private double totalValue;
    private Set<NashCommunicationData> protocol;

    /* CONSTRUCTOR */

    /**
     * Private constructor for compatibility with gson
     * 
     * @since 2.0
     */
    private ProtocolVector() {
        this.protocol = Collections.emptySet();
        this.value = ArrayUtils.EMPTY_DOUBLE_ARRAY;
        this.totalValue = Double.NaN;
        this.communications = (NashCommunicationData[][]) ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    /**
     * Construct a ProtocolVector object using the specified protocol and the number
     * of controllers.
     * 
     * @param protocol     The protocol to be vectorized by index of the sending
     *                     controller
     * @param nControllers The number of controllers in the system
     **/
    public ProtocolVector(Set<NashCommunicationData> protocol, int nControllers) {

        this.protocol = protocol;

        // Initialize arrays
        communications = new NashCommunicationData[nControllers][];
        value = new double[nControllers];

        // Vectorize the protocol by the index of the sending communications
        for (int i = 0; i < nControllers; i++) {

            List<NashCommunicationData> list = new ArrayList<NashCommunicationData>();

            for (NashCommunicationData data : protocol)
                if (data.getIndexOfSender() == i) {
                    list.add(data);
                    value[i] += (data.probability * (double) data.cost);
                }

            totalValue += value[i];

            communications[i] = list.toArray(NashCommunicationData[]::new);

        }

    }

    /* ACCESSOR METHODS */

    /**
     * Retrieve the original protocol (before it was vectorized).
     * 
     * @return The protocol
     **/
    public Set<NashCommunicationData> getOriginalProtocol() {
        return protocol;
    }

    /**
     * Retrieve the array of all communications associated with a specified sending
     * controller.
     * 
     * @param index The index of the associated sending controller (0-based)
     * @return The array of communications associated with the specified sender
     **/
    public NashCommunicationData[] getCommunications(int index) {
        return communications[index];
    }

    /**
     * Get the total value of all communications corresponding with the specified
     * sender.
     * 
     * @param index The index of the associated sending controller (0-based)
     * @return The total value, where value is the product of cost and probability
     **/
    public double getValue(int index) {
        return value[index];
    }

    /**
     * Get the total value of all communications in the protocol.
     * 
     * @return The total value, where value is the product of cost and probability
     **/
    public double getValue() {
        return totalValue;
    }

    /**
     * Given the source automaton, represent this object as a string.
     * 
     * @param automaton The automaton where this protocol came from
     * @return The string representation
     **/
    public String toString(Automaton automaton) {

        StringBuilder stringBuilder = new StringBuilder();

        for (NashCommunicationData[] element : communications) {
            StringBuilder str = new StringBuilder();
            for (NashCommunicationData data : element)
                str.append("," + data.toString(automaton));

            if (str.length() > 0)
                stringBuilder.append(",[" + str.substring(1) + "]");
            else
                stringBuilder.append(",[]");

        }

        if (stringBuilder.length() > 0)
            return "(" + stringBuilder.toString().substring(1) + ")";
        else
            return "()";
    }

}
