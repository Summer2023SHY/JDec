/**
 * CommunicationRole - An enumeration used to help indicate whether a controller is the sender, one of the recievers, or neither.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Enumeration Values
 *  -Private Instance Variable
 *  -Constructor
 *  -Accessor Methods
 **/
public enum CommunicationRole {

    /** ENUMERATION VALUES **/

  /** This role is associated with a controller who is neither the sender nor the reciever. */
  NONE((byte) 0),

  /** This role is associated with a controller who is the sender. */
  SENDER((byte) 1),

  /** This role is associated with a controller who is a reciever. */
  RECIEVER((byte) 2);

    /** PRIVATE INSTANCE VARIABLE **/

  private final byte value;

    /** CONSTRUCTOR **/

  /**
   * Each role is associated with a numeric value (stored as a byte). This is used when reading from
   * and writing to the binary file.
   * @param value The value of the CommunicationRole
   **/
  CommunicationRole(byte value) {
    this.value = value;
  }

    /** ACCESSOR METHODS **/

  /**
   * Get the numeric value associated with this enumeration value.
   * @return numeric value
   **/
  public byte getValue() {
    return value;
  }

  /**
   * Given a numeric value, get the associated communication role.
   * @param value The numeric value
   * @return communication role (or null, if it could not be found)
   **/
  public static CommunicationRole getRole(byte value) {

    for (CommunicationRole role : CommunicationRole.values()) {
      if (role.getValue() == value)
        return role;
    }

    return null;

  }

} 