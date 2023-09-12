package com.github.automaton.automata;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * Used to take a string and vectorize it into its components using some
 * basic syntax.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class LabelVector implements Iterable<String> {

    /* INSTANCE VARIABLES */

  private String label; 
  private String[] vector = null;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private LabelVector() {
    this.label = StringUtils.EMPTY;
    this.vector = null;
  }

  /**
   * Construct a LabelVector object, which takes a string and splits it into its vector components.
   * @param label The label to be vectorized (syntax: "&lt;first,second,third>" vectorizes into {"first",
   * "second", "third"})
   * @throws NullPointerException if argument is {@code null}
   **/
  public LabelVector(String label) {

    this.label = Objects.requireNonNull(label);

    if (label.isEmpty()) {
      vector = null;
    }

    // Ensure that the label has proper vector syntax before vectorizing it
    else if (label.charAt(0) == '<' && label.charAt(label.length() - 1) == '>')
      vector = label.substring(1, label.length() - 1).split(",");

  }

  /**
   * Construct a {@code LabelVector} object from its vector components
   * 
   * @param labels components of this vector
   * @throws NullPointerException if argument is {@code null}
   * @throws IllegalArgumentException if any element of the argument is {@code null}
   * 
   * @since 1.3
   */
  public LabelVector(String[] labels) {
    Validate.noNullElements(labels);
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

  /**
   * Construct a {@code LabelVector} object from its vector components
   * 
   * @param labels components of this vector
   * @throws NullPointerException if argument is {@code null}
   * @throws IllegalArgumentException if any element of the argument is {@code null}
   * 
   * @since 1.3
   */
  public LabelVector(List<String> labels) {
    this(Objects.requireNonNull(labels).toArray(new String[0]));
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
  /**
   * Returns an iterator over the labels in this vector
   * 
   * @return an iterator
   * @throws UnsupportedOperationException if this label is not a vector
   * 
   * @since 1.3
   */
  @Override
  public Iterator<String> iterator() {
    if (Objects.isNull(vector)) {
      throw new UnsupportedOperationException("This label is not a vector");
    }
    return new Iterator<String>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index != vector.length;
      }

      @Override
      public String next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements to iterate over");
        }
        return vector[index++];
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  /**
   * Indicates whether an object is "equal to" this label vector
   * 
   * @param other the reference object with which to compare
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
