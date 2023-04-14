package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Enum
 *  -Constructors
 *  -Automata Operations
 */

import java.io.*;

/**
 * Crush - Extending PrunedUStructure, this class represents a Crush structure.
 *
 * @author Micah Stairs
 */
public class Crush extends PrunedUStructure {

    /* ENUM */

  /** 
   * Enum constant that represents the way that communication costs
   * can be combined when creating the {@link Crush}.
   * 
   * @author Micah Stairs
   */
  public static enum CombiningCosts {

    /** This is used by the Nash algorithm, and it means that the Crush doesn't actually need to be generated since the costs are not being combined in any way. */
    UNIT,

    /** 
     * The maximum cost of the communications which are being crushed together becomes the cost of the combined communication.
     * NOTE: The costs are capped at Integer.MAX_VALUE.
     **/
    MAX,

    /** The sum of the costs of the communications being combined are used as the cost of the combined communication. */
    SUM,

    /** The average of the costs of the communications being combined are used as the cost of the combined communication. */
    AVERAGE;

  }

    /* CONSTRUCTORS */

  /**
   * Implicit constructor: used to load Crush from file.
   * @param headerFile  The file where the header should be stored
   * @param bodyFile    The file where the body should be stored
   **/
  public Crush(File headerFile, File bodyFile) {
    super(headerFile, bodyFile);
  }

  /**
   * Implicit constructor: used when creating a new Crush.
   * @param headerFile    The file where the header should be stored
   * @param bodyFile      The file where the body should be stored
   * @param nControllers  The number of controllers that were present before the U-Structure was created
   **/
  public Crush(File headerFile, File bodyFile, int nControllersBeforeUStructure /*, int indexOfCrushedController*/) {
    super(headerFile, bodyFile, nControllersBeforeUStructure);
    // this.indexOfCrushedController = indexOfCrushedController;
  }

    /* AUTOMATA OPERATIONS */

  @Override public Crush accessible(File newHeaderFile, File newBodyFile) {
    return accessibleHelper(new Crush(newHeaderFile, newBodyFile, nControllers));
  }

}