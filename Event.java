/**
 * Event - This simple class represents an event in an automaton. It supports both centralized and decentralized control, which
 *				 means that it can have observability and controllability properties for each controller. It also has support for
 *				 events that have labels formatted as vectors.
 *
 * NOTE: Current ID system allows for 255 unique events with 0 representing null, but we will make this flexible later to allow for more events.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *	-Private Instance Variables
 *	-Constructor
 *	-Accessor Methods
 *	-Overridden Methods
 **/

import java.util.*;

public class Event implements Comparable<Event> {
    
  	/** PRIVATE INSTANCE VARIABLES **/

  private String label;
  private int id;
  private boolean[] observable, controllable;

  /**
   * Events can sometimes be vectors (for example, automata created by synchonrized composition use them).
   * Example of syntax: "&lt;a_b_d>" actually represents an event vector: {"a", "b", "d"}.
   **/
  public LabelVector vector = null;

  	/** CONSTRUCTOR **/

  /**
   * Construct a new event with the specified properties.
   * @param label			The name of the state
   * @param observable	Whether or not the event can be observed
   * @param controllable	Whether or not the event can be controlled
   **/
	public Event(String label, int id, boolean[] observable, boolean[] controllable) {

		this.label = label;
		this.id = id;
		this.observable = observable;
		this.controllable = controllable;
		this.vector = new LabelVector(label);

	}

		/** ACCESSOR METHODS **/

	/**
	 * Get the label of the event
 	 * @return label
	 **/
	public String getLabel() {
		return label;
	}

	/**
	 * Get the ID number of the event
 	 * @return id
	 **/
	public int getID() {
		return id;
	}

	/**
	 * Get the observability property of the event for each controller.
 	 * @return whether or not the event is observable
	 **/
	public boolean[] isObservable() {
		return observable;
	}

	/**
	 * Get the controllability property of the event for each controller.
 	 * @return whether or not the event is controllable
	 **/
	public boolean[] isControllable() {
		return controllable;
	}

		/** OVERRIDDEN METHODS **/

	/**
	 * Returns a hash code for this event, based on the hash code of it's label (which is a string).
	 * @return a hash code value for this object
	 **/
	@Override public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Returns a value from the set {-1, 0, 1} to represent the comparison of two events (based on ID, which is an integer).
	 * @param other	The other event that we're comparing this one to
	 * @return -1 if this event comes before, 0 if the events are equal, 1 if this event comes after
	 **/
	@Override public int compareTo(Event other) {

		return (new Integer(id)).compareTo(other.getID());

	}

	/**
	 * Check for equality by comparing labels.
	 * @param obj - The event to compare this one to
	 * @return whether or not the events are equal
	 **/
	@Override public boolean equals(Object obj) {

		Event other = (Event) obj;

		return this.label.equals(other.label);

	}

	/**
	 * Turn this event into a more meaningful representation as a string.
	 * @return string representation
	 **/
	@Override public String toString() {

		return "("
			+ "\"" + label + "\",ID:"
			+ id + ","
			+ "Observable=" + Arrays.toString(observable) + ","
			+ "Controllable=" + Arrays.toString(controllable)
			+ ")";
	}

}