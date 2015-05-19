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

	public void setTargetStateID(long id) {
		targetStateID = id;
	}

	@Override public String toString() {
		return "("
			+ event + ","
			+ targetStateID
			+ ")";
	}

}