package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Mutator Method
 *  -Accessor Methods
 *  -Overridden Methods
 */

/**
 * Represents a transition in an automaton.
 *
 * @implNote An instance of this class should remain attached to a state in
 * order to be able to fully represent a transition (since a transition has
 * no reference to its initial state ID).
 *
 * @author Micah Stairs
 */
public class Transition {

    /* INSTANCE VARIABLES */
  
  private long targetStateID;
  private Event event;

    /* CONSTRUCTOR */

  /**
   * Constructs a Transition object.
   * @param event         The event triggering this transition
   * @param targetStateID The state that the transition leads to
   **/
  public Transition(Event event, long targetStateID) {
    this.event = event;
    this.targetStateID = targetStateID;

    if (targetStateID == 0) {
      System.out.println("WARNING: The target state ID was specified as 0 (which is null).");
      Thread.dumpStack();
    }
  }

    /* MUTATOR METHOD */

  /**
   * Set the state that this transition leads to.
   * @param id  The new ID of the target state
   **/
  public void setTargetStateID(long id) {
    targetStateID = id;
    if (targetStateID == 0) {
      System.out.println("WARNING: Setting target state ID to 0 (which is null).");
      Thread.dumpStack();
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