 /**
 * TransitionData - Holds all 3 pieces of information needed to identify a transition.
 *                  NOTE: This class is different from the Transition class, since this
 *                  class does not need to be attached to a specific state in order to
 *                  fully represent a transition (the Transition class does not have a
 *                  reference to the initial state ID, and it contains a reference to the
 *                  actually Event object instead of only holding onto its ID).
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Public Instance Variables
 *  -Constructor
 *  -Overridden Method
 **/

public class TransitionData {

    /** PUBLIC INSTANCE VARIABLES **/

  /** The ID of the state that the transition starts at. */
  public long initialStateID;

  /** The ID of the event which causes the transition. */
  public int eventID;

  /** The ID of the state that the transition ends at. */
  public long targetStateID;

    /** CONSTRUCTOR **/

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

    /** OVERRIDDEN METHOD **/

  /**
   * Check for equality by comparing properties.
   * @param obj The object to compare this one to
   * @return whether or not the transitions are equal
   **/
  @Override public boolean equals(Object obj) {

    TransitionData other = (TransitionData) obj;

    return initialStateID == other.initialStateID
      && eventID == other.eventID
      && targetStateID == other.targetStateID;

  }

}