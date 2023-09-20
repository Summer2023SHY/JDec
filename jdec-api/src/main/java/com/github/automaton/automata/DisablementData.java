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

/**
 * Holds all 3 pieces of information needed to identify a transition, as well
 * as information on which controllers are able to disable this transition.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class DisablementData extends TransitionData {

    /* INSTANCE VARIABLE */

  /** Whether or not a particular controller (0-based) is able to disable this transition */
  public boolean[] controllers;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private DisablementData() {
    this(0, -1, 0, ArrayUtils.EMPTY_BOOLEAN_ARRAY);
  }

  /**
   * Construct a DisablementData object, which can be used to keep track of which controllers were able to
   * disable a particular transition.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param controllers     An array indicating which controllers (0-based) are able to disable this transition
   **/
  public DisablementData(long initialStateID, int eventID, long targetStateID, boolean[] controllers) {
    
    super(initialStateID, eventID, targetStateID);
    this.controllers = controllers;

  }

    /* OVERRIDDEN METHODS */

  /**
   * Creates and returns a copy of this {@code DisablementData}.
   * 
   * @return a copy of this {@code DisablementData}
   * 
   * @since 2.0
   */
  @Override
  public DisablementData clone() {
    return new DisablementData(super.initialStateID, super.eventID, super.targetStateID, controllers.clone());
  }

  /**
   * Indicates whether an object is "equal to" this disablement data
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this disablement data is the same as the argument
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (!super.equals(obj)) {
      return false;
    }
    else if (obj instanceof DisablementData)
      return Arrays.equals(controllers, ((DisablementData) obj).controllers);
    else return false;
  }

  /**
   * Returns a hash code for this {@code DisablementData}.
   * 
   * @return a hash code value
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.initialStateID, super.eventID, super.targetStateID, Arrays.hashCode(controllers));
  }

}
