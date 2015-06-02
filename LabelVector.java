class LabelVector {

	private String label; 
	private String[] vector = null;

	public LabelVector(String label) {

		this.label = label;

		// Ensure that the label is a vector
		if (label.charAt(0) == '<' && label.charAt(label.length() - 1) == '>')
			vector = label.substring(1, label.length() - 1).split("_");

	}

	/**
	 * Get a specific label from the vector.
	 * @param index	The index in the vector
	 * @return the label from the vector
	 **/
	public String getLabelAtIndex(int index) {

		if (vector == null)
			return null;
		else
			return vector[index];
		
	}

	/**
	 * Get the size of the vector.
	 * @return the label from the vector, or -1 if this event is not a vector
	 **/
	public int getSize() {

		if (vector == null)
			return -1;

		return vector.length;
		
	}

	/**
	 * Returns a hash code for this event vector, based on the hash code of it's label (which is a string).
	 * @return a hash code value for this object
	 **/
	@Override public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Check for equality by comparing labels.
	 * @param obj - The label vector to compare this one to
	 * @return whether or not the label vectors are equal
	 **/
	@Override public boolean equals(Object obj) {

		LabelVector other = (LabelVector) obj;

		return this.label.equals(other.label);

	}

	/**
	 * Turn this label vector into a more meaningful representation as a string.
	 * @return string representation
	 **/
	@Override public String toString() {

		return label;
		
	}
}