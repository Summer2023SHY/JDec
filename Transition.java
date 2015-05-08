public class Transition {

	// Private instance variables
    private long targetStateID;
    private Event event;

	public Transition(Event event, long targetStateID) {
		this.event = event;
		this.targetStateID = targetStateID;
	}

	public Event getEvent() {
		return event;
	}

	public long getTargetStateID() {
		return targetStateID;
	}

}