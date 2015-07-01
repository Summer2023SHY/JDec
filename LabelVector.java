/**
 * LabelVector - This class is used to take a string and vectorize it into its components using some basic syntax.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *	-Private Instance Variables
 *	-Constructor
 *	-Accessor Methods
 *	-Overridden Methods
 **/

class LabelVector {

		/* PRIVATE INSTANCE VARIABLES */

	private String label; 
	private String[] vector = null;

		/** CONSTRUCTOR **/

	/**
	 * Construct a LabelVector object, which takes a string and splits it into its vector components.
	 * @param label	The label to be vectorized (syntax: "&lt;first,second,third>" vectorizes into {"first", "second", "third"})
	 **/
	public LabelVector(String label) {

		this.label = label;

		// Ensure that the label has proper vector syntax before vectorizing it
		if (label.charAt(0) == '<' && label.charAt(label.length() - 1) == '>')
			vector = label.substring(1, label.length() - 1).split(",");

	}

		/** ACCESSOR METHODS **/

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

		/** OVERRIDDEN METHODS **/

	/**
	 * Returns a hash code for this event vector, based on the hash code of it's label (which is a string).
	 * @return A hash code value for this object
	 **/
	@Override public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Check for equality by comparing labels.
	 * @param obj  The label vector to compare this one to
	 * @return     Whether or not the label vectors are equal
	 **/
	@Override public boolean equals(Object other) {
		return label.equals(((LabelVector) other).label);
	}

	/**
	 * Turn this label vector into a more meaningful representation as a string.
	 * @return The string representation
	 **/
	@Override public String toString() {
		return label;
	}

}