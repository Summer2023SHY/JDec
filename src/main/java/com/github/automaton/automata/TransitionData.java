package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Overridden Methods
 */

/**
 * Holds all 3 pieces of information needed to identify a transition.
 *
 * <p>NOTE: This class is different from the {@link Transition} class, since
 * this class does not need to be attached to a specific state in order to
 * fully represent a transition (the {@link Transition} class does not have a
 * reference to the initial state ID, and it contains a reference to the actual
 * {@link Event} object instead of only holding onto its ID).</p>
 *
 * @author Micah Stairs
 */
public class TransitionData {

    /* PUBLIC INSTANCE VARIABLES */

  /** The ID of the state that the transition starts at. */
  public long initialStateID;

  /** The ID of the event which causes the transition. */
  public int eventID;

  /** The ID of the state that the transition ends at. */
  public long targetStateID;

    /* CONSTRUCTOR */

  /**
   * Construct a TransitionData object using the IDs of the associated event and states.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   **/
  public TransitionData(long initialStateID, int eventID, long targetStateID) {
      this.initialStateID = initialStateID;
      this.eventID = eventID;
      this.targetStateID = targetStateID;
  }

  /**
   * Construct a {@code TransitionData} object using the specified event and states.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * 
   * @since 2.0
   **/
  public TransitionData(State initialState, Event event, State targetState) {
    this.initialStateID = initialState.getID();
    this.eventID = event.getID();
    this.targetStateID = targetState.getID();
  }

    /* METHOD */

  /**
   * Given the source automaton, provide even more information when represented as a string.
   * @param automaton The automaton where this transition data came from
   * @return          The string representation
   **/
  public String toString(Automaton automaton) {
    return String.format(
      "%s,%s,%s",
      automaton.getStateExcludingTransitions(initialStateID).getLabel(),
      automaton.getEvent(eventID).getLabel(),
      automaton.getStateExcludingTransitions(targetStateID).getLabel()
    );
  }

    /* OVERRIDDEN METHODS */

  /**
   * Indicates whether an object is "equal to" this transition data
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this transition data is the same as the argument
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (obj instanceof TransitionData) {
      TransitionData other = (TransitionData) obj;

      return initialStateID == other.initialStateID
        && eventID == other.eventID
        && targetStateID == other.targetStateID;
    }
    else return false;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Long.hashCode(initialStateID);
  }

  /**
   * Returns string representation of this transition data
   * @return string representation of this transition data
   */
  @Override
  public String toString() {
    return String.format("(%d,%d,%d)", initialStateID, eventID, targetStateID);
  }

}