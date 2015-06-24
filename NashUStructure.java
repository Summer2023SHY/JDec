import java.io.*;
import java.util.*;

public class NashUStructure extends UStructure {

  private List<NashCommunicationData> nashCommunications;
  
  /**
   * Implicit constructor: used to load automaton from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   **/
  public NashUStructure(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);
  }

  /**
   * Implicit constructor: used when creating a new Nash U-Structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   **/
  public NashUStructure(File headerFile, File bodyFile, int nControllersBeforeUStructure) {
    super(headerFile, bodyFile, nControllersBeforeUStructure);
  }

  /**
   * Add a nash communication.
   * @param initialStateID  The initial state
   * @param eventID         The event triggering the transition
   * @param targetStateID   The target state
   * @param roles           The communication roles associated with each controller
   * @param cost            The cost of this communication
   * @param probability     The probability of choosing this communication (a value between 0 and 1, inclusive)
   **/
  public void addNashCommunication(long initialStateID, int eventID, long targetStateID, CommunicationRole[] roles, int cost, double probability) {

    if (nashCommunications == null)
      nashCommunications = new ArrayList<NashCommunicationData>();

    nashCommunications.add(new NashCommunicationData(initialStateID, eventID, targetStateID, roles, cost, probability));

    // Update header file
    headerFileNeedsToBeWritten = true;

  }

}