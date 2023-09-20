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
import org.apache.logging.log4j.*;

/**
 * Holds all 3 pieces of information needed to identify a transition, as well
 * as an enumeration array to indicate which controller is the sender
 * and which are the receivers.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class CommunicationData extends TransitionData {

  private static Logger logger = LogManager.getLogger();

    /* INSTANCE VARIABLES */

  /** Holds the role for each of the controllers (sender, receivers, or none) */
  public CommunicationRole[] roles;

  private int indexOfSender = -1;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private CommunicationData() {
    super(0, -1, 0);
    this.roles = (CommunicationRole[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  /**
   * Construct a CommunicationData object, which can be used to represent a communication (including the sending and receiving roles).
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param roles           The array of communication roles (sender, receivers, or none)
   **/
  public CommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles) {
    
    super(initialStateID, eventID, targetStateID);
    this.roles = roles;

      /* Store the index of the sender */

    int nSenders = 0;

    for (int i = 0; i < roles.length; i++)
      if (roles[i] == CommunicationRole.SENDER) {
        indexOfSender = i;
        nSenders++;
      }

      /* Print error message to the console if there is not exactly one sender */

    if (nSenders != 1)
      logger.error("A communication must contain exactly one sender. " + nSenders + " senders were found.");
      
  }

    /* ACCESSOR METHOD */

  /**
   * Return the index (0-based) of the sending controller.
   * NOTE: There can only be one sender in a CommunicationData object. In cases where more than
   *       one sender is required, they can be split into multiple communications.
   * @return  The index of the sender, or -1 if there is no sender (which is prohibited by the
   *          constructor anyway)
   **/
  public int getIndexOfSender() {
  
    return indexOfSender;
  
  }

    /* OVERRIDDEN METHODS */

  /**
   * Creates and returns a copy of this {@code CommunicationData}.
   * 
   * @return a copy of this {@code CommunicationData}
   * 
   * @since 2.0
   */
  @Override
  public CommunicationData clone() {
    return new CommunicationData(initialStateID, eventID, targetStateID, roles.clone());
  }

  /**
   * Indicates whether an object is "equal to" this communication data
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this communication data is the same as the argument
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (!super.equals(obj)) return false;
    else if (obj instanceof CommunicationData) {
      return Arrays.deepEquals(roles, ((CommunicationData) obj).roles);
    }
    else return false;
  }

  /**
   * Returns a hash code for this {@code CommunicationData}.
   * 
   * @return a hash code value
   * 
   * @since 2.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.initialStateID, super.eventID, super.targetStateID, Arrays.hashCode(roles));
  }

  /** {@inheritDoc} */
  @Override
  public String toString(Automaton automaton) {

    StringBuilder str = new StringBuilder(" (");
    for (CommunicationRole role : roles)
      str.append(role.getCharacter());

    return super.toString(automaton) + str.toString() + ")";

  }

}
