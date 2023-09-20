package com.github.automaton.automata;

/* 
 * Copyright (C) 2016 Micah Stairs
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

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Used to vectorize a communication protocol into multiple components, based on the
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
   * Construct a ProtocolVector object using the specified protocol and the number of controllers.
   * @param protocol      The protocol to be vectorized by index of the sending controller
   * @param nControllers  The number of controllers in the system
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

			communications[i] = list.toArray(new NashCommunicationData[list.size()]);

		}

	}

    /* ACCESSOR METHODS */

  /**
   * Retrieve the original protocol (before it was vectorized).
   * @return  The protocol
   **/
  public Set<NashCommunicationData> getOriginalProtocol() {
    return protocol;
  }

  /**
   * Retrieve the array of all communications associated with a specified sending controller.
   * @param index The index of the associated sending controller (0-based)
   * @return      The array of communications associated with the specified sender
   **/
	public NashCommunicationData[] getCommunications(int index) {
		return communications[index];
	}

  /**
   * Get the total value of all communications corresponding with the specified sender.
   * @param index The index of the associated sending controller (0-based)
   * @return      The total value, where value is the product of cost and probability
   **/
  public double getValue(int index) {
    return value[index];
  }

  /**
   * Get the total value of all communications in the protocol.
   * @return  The total value, where value is the product of cost and probability
   **/
  public double getValue() {
    return totalValue;
  }

	/**
   * Given the source automaton, represent this object as a string.
   * @param automaton The automaton where this protocol came from
   * @return          The string representation
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
