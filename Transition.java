public class Transition {

	// Private instance variables
    private State targetState;
    private Event event;

	public Transition(Event event, State targetState) {
		this.event = event;
		this.targetState = targetState;
	}

	public Event getEvent() {
		return event;
	}

	public State getTargetState() {
		return targetState;
	}

}