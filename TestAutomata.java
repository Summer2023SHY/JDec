import java.util.*;
import java.io.*;

public class TestAutomata {

    // Colored output makes it more readable (doesn't work on all operating systems)
    // Example: System.out.println("This will be purple: " + PURPLE + "Purple!" + RESET + " normal color");
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String PURPLE = "\u001B[35m";

    // Verbose levels
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

        printTestCase("Ensuring that 'T' is parsed correctly", AutomataGUI.isTrue("T"), counter);
        printTestCase("Ensuring that 't' is parsed correctly", AutomataGUI.isTrue("t"), counter);
        printTestCase("Ensuring that 'F' is parsed correctly", !AutomataGUI.isTrue("F"), counter);
        printTestCase("Ensuring that 'f' is parsed correctly", !AutomataGUI.isTrue("f"), counter);

            /* isTrueArray() Tests */

        printTestOutput("GUI Parsing - isTrueArray(): ", 2);
        
        boolean[] expected = new boolean[] { true };
        boolean[] actual = AutomataGUI.isTrueArray("T");
        printTestCase("Ensuring that 'T' is parsed correctly", Arrays.equals(actual, expected), counter);

        expected = new boolean[] { false };
        actual = AutomataGUI.isTrueArray("f");
        printTestCase("Ensuring that 'f' is parsed correctly", Arrays.equals(actual, expected), counter);

        expected = new boolean[] { true, false, true };
        actual = AutomataGUI.isTrueArray("TFT");
        printTestCase("Ensuring that 'TFT' is parsed correctly", Arrays.equals(actual, expected), counter);

            /* createCombinedIDWithOrderedSet() Tests */

        printTestOutput("Combining IDs - combineIDs(): ", 2);
        
        ArrayList<Long> list = new ArrayList<Long>();
        list.add(4L);
        list.add(2L);
        list.add(7L);
        printTestCase("Ensuring that {4,2,7} with a max ID of 7 maps to 279", Automaton.combineIDs(list, 7) == 279, counter);

        printTestOutput("Separating IDs - separateIDs(): ", 2);
        printTestCase("Ensuring that 279 with a max ID of 7 maps back to {4,2,7}", list.equals(Automaton.separateIDs(279, 7)), counter);

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
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 1, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
        printTestCase("Ensuring that the added event is observable", a.getEvent(id).isObservable()[0], counter);
    	printTestCase("Ensuring that the added event is controllable", a.getEvent(id).isControllable()[0], counter);

