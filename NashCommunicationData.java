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
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public NashCommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, int cost, double probability) {
    
    super(initialStateID, eventID, targetStateID, roles);

    // Ensure that the cost is a non-negative value
    if (cost < 0)
      this.cost = 0;
    else
      this.cost = cost;

    // Ensure that probability is a value found in the range [0,1]
    if (probability < 0.0)
      this.probability = 0.0;
    else if (probability > 1.0)
      this.probability = 1.0;
    else
      this.probability = probability;

  }

  /**
   * Represent this piece of Nash communication data in the form of a string.
   * @param automaton The relevant automaton
   * @return          The string representation
   **/
  public String toNashString(Automaton automaton) {
    return super.toString(automaton) + "," + cost + "," + probability;
  }

  @Override public Object clone() {
    return new NashCommunicationData(initialStateID, eventID, targetStateID, (CommunicationRole[]) roles.clone(), cost, probability);
  }

}