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
 * Represents both a vector label and its associated communication roles (which
 * implies that we are only using this for event labels, not state labels).
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
class CommunicationLabelVector extends LabelVector {

    /* INSTANCE VARIABLE */

  public CommunicationRole[] roles;

    /* CONSTRUCTOR */

  /**
   * Private constructor for compatibility with gson
   * 
   * @since 2.0
   */
  private CommunicationLabelVector() {
    super(StringUtils.EMPTY);
    this.roles = (CommunicationRole[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  /**
   * Construct a CommunicationLabelVector object, given it's label and each controller's communication roles.
   * @param label The unvectorized label
   * @param roles The array of communication roles
   **/
  public CommunicationLabelVector(String label, CommunicationRole[] roles) {

    super(label);
    this.roles = roles;

  }

    /* OVERRIDDEN METHOD */

  /**
   * Check for equality by comparing labels and roles.
   * @param other The communication label vector to compare this one to
   * @return      Whether or not the communication label vectors are equal
   **/
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    else if (!super.equals(other)) return false;
    else if (other instanceof CommunicationLabelVector)
      return Arrays.deepEquals(roles, ((CommunicationLabelVector) other).roles);
    else return false;
  }

}
