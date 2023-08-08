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

import com.google.gson.annotations.SerializedName;

/**
 * Enumeration used to help indicate whether a controller is the sender,
 * one of the receivers, or neither.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
public enum CommunicationRole {

    /* ENUMERATION VALUES */

  /** This role is associated with a controller who is neither the sender nor the receiver. */
  @SerializedName("*")
  NONE((byte) 0, '*'),

  /** This role is associated with a controller who is the sender. */
  @SerializedName("S")
  SENDER((byte) 1, 'S'),

  /** This role is associated with a controller who is a receiver. */
  @SerializedName("R")
  RECEIVER((byte) 2, 'R');

    /* INSTANCE VARIABLES */

  private final byte numericValue;
  private final char character;

    /* CONSTRUCTOR */

  /**
   * Each role is associated with a numeric value (stored as a byte). This is used when reading from
   * and writing to the binary file.
   * @param numericValue  The value of the CommunicationRole
   * @param character     The character associated with the CommunicationRole
   **/
  CommunicationRole(byte numericValue, char character) {
    this.numericValue = numericValue;
    this.character = character;
  }

    /* ACCESSOR METHODS */

  /**
   * Get the numeric value associated with this enumeration value.
   * @return  The numeric value
   **/
  public byte getNumericValue() {
    return numericValue;
  }

  /**
   * Get the character associated with this enumeration value.
   * @return  The associated character
   **/
  public char getCharacter() {
    return character;
  }

  /**
   * Given a numeric value, get the associated communication role.
   * @param value The numeric value
   * @return      The communication role (or {@code null}, if it could not be found)
   * 
   * @deprecated Use {@link #valueOf(byte)} instead.
   **/
  @Deprecated(since = "2.0")
  public static CommunicationRole getRole(byte value) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getNumericValue() == value)
        return role;
    }

    return null;

  }

  /**
   * Returns the {@code CommunicationRole} with the specified numeric value.
   * @param value the numeric value
   * @return      the {@code CommunicationRole} with the specified numeric value
   * 
   * @throws IllegalArgumentException if there is no {@code CommunicationRole} with the specified numeric value
   * 
   * @since 2.0
   */
  public static CommunicationRole valueOf(byte value) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getNumericValue() == value)
        return role;
    }

    throw new IllegalArgumentException("No CommunicationRole with " + value + " as the numeric value.");

  }

  /**
   * Given a character, get the associated communication role.
   * @param ch The character
   * @return communication role (or {@code null}, if it could not be found)
   * 
   * @deprecated Use {@link #valueOf(char)} instead.
   **/
  @Deprecated(since = "2.0")
  public static CommunicationRole getRole(char ch) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getCharacter() == ch)
        return role;
    }

    return null;

  }

  /**
   * Returns the {@code CommunicationRole} with the specified char representation.
   * @param ch    the char representation of the {@code CommunicationRole} to be returned
   * @return      the {@code CommunicationRole} with the specified numeric value
   * 
   * @throws IllegalArgumentException if there is no {@code CommunicationRole} with the specified char representation
   * 
   * @since 2.0
   */
  public static CommunicationRole valueOf(char ch) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getCharacter() == ch)
        return role;
    }

    throw new IllegalArgumentException("No CommunicationRole with '" + ch + "' as the char representation.");

  }

} 
