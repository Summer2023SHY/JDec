import java.util.*;

public class Automaton {

	// Private instance variables
	private ArrayList<String> 	events,
								activeEvents,
								controllableEvents,
								observableEvents;
	private long numberOfStates;
	private File file = null;

		/** CONSTRUCTORS **/

    /**
     * Default constructor: create empty automaton
     **/
    public Automaton() {
    	events = new ArrayList<String>();
    	activeEvents = new ArrayList<String>();
    	controllableEvents = new ArrayList<String>();
    	observableEvents = new ArrayList<String>();
    	states = new ArrayList<State>();
    }

    /**
     *	Convenience constructor: create automaton from a binary file
	 *	@param file - The binary file to load the automaton from
     **/
	public Automaton(File file)
		this();
		this.file = file;
    }
    	/** AUTOMATA OPERATIONS **/

    static Automaton intersection(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    static Automaton union(Automaton first, Automaton second) {


    	return new Automaton(); // temporary
    }

    	/** ACCESSOR AND MUTATOR METHODS **/

	// public BufferedImage getImage() {

	// }    

    /**
     *	Given the ID number of a state, get the state information.
	 *	@param id - The unique identifier corresponding to the requested state
	 *	@return state - the requested state
     **/
    public State getState(long id) {
    	return state;
    }

    public ArrayList<String> getEvents() {
    	return events;
    }

    public ArrayList<String> getActiveEvents() {
    	return activeEvents;
    }

    public ArrayList<String> getControllableEvents() {
    	return controllableEvents;
    }

    public ArrayList<String> getObservableEvents() {
    	return observableEvents;
    }

}