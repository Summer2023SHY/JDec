public class Event {
    
    	/* Class constants */

    public static final int MAX_NUMBER_OF_EVENTS = 255;
	public static final int N_BYTES_OF_ID = 1; // (So 255 unique events with 0 representing null, NOTE: we will make this flexible later to allow up to 2^63 unique events)
 
    	/* Private instance variables */

    private String label;
    private int id; // Used as int to prevent overflow, although we read and write as unsigned byte
    private boolean observable, controllable;

    /**
     * 	Construct a new event with the specified properties.
     *	@param label - The name of the state
     *	@param observable - Whether or not the event can be observed
     *	@param controllable - Whether or not the event can be controlled
     **/
	public Event(String label, int id, boolean observable, boolean controllable) {
		this.label = label;
		this.id = id;
		this.observable = observable;
		this.controllable = controllable;
	}

	public boolean hasSameID(Event other) {
		return this.id == other.id;
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
	 *	Get the observability property of the event
 	 *	@return whether or not the event is observable
	 **/
	public boolean isObservable() {
		return observable;
	}

	/**
	 *	Get the controllability property of the event
 	 *	@return whether or not the event is controllable
	 **/
	public boolean isControlable() {
		return controllable;
	}


		/** OVERRIDDEN METHODS **/  

	@Override public int hashCode() {
		return label.hashCode();
	}

	/**
	 *	Check for equality (but do not use the ID number)
	 *	NOTE: This method is used to check to see if an event is unique and should be added to the event set.
	 *	@param obj - The event to compare this one to
	 *	@return whether or not the events are equal
	 **/
	@Override public boolean equals(Object obj) {

		Event event = (Event) obj;

		return this.label.equals(event.label)
			&& this.observable == event.observable
			&& this.controllable == event.controllable;

	}

	@Override public String toString() {
		return "("
			+ label + ",ID:"
			+ id + ","
			+ (controllable ? "Controllable" : "Not Controllable") + ","
			+ (observable ? "Observable" : "Not Observable")
			+ ")";
	}

}