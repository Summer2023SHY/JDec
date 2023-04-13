package automata;

/*
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overridden Method
 **/

import java.util.*;

/**
 * Holds all 3 pieces of information needed to identify a transition, as well
 * as information on which controllers are able to disable this transition.
 *
 * @author Micah Stairs
 */
public class DisablementData extends TransitionData {

    /* INSTANCE VARIABLE */

  /** Whether or not a particular controller (0-based) is able to disable this transition */
  public boolean[] controllers;

    /* CONSTRUCTOR */

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

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Long.hashCode(initialStateID);
  }

}