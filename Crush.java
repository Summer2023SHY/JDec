import java.io.*;
import java.util.*;

public class Crush extends PrunedUStructure {

  // private int indexOfCrushedController;

  /**
   * Implicit constructor: used to load pruned U-Structure from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   **/
  public Crush(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);
  }

  /**
   * Implicit constructor: used when creating a new pruned U-Structure structure.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   * @param nControllersBeforeUStructure  The number of controllers that were present before the U-Structure was created
   <!-- * @param indexOfCrushedController      The index of the controller in which the pruned U-Structure was crushed with respect to -->
   **/
  public Crush(File headerFile, File bodyFile, int nControllersBeforeUStructure /*, int indexOfCrushedController*/) {
    super(headerFile, bodyFile, nControllersBeforeUStructure);
    // this.indexOfCrushedController = indexOfCrushedController;
  } 

}