/**
 * CommunicationData - Holds all 3 pieces of information needed to identify a transition, as well as an enumeration array
 *                     to indicate which controller is the sender and which are the recievers.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Public Instance Variable
 *  -Constructor
 *  -Accessor Method
 *  -Overridden Method
 **/

import java.util.*;

public class CommunicationData extends TransitionData {

    /** PUBLIC INSTANCE VARIABLE **/

  /** Holds the role for each of the controllers (sender, reciever, or none) */
  public CommunicationRole[] roles;

    /** CONSTRUCTOR **/

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

      /* Print error message to the console if there is not exactly one sender */

    int nSenders = 0;

    for (CommunicationRole role : roles)
      if (role == CommunicationRole.SENDER)
        nSenders++;

    if (nSenders != 1) {
      Thread.dumpStack();
      System.err.println("ERROR: A communication must contain exactly one sender. " + nSenders + " senders were found.");
    }

  }

    /** ACCESSOR METHOD **/

  /**
   * Return the index of the sending controller.
   * NOTE:  There is only ever one sender. In cases where more than one sender
   *        is required, they can be split into multiple communications.
   * @return the index of the sender, or -1 if there is no sender
   **/
  public int getIndexOfSender() {

    for (int i = 0; i < roles.length; i++)
      if (roles[i] == CommunicationRole.SENDER)
        return i;

    return -1;

  }

    /** OVERRIDDEN METHOD **/

  /**
   * Given the source automaton, provide even more information when represented as a string.
   * @param automaton The automaton where this communication data came from
   * @return string representation
   **/
  @Override public String toString(Automaton automaton) {

    String str = " (";
    for (CommunicationRole role : roles)
      str += role.getCharacter();

    return super.toString(automaton) + str + ")";

  }

}