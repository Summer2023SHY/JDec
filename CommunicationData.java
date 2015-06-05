/**
 * Class to hold all 3 pieces of information needed to identify a transition, as well as
 * an enum array to indicate whether a given controller is a sender, reciever, or neither.
 **/

import java.util.*;

public class CommunicationData extends TransitionData {

  public CommunicationRole[] roles;

  public CommunicationData(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles) {
    super(initialStateID, eventID, targetStateID);
    this.roles = roles;
  }

  public int getIndexOfSender() {

    for (int i = 0; i < roles.length; i++)
      if (roles[i] == CommunicationRole.SENDER)
        return i;

    return -1;

  }

}