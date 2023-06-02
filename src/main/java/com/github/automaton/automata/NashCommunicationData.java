package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Method
 *  -Overridden Method
 */

import org.apache.commons.lang3.ArrayUtils;

/**
 * Holds all 3 pieces of information needed to identify a transition, as well
 * as an enumeration array to indicate which controller is the sender
 * and which are the receivers and the additional information of both cost
 * and probability values. This information is particularly useful when finding
 * Nash equilibria.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public class NashCommunicationData extends CommunicationData implements Cloneable {

    /* INSTANCE VARIABLES */

  public double cost;
  public double probability;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private NashCommunicationData() {
    super(0, -1, 0, (CommunicationRole[]) ArrayUtils.EMPTY_OBJECT_ARRAY);
    this.cost = Double.NaN;
    this.probability = Double.NaN;
  }

  /**
   * Construct a NashCommunicationData object, which is used by the NashUStructure class.
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param roles           The array of communication roles (sender, receiver, or none)
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public NashCommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, double cost, double probability) {
    
    super(initialStateID, eventID, targetStateID, roles);

    // Ensure that the cost is a non-negative value
    if (cost < 0.0)
      this.cost = 0.0;
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

    /* METHOD */

  /**
   * Represent this piece of Nash communication data in the form of a string.
   * @param automaton The relevant automaton
   * @return          The string representation
   **/
  public String toNashString(Automaton automaton) {
    return super.toString(automaton) + "," + cost + "," + probability;
  }

    /* OVERRIDDEN METHOD */
  /** {@inheritDoc} */
  @Override
  public Object clone() {
    return new NashCommunicationData(initialStateID, eventID, targetStateID, ArrayUtils.clone(roles), cost, probability);
  }

}