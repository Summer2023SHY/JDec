import java.util.*;

public class Event implements Comparable<Event> {
    
    	/* Class constants */

    // 255 unique events with 0 representing null
	// NOTE: We will make this flexible later to allow for more events
    public static final int MAX_NUMBER_OF_EVENTS = 255;
	public static final int N_BYTES_OF_ID = 1; 

    	/* Private instance variables */

    private String label;
    private int id;
    private boolean[] observable, controllable;

    /**
     * 	Construct a new event with the specified properties.
     *	@param label - The name of the state
     *	@param observable - Whether or not the event can be observed
     *	@param controllable - Whether or not the event can be controlled
     **/
	public Event(String label, int id, boolean[] observable, boolean[] controllable) {
		this.label = label;
		this.id = id;
		this.observable = observable;
		this.controllable = controllable;
	}

	// This is necessary when every element of the set needs to be checked (and we can't rely on using .contains because of how it handles HashSets and TreeSets).
	// This method is only needed to be used in cases where events are coming from different automata (so the IDs are all out of whack)
	public static boolean isElementOfSet(Set<Event> set, Event event) {

		for (Event e : set)
			if (e.equals(event))
				return true;

		return false;

	}

		/** STANDARD ACCESSOR AND MUTATOR METHODS **/

	/**
	 *	Get the label of the event
 	 *	@return label
	 **/
	public String getLabel() {
		return label;
	}

	/**
	 *	Get the ID number of the event
 	 *	@return id
	 **/
	public int getID() {
		return id;
	}

	/**
	 *	Get the observability property of the event for each controller.
 	 *	@return whether or not the event is observable
	 **/
	public boolean[] isObservable() {
		return observable;
	}

	/**
	 *	Get the controllability property of the event for each controller.
 	 *	@return whether or not the event is controllable
	 **/
	public boolean[] isControllable() {
		return controllable;
	}

		/** OVERRIDDEN METHODS **/

	@Override public int hashCode() {
		return label.hashCode();
	}

	@Override public int compareTo(Event other) {

		// if (this.label.equals(other.label))
		// 	return 0;

		return (new Integer(id)).compareTo(other.getID());

		// int comparedValue = (new Integer(id)).compareTo(other.getID());

		// // This check is necessary in case events are coming from different automata (so they may have the same ID but have different labels)
		// if (comparedValue == 0)
		// 	return 1;

		// return comparedValue;
	}

	/**
	 * Check for equality by comparing labels.
	 * NOTE: This method is used to check to see if an event is unique and should be added to the event set.
	 * @param obj - The event to compare this one to
	 * @return whether or not the events are equal
	 **/
	@Override public boolean equals(Object obj) {

		Event other = (Event) obj;

		return this.label.equals(other.label);

	}

	@Override public String toString() {

		return "("
			+ label + ",ID:"
			+ id + ","
			+ "Observable=" + Arrays.toString(observable) + ","
			+ "Controllable=" + Arrays.toString(controllable)
			+ ")";
	}

}