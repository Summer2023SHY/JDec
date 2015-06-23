public class NashCommunicationData extends CommunicationData {

  public int cost;
  public double probability;

    /** CONSTRUCTOR **/

  /**
   * Construct a NashCommunicationData object, which is used by the NashUStructure class.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param roles           The array of communication roles (sender, reciever, or none)
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication
   **/
  public NashCommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, int cost, double probability) {
    
    super(initialStateID, eventID, targetStateID, roles);

    this.cost        = cost;
    this.probability = probability;

  }

}