import java.util.*;
import java.io.*;

public class TestAutomata {

    // Colored output makes it more readable (doesn't work on all operating systems)
    // Example: System.out.println("This will be purple: " + PURPLE + "Purple!" + RESET + " normal color");
    public static String RESET = "\u001B[0m";
    public static String RED = "\u001B[31m";
    public static String GREEN = "\u001B[32m";
    public static String PURPLE = "\u001B[35m";

    // Verbose levels
	private static final int MAX_VERBOSE = 3;
	private static int verbose = 1;
    
    public static void main(String[] args) {

    		/* Turn verbose on if "-v" flag was used, turn on colored output if "-c" flag was used */

        boolean coloredOutput = false;

        for (String arg : args)
    		if (arg.length() >=2) {
                if (arg.substring(0, 2).equals("-v"))
                    verbose = MAX_VERBOSE;
                else if (arg.substring(0, 2).equals("-c"))
                    coloredOutput = true;
            }

        if (!coloredOutput)
            RESET = RED = GREEN = PURPLE = "";
		
			/* Run the testing suite */
    	
    	runTests();

    }

    private static void runTests() {

    	System.out.println(PURPLE + "RUNNING ALL TESTS..." + RESET);

        TestCounter counter = new TestCounter();

    		/* Run tests */

        counter.add(runHelperMethodTestRoutine());
        counter.add(runEventCreationTestRoutine());
        counter.add(runStateCreationTestRoutine());
        counter.add(runAutomatonCapacityTestRoutine());
        counter.add(runGUIInputTestRoutine());
    	counter.add(runAutomataOperationsTestRoutine());

    		/* Print summary of all tests */

    	if (counter.getFailedTests() > 0)
    		System.out.println(String.format("\n%s*** FAILED %d/%d TESTS ***%s", RED, counter.getFailedTests(), counter.getTotalTests(), RESET));
        else
            System.out.println(String.format("\n%sPASSED ALL %d TESTS%s", GREEN, counter.getTotalTests(), RESET));

    }

    private static TestCounter runHelperMethodTestRoutine() {

        String testRoutineName = "HELPER METHOD";

        printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

        TestCounter counter = new TestCounter();

            /* isTrue() Tests */

        printTestOutput("GUI Parsing - isTrue(): ", 2);

        printTestCase("Ensuring that 'T' is parsed correctly", new TestResult(AutomatonGenerator.isTrue("T"), true), counter);
        printTestCase("Ensuring that 't' is parsed correctly", new TestResult(AutomatonGenerator.isTrue("t"), true), counter);
        printTestCase("Ensuring that 'F' is parsed correctly", new TestResult(AutomatonGenerator.isTrue("F"), false), counter);
        printTestCase("Ensuring that 'f' is parsed correctly", new TestResult(AutomatonGenerator.isTrue("f"), false), counter);

            /* isTrueArray() Tests */

        printTestOutput("GUI Parsing - isTrueArray(): ", 2);
        
        boolean[] expected = new boolean[] { true };
        boolean[] actual = AutomatonGenerator.isTrueArray("T");
        printTestCase("Ensuring that 'T' is parsed correctly", new TestResult(actual, expected), counter);

        expected = new boolean[] { false };
        actual = AutomatonGenerator.isTrueArray("f");
        printTestCase("Ensuring that 'f' is parsed correctly", new TestResult(actual, expected), counter);

        expected = new boolean[] { true, false, true };
        actual = AutomatonGenerator.isTrueArray("TFt");
        printTestCase("Ensuring that 'TFt' is parsed correctly", new TestResult(actual, expected), counter);

            /* createCombinedIDWithOrderedSet() Tests */

        printTestOutput("Combining IDs - combineIDs(): ", 2);
        
        ArrayList<Long> list = new ArrayList<Long>();
        list.add(4L);
        list.add(2L);
        list.add(7L);
        printTestCase("Ensuring that {4,2,7} with a max ID of 7 maps to 279", new TestResult(Automaton.combineIDs(list, 7), 279), counter);

        printTestOutput("Separating IDs - separateIDs(): ", 2);
        printTestCase("Ensuring that 279 with a max ID of 7 maps back to {4,2,7}", new TestResult(list, Automaton.separateIDs(279, 7)), counter);

            /* Print summary of this test routine */

        printTestRoutineSummary(testRoutineName, counter);

        return counter;

    }

