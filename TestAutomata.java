public class TestAutomata {

	public static final int MAX_VERBOSE = 3;
    
    public static void main(String[] args) {
		
    	runTests(MAX_VERBOSE);

    }

    private static boolean runTests(int verbose) {

    	System.out.println("RUNNING ALL TESTS...");

    	boolean passedAllTests = true;

    		/* Run tests */

    	if (!runEventCreationTestRoutine(verbose))
    		passedAllTests = false;

    		/* Print summary of all tests */

    	if (passedAllTests)
    		System.out.println("\nPASSED ALL TESTS");
    	else
    		System.out.println("\nDID NOT PASS ALL TESTS");

    	return passedAllTests;

    }

    private static boolean runEventCreationTestRoutine(int verbose) {

    	printTestOutput("RUNNING EVENT CREATION TESTS...", verbose, 1);

    	TestCounter counter = new TestCounter();

    		/* Basic Event Creation Tests */

    	printTestOutput("BASIC EVENT CREATION: ", verbose, 2);

    	printTestOutput("Instantiating empty automaton...", verbose, 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding an event that is controllable and observable...", verbose, 3);
    	a.addEvent("firstEvent", true, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 1, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 1, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 1, verbose, counter);

    	printTestOutput("Adding an event that is observable, but not controllable...", verbose, 3);
    	a.addEvent("secondEvent", true, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 2, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 1, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 2, verbose, counter);

    	printTestOutput("Adding an event that is controllable, but not observable...", verbose, 3);
    	a.addEvent("thirdEvent", false, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 3, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 2, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, verbose, counter);

    	printTestOutput("Adding an event that neither controllable, nor observable...", verbose, 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 4, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 2, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, verbose, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with a different observability property...", verbose, 3);
    	a.addEvent("firstEvent", false, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 5, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 3, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 2, verbose, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with a different controllability property...", verbose, 3);
    	a.addEvent("secondEvent", true, true);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 6, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 4, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 3, verbose, counter);

    	printTestOutput("Adding an event with a pre-existing label, but with different controllability and observability properties...", verbose, 3);
    	a.addEvent("thirdEvent", true, false);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 7, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 4, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 4, verbose, counter);

    	printTestOutput("Adding a pre-existing event...", verbose, 3);
    	a.addEvent("fourthEvent", false, false);
    	printTestCase("Ensuring that 'events' set was not expanded", a.getEvents().size() == 7, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was not expanded", a.getControllableEvents().size() == 4, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was not expanded", a.getObservableEvents().size() == 4, verbose, counter);

    		/* Basic Event Creation Tests */

    	printTestOutput("EVENT ID ASSIGNMENTS: ", verbose, 2);

    	printTestOutput("Instantiating empty automaton...", verbose, 3);
    	a = new Automaton();

    	printTestOutput("Adding an event...", verbose, 3);
    	a.addEvent("firstEvent", true, true);
    	boolean passed = false;
    	for (Event e : a.getEvents())
    		if (e.getID() == 1)
    			passed = true;
    	printTestCase("Ensuring that the event's ID is 1", passed, verbose, counter);

    	printTestOutput("Adding a second event...", verbose, 3);
    	a.addEvent("secondEvent", true, true);
    	passed = false;
    	for (Event e : a.getEvents())
    		if (e.getID() == 2)
    			passed = true;
    	printTestCase("Ensuring that the event's ID is 2", passed, verbose, counter);

    	printTestOutput("Adding a pre-existing event...", verbose, 3);
    	a.addEvent("firstEvent", true, true);
    	passed = true;
    	for (Event e : a.getEvents())
    		if (e.getID() == 3)
    			passed = false;
    	printTestCase("Ensuring that no event has an ID of 3", passed, verbose, counter);

    		/* Print summary of this test routine */

    	String result = String.format("EVENT CREATION TESTS SUMMARY:\n\t%d/%d test cases passed\n\t%d test cases failed",
    		counter.getPassedTests(),
    		counter.getTotalTests(),
    		counter.getFailedTests());

    	printTestOutput(result, verbose, 1);

    	return counter.getPassedTests() == counter.getTotalTests();

    }

    private static void printTestOutput(String str, int verbose, int requiredVerbose) {

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

    private static void printTestCase(String str, boolean passed, int verbose, TestCounter counter) {

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

}

class TestCounter {

    	private static int 	nFailedTests = 0,
    						nPassedTests = 0;

    	public static void increment(boolean passed) {
    		if (passed)
    			nPassedTests++;
    		else
    			nFailedTests++;
    	}

    	public static int getFailedTests() {
    		return nFailedTests;
    	}

    	public static int getPassedTests() {
    		return nPassedTests;
    	}

    	public static int getTotalTests() {
    		return nFailedTests + nPassedTests;
    	}

    }