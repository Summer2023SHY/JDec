/**
 * CommunicationData - Holds all 3 pieces of information needed to identify a transition, as well as an enumeration array
 *                     to indicate which controller is the sender and which are the recievers.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Accessor Method
 *  -Overridden Methods
 **/

import java.util.*;

public class CommunicationData extends TransitionData {

    /* INSTANCE VARIABLES */

  /** Holds the role for each of the controllers (sender, reciever, or none) */
  public CommunicationRole[] roles;

  private int indexOfSender = -1;

    /* CONSTRUCTOR */

  /**
   * Construct a CommunicationData object, which can be used to represent a communication (including the sending and recieving roles).
   * @param initialStateID  The initial state's ID
   * @param eventID         The event's ID
   * @param targetStateID   The target state's ID
   * @param roles           The array of communication roles (sender, reciever, or none)
   **/
  public CommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles) {
    
    super(initialStateID, eventID, targetStateID);
    this.roles = roles;

      /* Store the index of the sender */

    int nSenders = 0;

    for (int i = 0; i < roles.length; i++)
      if (roles[i] == CommunicationRole.SENDER) {
        indexOfSender = i;
        nSenders++;
      }

      /* Print error message to the console if there is not exactly one sender */

    if (nSenders != 1)
      System.err.println("ERROR: A communication must contain exactly one sender. " + nSenders + " senders were found.");
      
  }

    /* ACCESSOR METHOD */

  /**
   * Return the index (0-based) of the sending controller.
   * NOTE: There can only be one sender in a CommunicationData object. In cases where more than
   *       one sender is required, they can be split into multiple communications.
   * @return  The index of the sender, or -1 if there is no sender (which is prohibited by the
   *          constructor anyway)
   **/
  public int getIndexOfSender() {
  
    return indexOfSender;
  
  }

    /* OVERRIDDEN METHODS */

  @Override public boolean equals(Object obj) {

    return super.equals(obj) && Arrays.deepEquals(roles, ((CommunicationData) obj).roles);

  }

  @Override public String toString(Automaton automaton) {

    String str = " (";
    for (CommunicationRole role : roles)
      str += role.getCharacter();

    return super.toString(automaton) + str + ")";

  }

}