public class Transition {

	// Private instance variables
    private State initialState, targetState;
    private Event event;

	public Transition(State initialState, Event event, State targetState) {
		this.initialState  = initialState;
		this.event = event;
		this.targetState = targetState;
	}

}