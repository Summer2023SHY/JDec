public class Event {
    
    // Private instance variables
    private String label;
    private boolean observable, controllable;

    /**
     * 	Construct a new event with the specified properties.
     *	@param label - The name of the state
     *	@param observable - Whether or not the event can be observed
     *	@param controllable - Whether or not the event can be controlled
     **/
	public Event(String label, boolean observable, boolean controllable) {
		this.label = label;
		this.observable = observable;
		this.controllable = controllable;
	}

}