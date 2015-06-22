import java.io.*;

public class NashUStructure extends UStructure {
  
  /**
   * Implicit constructor: used to load automaton from file.
   * @param headerFile                    The file where the header should be stored
   * @param bodyFile                      The file where the body should be stored
   **/
  public NashUStructure(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);

    automatonType = 2;
    headerFileNeedsToBeWritten = true;

  }

}