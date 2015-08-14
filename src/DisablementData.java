/**
 * DisablementData - Holds all 3 pieces of information needed to identify a transition, as well as information
 *                   on which controllers are able to disable this transition.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Constructor
 *  -Overridden Method
 **/

import java.util.*;

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

  @Override public boolean equals(Object obj) {

    return super.equals(obj) && Arrays.equals(controllers, ((DisablementData) obj).controllers);

  }

  @Override public int hashCode() {
    return ((Long) initialStateID).hashCode();
  }

}