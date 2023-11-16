package com.github.automaton.automata;

import java.util.Arrays;
import java.util.Objects;

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

import org.apache.commons.lang3.ArrayUtils;

/**
 * Holds all 3 pieces of information needed to identify a transition, as well
 * as an enumeration array to indicate which controller is the sender
 * and which are the receivers and the additional information of both cost
 * and probability values. This information is particularly useful when finding
 * Nash equilibria.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class NashCommunicationData extends CommunicationData {

    /* INSTANCE VARIABLES */

  public double cost;
  public double probability;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private NashCommunicationData() {
    super(0, -1, 0, (CommunicationRole[]) ArrayUtils.EMPTY_OBJECT_ARRAY);
    this.cost = Double.NaN;
    this.probability = Double.NaN;
  }

  /**
   * Construct a NashCommunicationData object, which is used by the NashUStructure class.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param roles           The array of communication roles (sender, receiver, or none)
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public NashCommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, double cost, double probability) {
    
    super(initialStateID, eventID, targetStateID, roles);

    // Ensure that the cost is a non-negative value
    if (cost < 0d)
      this.cost = 0d;
    else
      this.cost = cost;

    // Ensure that probability is a value found in the range [0,1]
    if (probability < 0.0)
      this.probability = 0.0;
    else if (probability > 1.0)
      this.probability = 1.0;
    else
      this.probability = probability;

  }

    /* METHOD */

  /**
   * Represent this piece of Nash communication data in the form of a string.
   * @param automaton The relevant automaton
   * @return          The string representation
   **/
  public String toNashString(Automaton automaton) {
    return super.toString(automaton) + "," + cost + "," + probability;
  }

    /* OVERRIDDEN METHOD */

  /**
   * Creates and returns a copy of this {@code NashCommunicationData}.
   * 
   * @return a copy of this {@code NashCommunicationData}
   * 
   * @since 2.0
   */
  @Override
  public NashCommunicationData clone() {
    return new NashCommunicationData(initialStateID, eventID, targetStateID, ArrayUtils.clone(roles), cost, probability);
  }

  /**
   * Indicates whether an object is "equal to" this Nash communication data
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this Nash communication data is the same as the argument
   * 
   * @since 2.0
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (!super.equals(obj)) return false;
    else if (obj instanceof NashCommunicationData nash) {
      return this.probability == nash.probability && this.cost == nash.cost;
    }
    else return false;
  }

  /**
   * Returns a hash code for this {@code NashCommunicationData}.
   * 
   * @return a hash code value
   * 
   * @since 2.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.initialStateID, super.eventID, super.targetStateID, Arrays.hashCode(roles), cost, probability);
  }



}
