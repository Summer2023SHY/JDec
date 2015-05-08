import java.util.*;
import java.io.*;

public class State {
    
		/* Private instance variables */
	
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

	// Create state with no transitions
	public State(String label, long id, boolean marked) {
		this.label = label;
		this.id = id;
		this.marked = marked;
		transitions = new ArrayList<Transition>();
	}

	public int getNumberOfTransitions() {
		return transitions.size();
	}

	// 	unfinished
	public boolean writeToFile(RandomAccessFile file, int nBytesPerState, int nTransitionsPerState) {
		
		try {

            file.seek(id * nBytesPerState);

            // for (int i = 0; i < nBytesPerState; i++) {
            //     file.writeUnsignedByte();
            // }

            return true;
          
	    } catch (IOException e) {
            e.printStackTrace();
            return false;
	    }

	}

	// 	unfinished
	public static State readFromFile(RandomAccessFile file, long id, int nBytesPerState, int nTransitionsPerState) {

		try {

            file.seek(id * nBytesPerState);

            for (int i = 0; i < nBytesPerState; i++) {
                int val = file.readUnsignedByte();
            }

            return null; // temporary
          
	    } catch (IOException e) {
            e.printStackTrace();
            return null;
	    }

	}

}