    private static TestCounter runEventCreationTestRoutine() {

    	String testRoutineName = "EVENT CREATION";

        printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic Event Creation Tests */

    	printTestOutput("BASIC EVENT CREATION: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding an event that is controllable and observable...", 3);
    	int id = a.addEvent("firstEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 1), counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", new TestResult(a.getActiveEvents().size(), 0), counter);
        printTestCase("Ensuring that the added event is observable", new TestResult(a.getEvent(id).isObservable()[0], true), counter);
    	printTestCase("Ensuring that the added event is controllable", new TestResult(a.getEvent(id).isControllable()[0], true), counter);

    	printTestOutput("Adding an event that is observable, but not controllable...", 3);
    	id = a.addEvent("secondEvent", new boolean[] { true }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 2), counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", new TestResult(a.getActiveEvents().size(), 0), counter);
    	printTestCase("Ensuring that the added event is observable", new TestResult(a.getEvent(id).isObservable()[0], true), counter);
        printTestCase("Ensuring that the added event is not controllable", new TestResult(a.getEvent(id).isControllable()[0], false), counter);

    	printTestOutput("Adding an event that is controllable, but not observable...", 3);
    	id = a.addEvent("thirdEvent", new boolean[] { false }, new boolean[] { true });
    	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 3), counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", new TestResult(a.getActiveEvents().size(), 0), counter);
    	printTestCase("Ensuring that the added event is not observable", new TestResult(a.getEvent(id).isObservable()[0], false), counter);
        printTestCase("Ensuring that the added event is controllable", new TestResult(a.getEvent(id).isControllable()[0], true), counter);