    	printTestOutput("Adding an event that is observable, but not controllable...", 3);
    	id = a.addEvent("secondEvent", new boolean[] { true }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 2, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that the added event is observable", a.getEvent(id).isObservable()[0], counter);
        printTestCase("Ensuring that the added event is not controllable", !a.getEvent(id).isControllable()[0], counter);

    	printTestOutput("Adding an event that is controllable, but not observable...", 3);
    	id = a.addEvent("thirdEvent", new boolean[] { false }, new boolean[] { true });
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 3, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that the added event is not observable", !a.getEvent(id).isObservable()[0], counter);
        printTestCase("Ensuring that the added event is controllable", a.getEvent(id).isControllable()[0], counter);

    	printTestOutput("Adding an event that neither controllable, nor observable...", 3);
    	id = a.addEvent("fourthEvent", new boolean[] { false }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was expanded", a.getEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that the added event is not observable", !a.getEvent(id).isObservable()[0], counter);
        printTestCase("Ensuring that the added event is not controllable", !a.getEvent(id).isControllable()[0], counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	id = a.addEvent("fourthEvent", new boolean[] { false }, new boolean[] { false });
    	printTestCase("Ensuring that 'events' set was not expanded", a.getEvents().size() == 4, counter);
    	printTestCase("Ensuring that 'activeEvents' set was not expanded", a.getActiveEvents().size() == 0, counter);
    	printTestCase("Ensuring that the event was not successfully added", id == 0, counter);

        a.closeFiles();
    	
        	/* Event ID Assignment Tests */

    	printTestOutput("EVENT ID ASSIGNMENTS: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	a = new Automaton();

    	printTestOutput("Adding an event...", 3);
    	id = a.addEvent("firstEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the event's ID is 1", id == 1, counter);

    	printTestOutput("Adding a second event...", 3);
    	id = a.addEvent("secondEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the event's ID is 2", id == 2, counter);

    	printTestOutput("Adding a pre-existing event...", 3);
    	id = a.addEvent("firstEvent", new boolean[] { true }, new boolean[] { true });
    	printTestCase("Ensuring that the method returned 0", id == 0, counter);

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
    	printTestCase("Ensuring that 'nStates' was incremented", a.getNumberOfStates() == 1, counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that the added state exists", a.stateExists(id), counter);
    	printTestCase("Ensuring that the added state was not labeled the initial state", a.getInitialStateID() == 0, counter);
    	printTestCase("Ensuring that the added state has the proper label", a.getState(id).getLabel().equals("firstState"), counter);
    	printTestCase("Ensuring that the added state is marked", a.getState(id).isMarked(), counter);

    	printTestOutput("Adding an initial state that is unmarked...", 3);
    	id = a.addState("secondState", false, true);
    	printTestCase("Ensuring that 'nStates' was incremented", a.getNumberOfStates() == 2, counter);
    	printTestCase("Ensuring that 'stateCapacity' was not increased", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that the added state exists", a.stateExists(id), counter);
    	printTestCase("Ensuring that the added state was labeled the initial state", a.getInitialStateID() == id, counter);
    	printTestCase("Ensuring that the added state has the proper label", a.getState(id).getLabel().equals("secondState"), counter);
    	printTestCase("Ensuring that the added state is unmarked", !a.getState(id).isMarked(), counter);

        a.closeFiles();
    	
    		/* State ID Assignment Tests */

    	printTestOutput("STATE ID ASSIGNMENTS: ", 2);

    	printTestOutput("Instantiating empty automaton...", 3);
    	a = new Automaton();

    	printTestOutput("Adding a state...", 3);
    	id = a.addState("firstState", true, true);
    	printTestCase("Ensuring that the state's ID is 1", id == 1, counter);

    	printTestOutput("Adding a second state...", 3);
        id = a.addState("secondState", true, true);
        printTestCase("Ensuring that the state's ID is 2", id == 2, counter);

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
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", a.getTransitionCapacity() == 1, counter);
        printTestCase("Ensuring that 'labelLength' was reset to '1'", a.getLabelLength() == 1, counter);
    	printTestCase("Ensuring that 'nControllers' was reset to '1'", a.getNumberOfControllers() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", a.getSizeOfState() == 4, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: -1, Transition capacity: -1, Label length: -1, Number of controllers: -1)...", 3);
    	a = new Automaton(-1, -1, -1, -1, true);
    	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", a.getTransitionCapacity() == 1, counter);
    	printTestCase("Ensuring that 'labelLength' was reset to '1'", a.getLabelLength() == 1, counter);
        printTestCase("Ensuring that 'nControllers' was reset to '1'", a.getNumberOfControllers() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", a.getSizeOfState() == 4, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: 255, Transition capacity: 2, Label length: 1, Number of controllers: 1)...", 3);
    	a = new Automaton(255, 2, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was left at '255'", a.getStateCapacity() == 255, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '2'", a.getTransitionCapacity() == 2, counter);
    	printTestCase("Ensuring that 'labelLength' was left at '1'", a.getLabelLength() == 1, counter);
        printTestCase("Ensuring that 'nControllers' was left at '1'", a.getNumberOfControllers() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", a.getSizeOfStateID() == 1, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '6'", a.getSizeOfState() == 6, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: 256, Transition capacity: 1, Label length: Automaton.MAX_LABEL_LENGTH, Number of controllers: Automaton.MAX_NUMBER_OF_CONTROLLERS)...", 3);
    	a = new Automaton(256, 1, Automaton.MAX_LABEL_LENGTH, Automaton.MAX_NUMBER_OF_CONTROLLERS, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to '65535'", a.getStateCapacity() == 65535, counter);
    	printTestCase("Ensuring that 'transitionCapacity' was left at '1'", a.getTransitionCapacity() == 1, counter);
    	printTestCase("Ensuring that 'labelLength' was left at 'Automaton.MAX_LABEL_LENGTH'", a.getLabelLength() == Automaton.MAX_LABEL_LENGTH, counter);
        printTestCase("Ensuring that 'nControllers' was left at 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", a.getNumberOfControllers() == Automaton.MAX_NUMBER_OF_CONTROLLERS, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '2'", a.getSizeOfStateID() == 2, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '104'", a.getSizeOfState() == 104, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE, Transition capacity: Integer.MAX_VALUE, Label length: Automaton.MAX_LABEL_LENGTH + 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE, Integer.MAX_VALUE, Automaton.MAX_LABEL_LENGTH + 1, Automaton.MAX_NUMBER_OF_CONTROLLERS + 1, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE'", a.getTransitionCapacity() == Integer.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'labelLength' was reduced to 'Automaton.MAX_LABEL_LENGTH'", a.getLabelLength() == Automaton.MAX_LABEL_LENGTH, counter);
        printTestCase("Ensuring that 'nControllers' was reduced to 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", a.getNumberOfControllers() == Automaton.MAX_NUMBER_OF_CONTROLLERS, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", a.getSizeOfStateID() == 8, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '101 + 9 * Integer.MAX_VALUE'", a.getSizeOfState() == 101 + 9 * (long) Integer.MAX_VALUE, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE - 1, Transition capacity: Integer.MAX_VALUE - 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE - 1, Integer.MAX_VALUE - 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);
    	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE - 1'", a.getTransitionCapacity() == Integer.MAX_VALUE - 1, counter);
    	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", a.getSizeOfStateID() == 8, counter);
    	printTestCase("Ensuring that 'nBytesPerState' was initialized to '2 + 9 * (Integer.MAX_VALUE - 1)'", a.getSizeOfState() == 2 + 9 * (long) (Integer.MAX_VALUE - 1), counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: (Long.MAX_VALUE >> 7) + 1, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton((Long.MAX_VALUE >> 7) + 1, 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", a.getStateCapacity() == Long.MAX_VALUE, counter);
        a.closeFiles();

    	printTestOutput("Instantiating empty automaton (State capacity: Long.MAX_VALUE >> 7, Transition capacity: 1, Label length: 1)...", 3);
    	a = new Automaton(Long.MAX_VALUE >> 7, 1, 1, 1, true);
    	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE >> 7'", a.getStateCapacity() == Long.MAX_VALUE >> 7, counter);
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
        Automaton a = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
                "e,T\nf,F", // States   
                "e,a,f\nf,b,e", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                null // Use temporary files to store automaton
            );
        a.generateInputForGUI();
        printTestCase("Ensuring the event input was saved and loaded correctly", a.getEventInput().equals("a,T,T\nb,T,F\nc,F,T\nd,F,F"), counter);
        printTestCase("Ensuring the state input was saved and loaded correctly", a.getStateInput().equals("e,T\nf,F"), counter);
        printTestCase("Ensuring the transition input was saved and loaded correctly", a.getTransitionInput().equals("e,a,f\nf,b,e"), counter);
        a.closeFiles();

        printTestOutput("Instantiating automaton from GUI input code with duplicate labels, omitted optional parameters, and an initial state...", 3);
        a = AutomataGUI.generateAutomaton(
                "a\nb,F,F\na,F,F\nb", // Events
                "*c\nc,F", // States    
                "", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                null // Use temporary files to store automaton
            );
        a.generateInputForGUI();
        printTestCase("Ensuring the event input was saved and loaded correctly", a.getEventInput().equals("a,T,T\nb,F,F"), counter);
        printTestCase("Ensuring the state input was saved and loaded correctly", a.getStateInput().equals("*c,T"), counter);
        printTestCase("Ensuring the transition input was saved and loaded correctly", a.getTransitionInput().equals(""), counter);
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
        Automaton fig2_12 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "*zero,F\none,F\ntwo,T\nthree,F\nfour,F\nfive,F\nsix,F", // States 
                "zero,a,one\none,a,three\none,b,two\none,g,five\ntwo,g,zero\nthree,b,four\nfour,g,four\nfour,a,three\nsix,a,three\nsix,b,two", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_12.hdr")
            );

        printTestOutput("Taking the co-accessible part of Figure 2.12 (and comparing the result to the automaton in Figure 2.13a)...", 3);
        Automaton result = fig2_12.coaccessible();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*zero,F\none,F\ntwo,T\nsix,F"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("zero,a,one\none,b,two\ntwo,g,zero\nsix,b,two"), counter);

            /* Trim Operation Tests */

        printTestOutput("TRIM OPERATION: ", 2);

        printTestOutput("Trimming the automaton in Figure 2.12 (and comparing the result to the automaton in Figure 2.13b)...", 3);
        result = fig2_12.trim();

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*zero,F\none,F\ntwo,T"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("zero,a,one\none,b,two\ntwo,g,zero"), counter);

            /* Intersection Operation Tests */

        printTestOutput("INTERSECTION OPERATION: ", 2);

        printTestOutput("Instantiating automaton from Figure 2.1...", 3);
        Automaton fig2_1 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\ng,T,T", // Events
                "*x,T\ny,F\nz,T", // States 
                "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_1.hdr")
            );
        printTestOutput("Instantiating automaton from Figure 2.2...", 3);
        Automaton fig2_2 = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T", // Events
                "*zero,F\none,T", // States 
                "zero,b,zero\nzero,a,one\none,a,one\none,b,zero", // Transitions
                1, // Number of controllers
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
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_13b.hdr")
            );

        printTestOutput("Taking the intersection of Figure 2.2 and Figure 2.13(b) (and comparing the result to the second automaton in Figure 2.15)...", 3);
        result = Automaton.intersection(fig2_2, fig2_13b);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*zero_zero,F\none_one,F\nzero_two,F"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("zero_zero,a,one_one\none_one,b,zero_two"), counter);

        printTestOutput("Instantiating the first automaton from Figure 2.20...", 3);
        Automaton fig2_20a = AutomataGUI.generateAutomaton(
                "a1\na2\nb\nr", // Events
                "*x1,F\nx2,F\nx3,T", // States 
                "x1,a1,x2\nx1,a2,x2\nx2,b,x3\nx3,r,x1", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_20a.hdr")
            );

        printTestOutput("Instantiating the second automaton from Figure 2.20...", 3);
        Automaton fig2_20b = AutomataGUI.generateAutomaton(
                "a1\nb\nc1\nr\na2\nc2", // Events
                "*y1,F\ny2,F\ny3,F\ny4,F\ny5,F\ny6,F", // States 
                "y1,a1,y2\ny2,b,y4\ny4,r,y1\ny4,c1,y6\ny6,r,y1\ny1,a2,y3\ny3,b,y5\ny5,c2,y6\ny5,r,y1", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_20b.hdr")
            );

        printTestOutput("Taking the intersection of the first two automata in Figure 2.20 (and comparing the result to the third automaton in Figure 2.20)...", 3);
        result = Automaton.intersection(fig2_20a, fig2_20b);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a1,T,T\na2,T,T\nb,T,T\nr,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*x1_y1,F\nx2_y2,F\nx2_y3,F\nx3_y4,F\nx3_y5,F"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("x1_y1,a1,x2_y2\nx1_y1,a2,x2_y3\nx2_y2,b,x3_y4\nx2_y3,b,x3_y5\nx3_y4,r,x1_y1\nx3_y5,r,x1_y1"), counter);

            /* Union Operation Tests */

        printTestOutput("UNION OPERATION: ", 2);

        printTestOutput("Taking the union of Figure 2.1 and Figure 2.2 (and comparing the result to the automaton in Figure 2.16)...", 3);
        result = Automaton.union(fig2_1, fig2_2);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\ng,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*x_zero,F\ny_zero,F\nz_zero,F\nx_one,T\ny_one,F\nz_one,T"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("x_zero,a,x_one\nx_zero,g,z_zero\ny_zero,b,y_zero\n"
            + "y_zero,a,x_one\nz_zero,b,z_zero\nz_zero,a,y_one\nz_zero,g,y_zero\nx_one,a,x_one\nx_one,g,z_one\ny_one,b,y_zero\ny_one,a,x_one\nz_one,b,z_zero\n"
            + "z_one,a,y_one\nz_one,g,y_one"), counter);

        printTestOutput("Instantiating the first automaton from Figure 2.17...", 3);
        Automaton fig2_17a = AutomataGUI.generateAutomaton(
                "a,T,T\nb,T,T\nc,T,T", // Events
                "*one,T\ntwo,F", // States 
                "one,c,one\none,a,two\ntwo,b,two", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17a.hdr")
            );

        printTestOutput("Instantiating the second automaton from Figure 2.17...", 3);
        Automaton fig2_17b = AutomataGUI.generateAutomaton(
                "b,T,T\na,T,T\nd,T,T", // Events
                "*A,T\nB,F", // States 
                "A,b,A\nA,a,B\nB,d,B", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17b.hdr")
            );

        printTestOutput("Instantiating the third automaton from Figure 2.17...", 3);
        Automaton fig2_17c = AutomataGUI.generateAutomaton(
                "c,T,T\nb,T,T\na,T,T", // Events
                "*D,T\nE,F", // States 
                "D,c,D\nD,b,E\nE,a,E", // Transitions
                1, // Number of controllers
                false, // We do not want it to be verbose
                new File("fig2_17c.hdr")
            );

        printTestOutput("Taking the union of the three automata in Figure 2.17 (and comparing the result to the automaton described in Example 2.17)...", 3);
        result = Automaton.union(Automaton.union(fig2_17a, fig2_17b), fig2_17c);

        result.generateInputForGUI();
        printTestCase("Ensuring the events are correct", result.getEventInput().equals("a,T,T\nb,T,T\nc,T,T\nd,T,T"), counter);
        printTestCase("Ensuring the states are correct", result.getStateInput().equals("*one_A_D,T"), counter);
        printTestCase("Ensuring the transitions are correct", result.getTransitionInput().equals("one_A_D,c,one_A_D"), counter);

            /* Print summary of this test routine */

        printTestRoutineSummary(testRoutineName, counter);

        return counter;

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
	    	System.out.println(str + ": " + (passed ? GREEN + "PASSED" + RESET : RED + "*** FAILED ***" + RESET));

	    }

    }

    /**
     * Helper method to print the results of a test routine.
     * @param testRoutineName - The test routine's name is used as part of the output.
     * @param counter - The counter contains the information about how many test cases passed and failed
     **/
    private static void printTestRoutineSummary(String testRoutineName, TestCounter counter) {

    	String passed = (counter.getPassedTests() > 0) ? String.format("\n\t\t%sPASSED: %d/%d%s", GREEN, counter.getPassedTests(), counter.getTotalTests(), RESET) : "";
    	String failed = (counter.getFailedTests() > 0) ? String.format("\n\t\t%s*** FAILED: %d/%d% ***s", RED, counter.getFailedTests(), counter.getTotalTests(), RESET) : "";

    	printTestOutput(testRoutineName + " TEST ROUTINE SUMMARY:" + passed + failed, 1);

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