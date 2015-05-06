public class State {
    
	// Private instance variables
	private String label;
	private long id;
	private boolean marked;
	private ArrayList<Transition> transitions;

	public State(String label, long id, boolean marked, ArrayList<Transition> transitions) {
		this.label = label;
		this.id = id;
		this.marked = marked;
		this.transitions = transitions;
	}

		/** ACCESSOR AND MUTATOR METHODS **/

}