    	printTestOutput("Adding an event that neither controllable, nor observable...", 3);
    	id = a.addEvent("fourthEvent", new boolean[] { false }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 4), counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", new TestResult(a.getActiveEvents().size(), 0), counter);
    	printTestCase("Ensuring that the added event is not observable", new TestResult(a.getEvent(id).isObservable()[0], false), counter);
        printTestCase("Ensuring that the added event is not controllable", new TestResult(a.getEvent(id).isControllable()[0], false), counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	id = a.addEvent("fourthEvent", new boolean[] { false }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was not expanded", new TestResult(a.getEvents().size(), 4), counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", new TestResult(a.getActiveEvents().size(), 0), counter);
    	printTestCase("Ensuring that the method returned proper negative value", new TestResult(id, -4), counter);

        a.closeFiles();
    	
        	/* Event ID Assignment Tests */

    	printTestOutput("EVENT ID ASSIGNMENTS: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	a = new Automaton();

    	printTestOutput("Adding an event...", 3);
    	id = a.addEvent("firstEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the event's ID is 1", new TestResult(id, 1), counter);

    	printTestOutput("Adding a second event...", 3);
    	id = a.addEvent("secondEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the event's ID is 2", new TestResult(id, 2), counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	id = a.addEvent("firstEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the method returned proper negative value", new TestResult(id, -1), counter);

        a.closeFiles();

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter;

    }

    private static TestCounter runStateCreationTestRoutine() {

    	String testRoutineName = "STATE CREATION";

        printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    	TestCounter counter = new TestCounter();

    		/* Basic State Creation Tests */

    	printTestOutput("BASIC STATE CREATION: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	Automaton a = new Automaton();

    	printTestOutput("Adding a state that is marked...", 3);
    	long id = a.addState("firstState", true, false);
    	printTestCase("Ensuring that 'nStates' was incremented", new TestResult(a.getNumberOfStates(), 1), counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", new TestResult(a.getStateCapacity(), 255), counter);
    	printTestCase("Ensuring that the added state exists", new TestResult(a.stateExists(id), true), counter);
    	printTestCase("Ensuring that the added state was not labeled the initial state", new TestResult(a.getInitialStateID(), 0), counter);
    	printTestCase("Ensuring that the added state has the proper label", new TestResult(a.getState(id).getLabel(), "firstState"), counter);
    	printTestCase("Ensuring that the added state is marked", new TestResult(a.getState(id).isMarked(), true), counter);

    	printTestOutput("Adding an initial state that is unmarked...", 3);
    	id = a.addState("secondState", false, true);
    	printTestCase("Ensuring that 'nStates' was incremented", new TestResult(a.getNumberOfStates(), 2), counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", new TestResult(a.getStateCapacity(), 255), counter);
    	printTestCase("Ensuring that the added state exists", new TestResult(a.stateExists(id), true), counter);
    	printTestCase("Ensuring that the added state was labeled the initial state", new TestResult(a.getInitialStateID(), id), counter);
    	printTestCase("Ensuring that the added state has the proper label", new TestResult(a.getState(id).getLabel(), "secondState"), counter);
    	printTestCase("Ensuring that the added state is unmarked", new TestResult(a.getState(id).isMarked(), false), counter);

        printTestOutput("Adding a state with a label of maximum length...", 3);
        id = a.addState("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv", false, true);
        printTestCase("Ensuring that the added state has the proper label", new TestResult(a.getState(id).getLabel(), "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv"), counter);

        printTestOutput("Adding a state with a label exceeding maximum length...", 3);
        id = a.addState("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvw", false, true);
        printTestCase("Ensuring that the state was not added", new TestResult(id, 0), counter);

        a.closeFiles();
    	
    		/* State ID Assignment Tests */

    	printTestOutput("STATE ID ASSIGNMENTS: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	a = new Automaton();

    	printTestOutput("Adding a state...", 3);
    	id = a.addState("firstState", true, true);
    	printTestCase("Ensuring that the state's ID is 1", new TestResult(id, 1), counter);

    	printTestOutput("Adding a second state...", 3);
        id = a.addState("secondState", true, true);
        printTestCase("Ensuring that the state's ID is 2", new TestResult(id, 2), counter);

        a.closeFiles();

            /* State Label Trimming Tests */

        printTestOutput("STATE LABEL TRIMMING TESTS: ", 2);

        printTestOutput("Instantiating automaton...", 3);
        a = AutomatonGenerator.generateFromGUICode(
                "", // Events
                "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv0\nabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv1", // States 
                "", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                null // Use temporary files
            );

        a.generateInputForGUI();
        printTestCase("Ensuring the states are correct", new TestResult(a.getStateInput(), "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv,T"), counter);

        a.closeFiles();

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter;

    }

    private static TestCounter runAutomatonCapacityTestRoutine() {

    	String testRoutineName = "AUTOMATON CAPACITY";

        printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    	TestCounter counter = new TestCounter();

    		/* Automaton Capacity Initialization Tests */

    	printTestOutput("AUTOMATON CAPACITY INITIALIZATION: ", 2);

    	printTestOutput("Instantiating empty automaton (State capacity: 0, Transition capacity: 0, Label length: 0, Number of controllers: 0)...", 3);
    	Automaton a = new Automaton(0, 0, 0, 0, true);
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", new TestResult(a.getStateCapacity(), 255), counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", new TestResult(a.getTransitionCapacity(), 1), counter);
        printTestCase("Ensuring that 'labelLength' was reset to '1'", new TestResult(a.getLabelLength(), 1), counter);
    	printTestCase("Ensuring that 'nControllers' was reset to '1'", new TestResult(a.getNumberOfControllers(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(a.getSizeOfStateID(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", new TestResult(a.getSizeOfState(), 4), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: -1, Transition capacity: -1, Label length: -1, Number of controllers: -1)...", 3);
    	a = new Automaton(-1, -1, -1, -1, true);
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", new TestResult(a.getStateCapacity(), 255), counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", new TestResult(a.getTransitionCapacity(), 1), counter);
    	printTestCase("Ensuring that 'labelLength' was reset to '1'", new TestResult(a.getLabelLength(), 1), counter);
        printTestCase("Ensuring that 'nControllers' was reset to '1'", new TestResult(a.getNumberOfControllers(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(a.getSizeOfStateID(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", new TestResult(a.getSizeOfState(), 4), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: 255, Transition capacity: 2, Label length: 1, Number of controllers: 1)...", 3);
    	a = new Automaton(255, 2, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was left at '255'", new TestResult(a.getStateCapacity(), 255), counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '2'", new TestResult(a.getTransitionCapacity(), 2), counter);
    	printTestCase("Ensuring that 'labelLength' was left at '1'", new TestResult(a.getLabelLength(), 1), counter);
        printTestCase("Ensuring that 'nControllers' was left at '1'", new TestResult(a.getNumberOfControllers(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(a.getSizeOfStateID(), 1), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '6'", new TestResult(a.getSizeOfState(), 6), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: 256, Transition capacity: 1, Label length: Automaton.MAX_LABEL_LENGTH, Number of controllers: Automaton.MAX_NUMBER_OF_CONTROLLERS)...", 3);
    	a = new Automaton(256, 1, Automaton.MAX_LABEL_LENGTH, Automaton.MAX_NUMBER_OF_CONTROLLERS, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to '65535'", new TestResult(a.getStateCapacity(), 65535), counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '1'", new TestResult(a.getTransitionCapacity(), 1), counter);
    	printTestCase("Ensuring that 'labelLength' was left at 'Automaton.MAX_LABEL_LENGTH'", new TestResult(a.getLabelLength(), Automaton.MAX_LABEL_LENGTH), counter);
        printTestCase("Ensuring that 'nControllers' was left at 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", new TestResult(a.getNumberOfControllers(), Automaton.MAX_NUMBER_OF_CONTROLLERS), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '2'", new TestResult(a.getSizeOfStateID(), 2), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '104'", new TestResult(a.getSizeOfState(), 104), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE, Transition capacity: Integer.MAX_VALUE, Label length: Automaton.MAX_LABEL_LENGTH + 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE, Integer.MAX_VALUE, Automaton.MAX_LABEL_LENGTH + 1, Automaton.MAX_NUMBER_OF_CONTROLLERS + 1, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE'", new TestResult(a.getStateCapacity(), Long.MAX_VALUE), counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE'", new TestResult(a.getTransitionCapacity(), Integer.MAX_VALUE), counter);
    	printTestCase("Ensuring that 'labelLength' was reduced to 'Automaton.MAX_LABEL_LENGTH'", new TestResult(a.getLabelLength(), Automaton.MAX_LABEL_LENGTH), counter);
        printTestCase("Ensuring that 'nControllers' was reduced to 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", new TestResult(a.getNumberOfControllers(), Automaton.MAX_NUMBER_OF_CONTROLLERS), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", new TestResult(a.getSizeOfStateID(), 8), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '101 + 9 * Integer.MAX_VALUE'", new TestResult(a.getSizeOfState(), 101 + 9 * (long) Integer.MAX_VALUE), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE - 1, Transition capacity: Integer.MAX_VALUE - 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE - 1, Integer.MAX_VALUE - 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", new TestResult(a.getStateCapacity(), Long.MAX_VALUE), counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE - 1'", new TestResult(a.getTransitionCapacity(), Integer.MAX_VALUE - 1), counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", new TestResult(a.getSizeOfStateID(), 8), counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '2 + 9 * (Integer.MAX_VALUE - 1)'", new TestResult(a.getSizeOfState(), 2 + 9 * (long) (Integer.MAX_VALUE - 1)), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: (Long.MAX_VALUE >> 7) + 1, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton((Long.MAX_VALUE >> 7) + 1, 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", new TestResult(a.getStateCapacity(), Long.MAX_VALUE), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE >> 7, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE >> 7, 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE >> 7'", new TestResult(a.getStateCapacity(), Long.MAX_VALUE >> 7), counter);
    	a.closeFiles();

    		/* Print summary of this test routine */

    	printTestRoutineSummary(testRoutineName, counter);

    	return counter;

    }

    private static TestCounter runGUIInputTestRoutine() {

        String testRoutineName = "GUI INPUT";

        printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

        TestCounter counter = new TestCounter();

            /* Basic GUI Input Tests */

        printTestOutput("BASIC GUI INPUT: ", 2);

        printTestOutput("Instantiating automaton from simple GUI input code...", 3);
        Automaton a = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
                "e,T\nf,F", // States   
                "e,a,f\nf,b,e", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                null // Use temporary files to store automaton
            );
        a.generateInputForGUI();
        printTestCase("Ensuring the event input was saved and loaded correctly", new TestResult(a.getEventInput(), "a,T,T\nb,T,F\nc,F,T\nd,F,F"), counter);
        printTestCase("Ensuring the state input was saved and loaded correctly", new TestResult(a.getStateInput(), "e,T\nf,F"), counter);
        printTestCase("Ensuring the transition input was saved and loaded correctly", new TestResult(a.getTransitionInput(), "e,a,f\nf,b,e"), counter);
        a.closeFiles();

        printTestOutput("Instantiating automaton from GUI input code with duplicate labels, omitted optional parameters, and an initial state...", 3);
        a = AutomatonGenerator.generateFromGUICode(
                "a\nb,F,F\na,F,F\nb", // Events
                "@c\nc,F", // States    
                "", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                null // Use temporary files to store automaton
            );
        a.generateInputForGUI();
        printTestCase("Ensuring the event input was saved and loaded correctly", new TestResult(a.getEventInput(), "a,T,T\nb,F,F"), counter);
        printTestCase("Ensuring the state input was saved and loaded correctly", new TestResult(a.getStateInput(), "@c,T"), counter);
        printTestCase("Ensuring the transition input was saved and loaded correctly", new TestResult(a.getTransitionInput(), ""), counter);
        a.closeFiles();

            /* Print summary of this test routine */

        printTestRoutineSummary(testRoutineName, counter);

        return counter;

    }

    private static TestCounter runAutomataOperationsTestRoutine() {

        String testRoutineName = "AUTOMATA OPERATIONS";

        printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

        TestCounter counter = new TestCounter();

            /* Co-Accessible Operation Tests */

        printTestOutput("CO-ACCESSIBLE OPERATION: ", 2);

        printTestOutput("Instantiating automaton from Figure 2.1...", 3);
        Automaton fig2_12 = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "@zero,F\none,F\ntwo,T\nthree,F\nfour,F\nfive,F\nsix,F", // States 
                "zero,a,one\none,a,three\none,b,two\none,g,five\ntwo,g,zero\nthree,b,four\nfour,g,four\nfour,a,three\nsix,a,three\nsix,b,two", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_12.hdr")
            );

        printTestOutput("Taking the co-accessible part of Figure 2.12 (and comparing the result to the automaton in Figure 2.13a)...", 3);
        Automaton result = fig2_12.coaccessible();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero,F\none,F\ntwo,T\nsix,F"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero,a,one\none,b,two\ntwo,g,zero\nsix,b,two"), counter);

            /* Trim Operation Tests */

        printTestOutput("TRIM OPERATION: ", 2);

        printTestOutput("Trimming the automaton in Figure 2.12 (and comparing the result to the automaton in Figure 2.13b)...", 3);
        result = fig2_12.trim();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero,F\none,F\ntwo,T"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero,a,one\none,b,two\ntwo,g,zero"), counter);

            /* Intersection Operation Tests */

        printTestOutput("INTERSECTION OPERATION: ", 2);

        printTestOutput("Instantiating automaton from Figure 2.1...", 3);
        Automaton fig2_1 = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "@x,T\ny,F\nz,T", // States 
                "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_1.hdr")
            );
        printTestOutput("Instantiating automaton from Figure 2.2...", 3);
        Automaton fig2_2 = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,T", // Events
                "@zero,F\none,T", // States 
                "zero,b,zero\nzero,a,one\none,a,one\none,b,zero", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_2.hdr")
            );

        printTestOutput("Taking the intersection of Figure 2.1 and Figure 2.2 (and comparing the result to the first automaton in Figure 2.15)...", 3);
        result = Automaton.intersection(fig2_1, fig2_2);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x_zero,F\nx_one,T"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x_zero,a,x_one\nx_one,a,x_one"), counter);

        printTestOutput("Instantiating automaton from Figure 2.13(b)...", 3);
        Automaton fig2_13b = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "@zero,F\none,F\ntwo,T", // States 
                "zero,a,one\none,b,two\ntwo,g,zero", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_13b.hdr")
            );

        printTestOutput("Taking the intersection of Figure 2.2 and Figure 2.13(b) (and comparing the result to the second automaton in Figure 2.15)...", 3);
        result = Automaton.intersection(fig2_2, fig2_13b);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero_zero,F\none_one,F\nzero_two,F"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero_zero,a,one_one\none_one,b,zero_two"), counter);

        printTestOutput("Instantiating the first automaton from Figure 2.20...", 3);
        Automaton fig2_20a = AutomatonGenerator.generateFromGUICode(
                "a1\na2\nb\nr", // Events
                "@x1,F\nx2,F\nx3,T", // States 
                "x1,a1,x2\nx1,a2,x2\nx2,b,x3\nx3,r,x1", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_20a.hdr")
            );

        printTestOutput("Instantiating the second automaton from Figure 2.20...", 3);
        Automaton fig2_20b = AutomatonGenerator.generateFromGUICode(
                "a1\nb\nc1\nr\na2\nc2", // Events
                "@y1,F\ny2,F\ny3,F\ny4,F\ny5,F\ny6,F", // States 
                "y1,a1,y2\ny2,b,y4\ny4,r,y1\ny4,c1,y6\ny6,r,y1\ny1,a2,y3\ny3,b,y5\ny5,c2,y6\ny5,r,y1", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_20b.hdr")
            );

        printTestOutput("Taking the intersection of the first two automata in Figure 2.20 (and comparing the result to the third automaton in Figure 2.20)...", 3);
        result = Automaton.intersection(fig2_20a, fig2_20b);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a1,T,T\na2,T,T\nb,T,T\nr,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x1_y1,F\nx2_y2,F\nx2_y3,F\nx3_y4,F\nx3_y5,F"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x1_y1,a1,x2_y2\nx1_y1,a2,x2_y3\nx2_y2,b,x3_y4\nx2_y3,b,x3_y5\nx3_y4,r,x1_y1\nx3_y5,r,x1_y1"), counter);

            /* Union Operation Tests */

        printTestOutput("UNION OPERATION: ", 2);

        printTestOutput("Taking the union of Figure 2.1 and Figure 2.2 (and comparing the result to the automaton in Figure 2.16)...", 3);
        result = Automaton.union(fig2_1, fig2_2);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x_zero,F\ny_zero,F\nz_zero,F\nx_one,T\ny_one,F\nz_one,T"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x_zero,a,x_one\nx_zero,g,z_zero\ny_zero,b,y_zero\ny_zero,a,x_one\nz_zero,b,z_zero\nz_zero,a,y_one\nz_zero,g,y_zero\nx_one,a,x_one\nx_one,g,z_one\ny_one,b,y_zero\ny_one,a,x_one\nz_one,b,z_zero\nz_one,a,y_one\nz_one,g,y_one"), counter);

        printTestOutput("Instantiating the first automaton from Figure 2.17...", 3);
        Automaton fig2_17a = AutomatonGenerator.generateFromGUICode(
                "a,T,T\nb,T,T\nc,T,T", // Events
                "@one,T\ntwo,F", // States 
                "one,c,one\none,a,two\ntwo,b,two", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17a.hdr")
            );

        printTestOutput("Instantiating the second automaton from Figure 2.17...", 3);
        Automaton fig2_17b = AutomatonGenerator.generateFromGUICode(
                "b,T,T\na,T,T\nd,T,T", // Events
                "@A,T\nB,F", // States 
                "A,b,A\nA,a,B\nB,d,B", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17b.hdr")
            );

        printTestOutput("Instantiating the third automaton from Figure 2.17...", 3);
        Automaton fig2_17c = AutomatonGenerator.generateFromGUICode(
                "c,T,T\nb,T,T\na,T,T", // Events
                "@D,T\nE,F", // States 
                "D,c,D\nD,b,E\nE,a,E", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17c.hdr")
            );

        printTestOutput("Taking the union of the three automata in Figure 2.17 (and comparing the result to the automaton described in Example 2.17)...", 3);
        result = Automaton.union(Automaton.union(fig2_17a, fig2_17b), fig2_17c);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\nc,T,T\nd,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@one_A_D,T"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "one_A_D,c,one_A_D"), counter);

            /* Synchronized Composition Operation Tests */

        printTestOutput("SYNCHRONIZED COMPOSITION OPERATION: ", 2);

        printTestOutput("Instantiating an automaton...", 3);
        Automaton synchronizedCompositionExample = AutomatonGenerator.generateFromGUICode(
                "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
                "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
                "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD", // Transitions
                2, // Number of controllers
                false, // We do not want it to be verbose
                new File("synchronizedCompositionExample.hdr")
            );

        printTestOutput("Taking the synchronized composition of an automaton...", 3);
        result = synchronizedCompositionExample.synchronizedComposition();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "<a_a_*>,T,T\n<b_*_b>,T,T\n<*_b_*>,T,T\n<*_*_a>,T,T\n<o_o_o>,T,T"), counter);
        printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@1_1_1,F\n1_1_2,F\n1_3_1,F\n1_3_2,F\n2_2_1,F\n2_2_2,F\n2_4_1,F\n2_4_2,F\n2_5_1,F\n2_5_2,F\n3_1_3,F\n3_1_4,F\n3_1_5,F\n3_3_3,F\n3_3_4,F\n3_3_5,F\n4_2_3,F\n4_2_4,F\n4_2_5,F\n4_4_3,F\n4_4_4,F\n4_4_5,F\n4_5_3,F\n4_5_4,F\n4_5_5,F\n5_2_3,F\n5_2_4,F\n5_2_5,F\n5_4_3,F\n5_4_4,F\n5_4_5,F\n5_5_3,F\n5_5_4,F\n5_5_5,F\n6_6_6,F\n6_6_7,F\n6_7_6,F\n6_7_7,F\n7_6_6,F\n7_6_7,F\n7_7_6,F\n7_7_7,F"), counter);
        printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "1_1_1,<a_a_*>,2_2_1\n1_1_1,<b_*_b>,3_1_3\n1_1_1,<*_b_*>,1_3_1\n1_1_1,<*_*_a>,1_1_2\n1_1_2,<a_a_*>,2_2_2\n1_1_2,<b_*_b>,3_1_4\n1_1_2,<*_b_*>,1_3_2\n1_3_1,<a_a_*>,2_5_1\n1_3_1,<b_*_b>,3_3_3\n1_3_1,<*_*_a>,1_3_2\n1_3_2,<a_a_*>,2_5_2\n1_3_2,<b_*_b>,3_3_4\n2_2_1,<b_*_b>,4_2_3\n2_2_1,<*_b_*>,2_4_1\n2_2_1,<*_*_a>,2_2_2\n2_2_2,<b_*_b>,4_2_4\n2_2_2,<*_b_*>,2_4_2\n2_4_1,<b_*_b>,4_4_3\n2_4_1,<*_*_a>,2_4_2\n2_4_2,<b_*_b>,4_4_4\n2_5_1,<b_*_b>,4_5_3\n2_5_1,<*_*_a>,2_5_2\n2_5_2,<b_*_b>,4_5_4\n3_1_3,<a_a_*>,5_2_3\n3_1_3,<*_b_*>,3_3_3\n3_1_3,<*_*_a>,3_1_5\n3_1_4,<a_a_*>,5_2_4\n3_1_4,<*_b_*>,3_3_4\n3_1_5,<a_a_*>,5_2_5\n3_1_5,<*_b_*>,3_3_5\n3_3_3,<a_a_*>,5_5_3\n3_3_3,<*_*_a>,3_3_5\n3_3_4,<a_a_*>,5_5_4\n3_3_5,<a_a_*>,5_5_5\n4_2_3,<*_b_*>,4_4_3\n4_2_3,<*_*_a>,4_2_5\n4_2_4,<*_b_*>,4_4_4\n4_2_5,<*_b_*>,4_4_5\n4_4_3,<*_*_a>,4_4_5\n4_4_4,<o_o_o>,6_6_6\n4_4_5,<o_o_o>,6_6_7\n4_5_3,<*_*_a>,4_5_5\n4_5_4,<o_o_o>,6_7_6\n4_5_5,<o_o_o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*_b_*>,5_4_3\n5_2_3,<*_*_a>,5_2_5\n5_2_4,<*_b_*>,5_4_4\n5_2_5,<*_b_*>,5_4_5\n5_4_3,<*_*_a>,5_4_5\n5_4_4,<o_o_o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o_o_o>,7_6_7\n5_5_3,<*_*_a>,5_5_5\n5_5_4,<o_o_o>,7_7_6\n5_5_5,<o_o_o>,7_7_7"), counter);

            /* Print summary of this test routine */

        printTestRoutineSummary(testRoutineName, counter);

        return counter;

    }

    private static boolean areEqual(String expected, String actual) {
        if (expected.equals(actual))
            return true;

        for (int i = 0; i < MAX_VERBOSE; i++)
            System.out.print("\t");
        System.out.println("EXPECTED: " + expected);

        for (int i = 0; i < MAX_VERBOSE; i++)
            System.out.print("\t");
        System.out.println("ACTUAL: " + actual);

        return false;
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
    	System.out.println(PURPLE + str + RESET);

    }

    private static void printTestCase(String str, TestResult result, TestCounter counter) {

            /* Update counters */

        counter.increment(result.passed);

            /* Print output */

        // Do not print output if the verbose is not high enough (unless the test case failed)
        if (verbose == MAX_VERBOSE || !result.passed) {
            
            // Indent the line
            for (int i = 0; i < MAX_VERBOSE; i++)
                System.out.print("\t");

            // Print test case results
            System.out.println(str + ": " + (result.passed ? GREEN + "PASSED" + RESET : RED + "*** FAILED ***" + RESET));
            System.out.print(result.getSummary(MAX_VERBOSE + 1));

        }

    }

    /**
     * Helper method to print the results of a test routine.
     * @param testRoutineName - The test routine's name is used as part of the output.
     * @param counter - The counter contains the information about how many test cases passed and failed
     **/
    private static void printTestRoutineSummary(String testRoutineName, TestCounter counter) {

    	String passed = (counter.getPassedTests() > 0) ? String.format("\n\t\t%sPASSED: %d/%d%s", GREEN, counter.getPassedTests(), counter.getTotalTests(), RESET) : "";
    	String failed = (counter.getFailedTests() > 0) ? String.format("\n\t\t%s*** FAILED: %d/%d ***%s", RED, counter.getFailedTests(), counter.getTotalTests(), RESET) : "";

    	printTestOutput(testRoutineName + " TEST ROUTINE SUMMARY:" + passed + failed, 1);

    }

}

