package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Accessor Methods
 *  -Overridden Methods
 */

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * Used to take a string and vectorize it into its components using some
 * basic syntax.
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

  /**
   * Construct a {@code LabelVector} object from its vector components
   * 
   * @param labels components of this vector
   * @throws IllegalArgumentException if argument or any element of it is {@code null}
   * 
   * @since 2.0
   */
  public LabelVector(String[] labels) {
    if (ObjectUtils.anyNull((Object[]) labels)) {
      throw new IllegalArgumentException();
    }
    this.vector = ArrayUtils.clone(labels);
    StringBuilder labelBuilder = new StringBuilder();
    labelBuilder.append('<');
    for (String l : vector) {
      labelBuilder.append(l);
      labelBuilder.append(',');
    }
    labelBuilder.deleteCharAt(labelBuilder.length() - 1);
    labelBuilder.append('>');
    this.label = labelBuilder.toString();
  }

    /* ACCESSOR METHODS */

  /**
   * Check to see if this label vector is unobservable to the specified controller.
   * @param index The index of the controller (1-based)
   * @return      Whether or not the label vector is unobservable to the specified controller.
   **/
  public boolean isUnobservableToController(int index) {
    return Objects.equals(getLabelAtIndex(0), "*") || Objects.equals(getLabelAtIndex(index), "*");
  }

  /**
   * Get a specific label from the vector.
   * @param index  The index in the vector
   * @return       The label from the vector, or {@code null} if this label is not a vector
   * @throws IndexOutOfBoundsException if argument is out of bounds
   **/
  public String getLabelAtIndex(int index) {
    if (Objects.isNull(vector)) return null;
    return vector[Objects.checkIndex(index, getSize())];
  }

  /**
   * Get the size of the vector.
   * @return The size of this vector, or {@code -1} if this label is not a vector
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