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

import static org.apache.commons.lang3.exception.ExceptionUtils.*;

import java.util.Objects;

import org.apache.logging.log4j.*;

/**
 * Represents a transition in an automaton.
 *
 * @implNote An instance of this class should remain attached to a state in
 * order to be able to fully represent a transition (since a transition has
 * no reference to its initial state ID).
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class Transition implements Cloneable {

  private static Logger logger = LogManager.getLogger();

    /* INSTANCE VARIABLES */
  
  private long targetStateID;
  private Event event;

    /* CONSTRUCTOR */

  private Transition() {
    this.event = null;
    targetStateID = 0;
  }

  /**
   * Constructs a Transition object.
   * @param event         The event triggering this transition
   * @param targetStateID The state that the transition leads to
   **/
  public Transition(Event event, long targetStateID) {
    this.event = event;
    setTargetStateID(targetStateID);
  }

    /* MUTATOR METHOD */

  /**
   * Set the state that this transition leads to.
   * @param id  The new ID of the target state
   **/
  public void setTargetStateID(long id) {
    targetStateID = id;
    if (targetStateID == 0) {
      logger.warn("Setting target state ID to 0 (which is null).");
      logger.warn(getStackTrace(new Exception()));
    }
  }

  /**
   * Change the event which triggers this transition.
   * @param event The new event
   **/
  public void setEvent(Event event) {
    this.event = event;
  }

    /* ACCESSOR METHODS */

  /**
   * Returns the event which triggers this transition.
   * @return The event
   **/
  public Event getEvent() {
    return event;
  }

  /**
   * Returns the ID of the state that this transition leads to.
   * @return The ID of the target state
   **/
  public long getTargetStateID() {
    return targetStateID;
  }

    /* OVERRIDDEN METHODS */

  /**
   * Creates a shallow copy of this {@code Transition} instance.
   * (The triggering event itself is not cloned.)
   * 
   * @return a shallow copy of this {@code Transition}
   * 
   * @since 2.0
   */
  @Override
  public Object clone() {
      return new Transition(event, targetStateID);
  }

  /**
   * Indicates whether an object is "equal to" this transition
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this transition is the same as the argument
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (obj instanceof Transition) {
      Transition other = (Transition) obj;
      return targetStateID == other.targetStateID && event.equals(other.event);
    }
    else return false;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
      return Objects.hash(event, targetStateID);
  }

  /**
   * Returns string representation of this transition
   * @return string representation of this transition
   */
  @Override
  public String toString() {
    return "("
      + event + ","
      + targetStateID
      + ")";
  }

}
