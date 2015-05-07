public class TestAutomata {

	public static final int MAX_VERBOSE = 3;
	public static final int MAX_VERBOSE_WITH_LINE_SEPARATIONS = 3;
    
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

    	Automaton a = new Automaton();
    	TestCounter counter = new TestCounter();
    	Integer nPassed = 0, nTotalTests = 0;

    		/* Basic Event Creation Tests */

    	printTestOutput("BASIC EVENT CREATION: ", verbose, 2);

    	a.addEvent("firstEvent", true, true);
    	printTestOutput("Adding an event...", verbose, 3);
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 1, verbose, counter);
    	printTestCase("Ensuring that 'controllableEvents' set was expanded", a.getControllableEvents().size() == 1, verbose, counter);
    	printTestCase("Ensuring that 'observableEvents' set was expanded", a.getObservableEvents().size() == 1, verbose, counter);

    		/* Print summary of this test routine */

    	String result = String.format("EVENT CREATION TESTS SUMMARY:\n\t%d/%d test cases passed\n\t%d test cases failed",
    		counter.getPassedTests(),
    		counter.getTotalTests(),
    		counter.getFailedTests());

    	printTestOutput(result, verbose, 1);

    	return nPassed == nTotalTests;

    }

    private static void printTestOutput(String str, int verbose, int requiredVerbose) {

    	// Do not print output if the verbose is not high enough
    	if (verbose < requiredVerbose)
    		return;

    	// Add empty lines
		if (requiredVerbose <= MAX_VERBOSE_WITH_LINE_SEPARATIONS)
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