class TestResult {

    public boolean passed;
    private String summary = "";

    public TestResult(long expected, long actual) {
        if (expected == actual)
            passed = true;
        else {
            passed = false;
            summary += "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
        }
    }

    public TestResult(boolean expected, boolean actual) {
        if (expected == actual)
            passed = true;
        else {
            passed = false;
            summary += "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
        }
    }

    public TestResult(String expected, String actual) {
        if (expected.equals(actual))
            passed = true;
        else {
            passed = false;
            summary += "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
        }
    }

    public TestResult(List<Long> expected, List<Long> actual) {
        if (expected.equals(actual))
            passed = true;
        else {
            passed = false;
            summary += "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
        }
    }

    public TestResult(boolean[] expected, boolean[] actual) {
        if (Arrays.equals(expected, actual))
            passed = true;
        else {
            passed = false;
            summary += "\nEXPECTED:\n" + Arrays.toString(expected) + "\n\nACTUAL:\n" + Arrays.toString(actual)  + "\n";
        }
    }

    public String getSummary(int nTabs) {

        String indentation = "";
        for (int i = 0; i < nTabs; i++)
            indentation += "\t";

        return summary.replaceAll("(?m)^", indentation);

    }

}

class TestCounter {

    	private int nFailedTests = 0,
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

        public void add(TestCounter counter) {
            nFailedTests += counter.getFailedTests();
            nPassedTests += counter.getPassedTests();
        }

    }