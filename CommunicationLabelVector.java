/**
 * CommunicationLabelVector - This class, extending LabelVector, is able to represent both a vector label
 *                            and its associated communication roles (which implies that we are only using
 *                            this for event labels, not state labels).
 *
 * @author Micah Stairs
 * 
 * TABLE OF CONTENTS:
 *  -Public Instance Variable
 *  -Constructor
 *  -Overridden Method
 **/

import java.util.*;

class CommunicationLabelVector extends LabelVector {

    /** PUBLIC INSTANCE VARIABLE **/

  public CommunicationRole[] roles;

    /** CONSTRUCTOR **/

  /**
   * Construct a CommunicationLabelVector object, given it's label and each controller's communication roles.
   * @param label The unvectorized label
   * @param roles The array of communication roles
   **/
  public CommunicationLabelVector(String label, CommunicationRole[] roles) {

    super(label);

    this.roles = roles;

  }

    /** OVERRIDDEN METHOD **/

  /**
   * Check for equality by comparing labels and roles.
   * @param obj The communication label vector to compare this one to
   * @return whether or not the communication label vectors are equal
   **/
  @Override public boolean equals(Object other) {
    return super.equals(other) && Arrays.deepEquals(roles, ((CommunicationLabelVector) other).roles);
  }

}