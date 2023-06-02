package com.github.automaton.automata;
/*
 * TABLE OF CONTENTS:
 *  -Enumeration Values
 *  -Instance Variables
 *  -Constructor
 *  -Accessor Methods
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
   * @return      The communication role (or null, if it could not be found)
   **/
  public static CommunicationRole getRole(byte value) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getNumericValue() == value)
        return role;
    }

    return null;

  }

  /**
   * Given a character, get the associated communication role.
   * @param ch The character
   * @return communication role (or null, if it could not be found)
   **/
  public static CommunicationRole getRole(char ch) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getCharacter() == ch)
        return role;
    }

    return null;

  }

} 