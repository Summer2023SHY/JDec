package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Accessor Methods
 *  -Overridden Methods
 */

/**
 * LabelVector - This class is used to take a string and vectorize it into its components using some
 *               basic syntax.
 *
 * @author Micah Stairs
 */
public class LabelVector {

    /* INSTANCE VARIABLES */

  private String label; 
  private String[] vector = null;

    /* CONSTRUCTOR */

  /**
   * Construct a LabelVector object, which takes a string and splits it into its vector components.
   * @param label The label to be vectorized (syntax: "&lt;first,second,third>" vectorizes into {"first",
   * "second", "third"})
   **/
  public LabelVector(String label) {

    this.label = label;

    // Ensure that the label has proper vector syntax before vectorizing it
    if (label.charAt(0) == '<' && label.charAt(label.length() - 1) == '>')
      vector = label.substring(1, label.length() - 1).split(",");

  }

    /* ACCESSOR METHODS */

  /**
   * Check to see if this label vector is unobservable to the specified controller.
   * @param index The index of the controller (1-based)
   * @return      Whether or not the label vector is unobservable to the specified controller.
   **/
  public boolean isUnobservableToController(int index) {
    return getLabelAtIndex(0).equals("*") || getLabelAtIndex(index).equals("*");
  }

  /**
   * Get a specific label from the vector.
   * @param index  The index in the vector
   * @return       The label from the vector, or null if this label is not a vector
   **/
  public String getLabelAtIndex(int index) {

    if (vector == null)
      return null;
    else
      return vector[index];
    
  }

  /**
   * Get the size of the vector.
   * @return The label from the vector, or -1 if this label is not a vector
   **/
  public int getSize() {

    if (vector == null)
      return -1;

    return vector.length;
    
  }

    /* OVERRIDDEN METHODS */

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  /**
   * Indicates whether an object is "equal to" this label vector
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this label vector is the same as the argument
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    else if (other instanceof LabelVector)
      return label.equals(((LabelVector) other).label);
    else return false;
  }

  /**
   * Returns string representation of this label vector
   * @return string representation of this label vector
   */
  @Override
  public String toString() {
    return label;
  }

}