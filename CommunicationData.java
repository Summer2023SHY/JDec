/**
 * CommunicationData -  Holds all 3 pieces of information needed to identify a transition, as well as an enumeration array
 *                      to indicate which controller is the sender and which are the recievers.
 **/

import java.util.*;

public class CommunicationData extends TransitionData {

  /** Holds the role for each of the controllers (sender, reciever, or none) */
  public CommunicationRole[] roles;

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
  }

  /**
   * Return the index of the sending controller.
   * NOTE:  There is only ever one sender. In cases where more than one sender
   *        is required, they can be split into multiple communications.
   * @return the index of the sender
   **/
  public int getIndexOfSender() {

    for (int i = 0; i < roles.length; i++)
      if (roles[i] == CommunicationRole.SENDER)
        return i;

    return -1;

  }

}