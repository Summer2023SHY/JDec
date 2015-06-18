/**
 * Transition - Represents a transition in an automaton.
 *							NOTE: An instance of this class should remain attached to a state in order to be
 *							able to fully represent a transition (since a transition has no reference to its
 * 							initial state ID).
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *	-Private Instance Variables
 *	-Constructor
 *	-Mutator Method
 *	-Accessor Methods
 *	-Overridden Methods
 **/

public class Transition {

		/* PRIVATE INSTANCE VARIABLES */
  
  private long targetStateID;
  private Event event;

  	/** CONSTRUCTOR **/

  /**
   * Constructs a Transition object.
   * @param event					The event triggering this transition
   * @param targetStateID	The state that the transition leads to
   **/
	public Transition(Event event, long targetStateID) {
		this.event = event;
		this.targetStateID = targetStateID;
	}

		/** MUTATOR METHOD **/

	/**
	 * Set the state that this transition leads to.
	 * @param id	The new ID of the target state
	 **/
	public void setTargetStateID(long id) {
		targetStateID = id;
	}

  /**
   * Change the event which triggers this transition.
   * @param event The new event
   **/
  public void setEvent(Event event) {
    this.event = event;
  }


		/** ACCESSOR METHODS **/

	/**
	 * Returns the event which triggers this transition.
	 * @return event
	 **/
	public Event getEvent() {
		return event;
	}

	/**
	 * Returns the ID of the state that this transition leads to.
	 * @return ID of the target state
	 **/
	public long getTargetStateID() {
		return targetStateID;
	}

		/** OVERRIDDEN METHODS **/

	/**
	 * Check for equality by comparing events and target states.
	 * @param obj - The transition to compare this one to
	 * @return whether or not the transitions are equal
	 **/
	@Override public boolean equals(Object obj) {

		Transition other = (Transition) obj;

		return targetStateID == other.targetStateID && event.equals(other.event);

	}

	/**
	 * Turn this event into a more meaningful representation as a string.
	 * @return string representation
	 **/
	@Override public String toString() {
		return "("
			+ event + ","
			+ targetStateID
			+ ")";
	}

}