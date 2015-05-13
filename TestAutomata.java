public class TestAutomata {

	public static final int MAX_VERBOSE = 3;
	private static int verbose = 1;
    
    public static void main(String[] args) {

    		/* Turn verbose on if "-v" flag was used */

    	if (args.length > 0)
    		if (args[0].length() >=2 && args[0].substring(0, 2).equals("-v"))
    			verbose = MAX_VERBOSE;
		
			/* Run the testing suite */
    	
    	runTests();

    }

    private static boolean runTests() {

    	System.out.println("RUNNING ALL TESTS...");

    	boolean passedAllTests = true;

    		/* Run tests */

    	if (!runEventCreationTestRoutine())
    		passedAllTests = false;

    	if (!runAutomatonCapacityTestRoutine())
    		passedAllTests = false;

    	if (!runGUIInputTestRoutine())
    		passedAllTests = false;

    		/* Print summary of all tests */

    	if (passedAllTests)
    		System.out.println("\nPASSED ALL TESTS");
    	else
    		System.out.println("\nDID NOT PASS ALL TESTS");

    	return passedAllTests;

    }

    private static boolean runEventCreationTestRoutine() {

    	String testRoutineName = "EVENT CREATION";

    	printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic Event Creation Tests */

    	printTestOutput("BASIC EVENT CREATION: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding an event that is controllable and observable...", 3);
    	a.addEvent("firstEvent", true, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 1, counter);

    	printTestOutput("Adding an event that is observable, but not controllable...", 3);
    	a.addEvent("secondEvent", true, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event that is controllable, but not observable...", 3);
    	a.addEvent("thirdEvent", false, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 3, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 3, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event that neither controllable, nor observable...", 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with a different observability property...", 3);
    	a.addEvent("firstEvent", false, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 5, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 5, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 3, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with a different controllability property...", 3);
    	a.addEvent("secondEvent", true, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 6, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 6, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 3, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with different controllability and observability properties...", 3);
    	a.addEvent("thirdEvent", true, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 7, counter);
    	printTestCase("Ensuring that 'activeEvents' set was expanded", a.getActiveEvents().size() == 7, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 4, counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was not expanded", a.getEvents().size() == 7, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 7, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 4, counter);

    		/* Event ID Assignment Tests */

    	printTestOutput("EVENT ID ASSIGNMENTS: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	a = new Automaton();

    	printTestOutput("Adding an event...", 3);
    	a.addEvent("firstEvent", true, true);
    	boolean passed = false;
    	for (Event e : a.getEvents())
    		if (e.getID() == 1)
    			passed = true;
    	printTestCase("Ensuring that the event's ID is 1", passed, counter);

    	printTestOutput("Adding a second event...", 3);
    	a.addEvent("secondEvent", true, true);
    	passed = false;
    	for (Event e : a.getEvents())
    		if (e.getID() == 2)
    			passed = true;
    	printTestCase("Ensuring that the event's ID is 2", passed, counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	a.addEvent("firstEvent", true, true);
    	passed = true;
    	for (Event e : a.getEvents())
    		if (e.getID() == 3)
    			passed = false;
    	printTestCase("Ensuring that no event has an ID of 3", passed, counter);

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter.getPassedTests() == counter.getTotalTests();

    }

    private static boolean runAutomatonCapacityTestRoutine() {

    	String testRoutineName = "AUTOMATON CAPACITY";

    	printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    	TestCounter counter = new TestCounter();

    		/* Automaton Capacity Initialization Tests */

    	printTestOutput("AUTOMATON CAPACITY INITIALIZATION: ", 2);

    	printTestOutput("Instantiating empty automaton (State capacity: 0, Transition capacity: 0, Label length: 0)...", 3);
    	Automaton a = new Automaton(0, 0, 0, true);
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", a.getTransitionCapacity() == 1, counter);
    	printTestCase("Ensuring that 'labelLength' was reset to '1'", a.getLabelLength() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", a.getSizeOfState() == 4, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: -1, Transition capacity: -1, Label length: -1)...", 3);
    	a = new Automaton(-1, -1, -1, true);
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", a.getTransitionCapacity() == 1, counter);
    	printTestCase("Ensuring that 'labelLength' was reset to '1'", a.getLabelLength() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", a.getSizeOfState() == 4, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: 255, Transition capacity: 2, Label length: 1)...", 3);
    	a = new Automaton(255, 2, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was left at '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '2'", a.getTransitionCapacity() == 2, counter);
    	printTestCase("Ensuring that 'labelLength' was left at '1'", a.getLabelLength() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '6'", a.getSizeOfState() == 6, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: 256, Transition capacity: 1, Label length: 100)...", 3);
    	a = new Automaton(256, 1, 100, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to '65535'", a.getStateCapacity() == 65535, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '1'", a.getTransitionCapacity() == 1, counter);
    	printTestCase("Ensuring that 'labelLength' was left at '100'", a.getLabelLength() == 100, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '2'", a.getSizeOfStateID() == 2, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '104'", a.getSizeOfState() == 104, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE, Transition capacity: Integer.MAX_VALUE, Label length: 101)...", 3);
    	a = new Automaton(Long.MAX_VALUE, Integer.MAX_VALUE, 101, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE'", a.getTransitionCapacity() == Integer.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'labelLength' was reduced to '100'", a.getLabelLength() == 100, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", a.getSizeOfStateID() == 8, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '101 + 9 * Integer.MAX_VALUE'", a.getSizeOfState() == 101 + 9 * (long) Integer.MAX_VALUE, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE - 1, Transition capacity: Integer.MAX_VALUE - 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE - 1, Integer.MAX_VALUE - 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE - 1'", a.getTransitionCapacity() == Integer.MAX_VALUE - 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", a.getSizeOfStateID() == 8, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '2 + 9 * (Integer.MAX_VALUE - 1)'", a.getSizeOfState() == 2 + 9 * (long) (Integer.MAX_VALUE - 1), counter);

    	printTestOutput("Instantiating empty automaton (State capacity: (Long.MAX_VALUE >> 7) + 1, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton((Long.MAX_VALUE >> 7) + 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE >> 7, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE >> 7, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE >> 7'", a.getStateCapacity() == Long.MAX_VALUE >> 7, counter);
    	
    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter.getPassedTests() == counter.getTotalTests();

    }

    private static boolean runGUIInputTestRoutine() {

    	String testRoutineName = "GUI INPUT";

    	printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic GUI Input Tests */

    	printTestOutput("BASIC GUI INPUT: ", 2);

    	printTestOutput("Instantiating automaton from simple GUI input code...", 3);
    	Automaton a = AutomataGUI.generateAutomaton(
    			"a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
    			"e,T\nf,F", // States	
    			"e,a,f\nf,b,e", // Transitions
    			false, // We do not want it to be verbose
    			null // Use temporary files to store automaton
    		);
    	a.generateInputForGUI();
    	printTestCase("Ensuring the event input was saved and loaded correctly", a.getEventInput().equals("a,T,T\nb,T,F\nc,F,T\nd,F,F"), counter);
    	printTestCase("Ensuring the state input was saved and loaded correctly", a.getStateInput().equals("e,T\nf,F"), counter);
    	printTestCase("Ensuring the transition input was saved and loaded correctly", a.getTransitionInput().equals("e,a,f\nf,b,e"), counter);

    	printTestOutput("Instantiating automaton from GUI input code with duplicate labels, omitted optional parameters, and an initial state...", 3);
    	a = AutomataGUI.generateAutomaton(
    			"a\nb,F\na,F,F\nb", // Events
    			"*c\nc,F", // States	
    			"", // Transitions
    			false, // We do not want it to be verbose
    			null // Use temporary files to store automaton
    		);
    	a.generateInputForGUI();
    	printTestCase("Ensuring the event input was saved and loaded correctly", a.getEventInput().equals("a,T,T\nb,F,T"), counter);
    	printTestCase("Ensuring the state input was saved and loaded correctly", a.getStateInput().equals("*c,T"), counter);
    	printTestCase("Ensuring the transition input was saved and loaded correctly", a.getTransitionInput().equals(""), counter);

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter.getPassedTests() == counter.getTotalTests();

    }

    private static void printTestOutput(String str, int requiredVerbose) {

    	// Do not print output if the verbose is not high enough
    	if (verbose < requiredVerbose)
    		return;

    	// Add empty line
		System.out.println();

		// Indent the line
    	for (int i = 0; i < requiredVerbose; i++)
    		System.out.print("\t");

    	// Print output
    	System.out.println(str);

    }

    private static void printTestCase(String str, boolean passed, TestCounter counter) {

    		/* Update counters */

    	counter.increment(passed);

    		/* Print output */

    	// Do not print output if the verbose is not high enough (unless the test case failed)
    	if (verbose == MAX_VERBOSE || !passed) {
	    	
	    	// Indent the line
	    	for (int i = 0; i < MAX_VERBOSE; i++)
	    		System.out.print("\t");

	    	// Print test case results
	    	System.out.println(str + ": " + (passed ? "PASSED" : "*** FAILED ***"));

	    }

    }

    /**
     * Helper method to print the results of a test routine.
     * @param testRoutineName - The test routine's name is used as part of the output.
     * @param counter - The counter contains the information about how many test cases passed and failed
     **/
    private static void printTestRoutineSummary(String testRoutineName, TestCounter counter) {

    	String passed = (counter.getPassedTests() > 0) ? String.format("\n\t\tPASSED: %d/%d", counter.getPassedTests(), counter.getTotalTests()) : "";
    	String failed = (counter.getFailedTests() > 0) ? String.format("\n\t\tFAILED: %d/%d", counter.getFailedTests(), counter.getTotalTests()) : "";

    	printTestOutput(testRoutineName + " TEST ROUTINE SUMMARY:" + passed + failed, 1);

    }

}

class TestCounter {

    	private int 	nFailedTests = 0,
    					nPassedTests = 0;

    	public void increment(boolean passed) {
    		if (passed)
    			nPassedTests++;
    		else
    			nFailedTests++;
    	}

    	public int getFailedTests() {
    		return nFailedTests;
    	}

    	public int getPassedTests() {
    		return nPassedTests;
    	}

    	public int getTotalTests() {
    		return nFailedTests + nPassedTests;
    	}

    }