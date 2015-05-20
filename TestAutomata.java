import java.io.*;

public class TestAutomata {

	private static final int MAX_VERBOSE = 3;
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

    	if (!runStateCreationTestRoutine())
    		passedAllTests = false;

    	if (!runAutomatonCapacityTestRoutine())
    		passedAllTests = false;

    	if (!runGUIInputTestRoutine())
    		passedAllTests = false;

        if (!runAutomataOperationsTestRoutine())
            passedAllTests = false;

    		/* Print summary of all tests */

    	if (passedAllTests)
    		System.out.println("\nPASSED ALL TESTS");
    	else
    		System.out.println("\n*** DID NOT PASS ALL TESTS ***");

    	return passedAllTests;

    }

    private static boolean runEventCreationTestRoutine() {

    	String testRoutineName = "EVENT CREATION";

        if (verbose > 1)
    	   printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic Event Creation Tests */

    	printTestOutput("BASIC EVENT CREATION: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding an event that is controllable and observable...", 3);
    	a.addEvent("firstEvent", true, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 1, counter);

    	printTestOutput("Adding an event that is observable, but not controllable...", 3);
    	a.addEvent("secondEvent", true, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event that is controllable, but not observable...", 3);
    	a.addEvent("thirdEvent", false, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 3, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding an event that neither controllable, nor observable...", 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was not expanded", a.getEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	// printTestOutput("Adding an event with a pre-existing label, but with a different observability property...", 3);
    	// a.addEvent("firstEvent", false, true);
    	// printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 5, counter);
    	// printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	// printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 3, counter);
    	// printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, counter);

    	// printTestOutput("Adding an event with a pre-existing label, but with a different controllability property...", 3);
    	// a.addEvent("secondEvent", true, true);
    	// printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 6, counter);
    	// printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	// printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 4, counter);
    	// printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 3, counter);

    	// printTestOutput("Adding an event with a pre-existing label, but with different controllability and observability properties...", 3);
    	// a.addEvent("thirdEvent", true, false);
    	// printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 7, counter);
    	// printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	// printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 4, counter);
    	// printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 4, counter);

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

    private static boolean runStateCreationTestRoutine() {

    	String testRoutineName = "STATE CREATION";

        if (verbose > 1)
    	   printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic State Creation Tests */

    	printTestOutput("BASIC STATE CREATION: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding a state that is marked...", 3);
    	a.addState("firstState", true, false);
    	printTestCase("Ensuring that 'nStates' was incremented", a.getNumberOfStates() == 1, counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that the added state exists", a.stateExists(1), counter);
    	printTestCase("Ensuring that the added state was not labeled the initial state", a.getInitialStateID() == 0, counter);
    	printTestCase("Ensuring that the added state has the proper label", a.getState(1).getLabel().equals("firstState"), counter);
    	printTestCase("Ensuring that the added state is marked", a.getState(1).isMarked(), counter);

    	printTestOutput("Adding an initial state that is unmarked...", 3);
    	a.addState("secondState", false, true);
    	printTestCase("Ensuring that 'nStates' was incremented", a.getNumberOfStates() == 2, counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that the added state exists", a.stateExists(2), counter);
    	printTestCase("Ensuring that the added state was labeled the initial state", a.getInitialStateID() == 2, counter);
    	printTestCase("Ensuring that the added state has the proper label", a.getState(2).getLabel().equals("secondState"), counter);
    	printTestCase("Ensuring that the added state is unmarked", !a.getState(2).isMarked(), counter);
    	
    		/* State ID Assignment Tests */

    	// printTestOutput("STATE ID ASSIGNMENTS: ", 2);

    	// printTestOutput("Instantiating empty automaton...", 3);
    	// a = new Automaton();

    	// printTestOutput("Adding an event...", 3);
    	// a.addEvent("firstEvent", true, true);
    	// boolean passed = false;
    	// for (Event e : a.getEvents())
    	// 	if (e.getID() == 1)
    	// 		passed = true;
    	// printTestCase("Ensuring that the event's ID is 1", passed, counter);

    	// printTestOutput("Adding a second event...", 3);
    	// a.addEvent("secondEvent", true, true);
    	// passed = false;
    	// for (Event e : a.getEvents())
    	// 	if (e.getID() == 2)
    	// 		passed = true;
    	// printTestCase("Ensuring that the event's ID is 2", passed, counter);

    	// printTestOutput("Adding a pre-existing event...", 3);
    	// a.addEvent("firstEvent", true, true);
    	// passed = true;
    	// for (Event e : a.getEvents())
    	// 	if (e.getID() == 3)
    	// 		passed = false;
    	// printTestCase("Ensuring that no event has an ID of 3", passed, counter);

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter.getPassedTests() == counter.getTotalTests();

    }

    private static boolean runAutomatonCapacityTestRoutine() {

    	String testRoutineName = "AUTOMATON CAPACITY";

        if (verbose > 1)
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

        if (verbose > 1)
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

    private static boolean runAutomataOperationsTestRoutine() {

        String testRoutineName = "AUTOMATA OPERATIONS";

        if (verbose > 1)
            printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

        TestCounter counter = new TestCounter();

            /* Co-Accessible Operation Tests */

        printTestOutput("CO-ACCESSIBLE OPERATION: ", 2);

        printTestOutput("Instantiating automaton from Figure 2.1...", 3);
        Automaton fig2_12 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "*zero,F\none,F\ntwo,T\nthree,F\nfour,F\nfive,F\nsix,F", // States 
                "zero,a,one\none,a,three\none,b,two\none,g,five\ntwo,g,zero\nthree,b,four\nfour,g,four\nfour,a,three\nsix,a,three\nsix,b,two", // Transitions
                false, // We do not want it to be verbose
                new File("fig2_12.hdr")
            );

        printTestOutput("Taking the co-accessible part of Figure 2.12 (and comparing the result to the automaton in Figure 2.13a)...", 3);
        Automaton result = fig2_12.coaccessible();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*zero,F\none,F\ntwo,T\nsix,F"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("zero,a,one\none,b,two\ntwo,g,zero\nsix,b,two"), counter);

            /* Intersection Operation Tests */

        printTestOutput("INTERSECTION OPERATION: ", 2);

        printTestOutput("Instantiating automaton from Figure 2.1...", 3);
        Automaton fig2_1 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "*x,T\ny,F\nz,T", // States 
                "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y", // Transitions
                false, // We do not want it to be verbose
                new File("fig2_1.hdr")
            );
        printTestOutput("Instantiating automaton from Figure 2.2...", 3);
        Automaton fig2_2 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T", // Events
                "*zero,F\none,T", // States 
                "zero,b,zero\nzero,a,one\none,a,one\none,b,zero", // Transitions
                false, // We do not want it to be verbose
                new File("fig2_2.hdr")
            );

        printTestOutput("Taking the intersection of Figure 2.1 and Figure 2.2 (and comparing the result to the first automaton in Figure 2.15)...", 3);
        result = Automaton.intersection(fig2_1, fig2_2);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*x_zero,F\nx_one,T"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("x_zero,a,x_one\nx_one,a,x_one"), counter);

        printTestOutput("Instantiating automaton from Figure 2.13(b)...", 3);
        Automaton fig2_13b = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "*zero,F\none,F\ntwo,T", // States 
                "zero,a,one\none,b,two\ntwo,g,zero", // Transitions
                false, // We do not want it to be verbose
                new File("fig2_13b.hdr")
            );

        printTestOutput("Taking the intersection of Figure 2.2 and Figure 2.13(b) (and comparing the result to the second automaton in Figure 2.15)...", 3);
        result = Automaton.intersection(fig2_2, fig2_13b);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*zero_zero,F\none_one,F\nzero_two,F"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("zero_zero,a,one_one\none_one,b,zero_two"), counter);

            /* Union Operation Tests */

        printTestOutput("UNION OPERATION: ", 2);

        printTestOutput("Taking the union of Figure 2.1 and Figure 2.2 (and comparing the result the automaton in Figure 2.16)...", 3);
        result = Automaton.union(fig2_1, fig2_2);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*x_zero,F\ny_zero,F\nz_zero,F\nx_one,T\ny_one,F\nz_one,T"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("x_zero,a,x_one\nx_zero,g,z_zero\ny_zero,b,y_zero\n"
            + "y_zero,a,x_one\nz_zero,b,z_zero\nz_zero,a,y_one\nz_zero,g,y_zero\nx_one,a,x_one\nx_one,g,z_one\ny_one,b,y_zero\ny_one,a,x_one\nz_one,b,z_zero\n"
            + "z_one,a,y_one\nz_one,g,y_one"), counter);

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