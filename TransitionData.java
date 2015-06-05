/**
* Holds all 3 pieces of information needed to identify a transition.
**/
public class TransitionData {

  public long initialStateID, targetStateID;
  public int eventID;

  public TransitionData(long initialStateID, int eventID, long targetStateID) {
      this.initialStateID = initialStateID;
      this.eventID = eventID;
      this.targetStateID = targetStateID;
  }

  /**
   * Check for equality by comparing properties.
   * @param obj - The object to compare this one to
   * @return whether or not the transitions are equal
   **/
  @Override public boolean equals(Object obj) {

    TransitionData other = (TransitionData) obj;

    return initialStateID == other.initialStateID
      && eventID == other.eventID
      && targetStateID == other.targetStateID;

  }

}