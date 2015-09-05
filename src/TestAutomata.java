import java.util.*;
import java.io.*;

public class TestAutomata {

  // Colored output makes it more readable (doesn't work on all operating systems)
  // Example: System.out.println("The following will be purple: " + PURPLE + "Purple!" + RESET + " Normal color");
  public static String RESET = "\u001B[0m";
  public static String RED = "\u001B[31m";
  public static String GREEN = "\u001B[32m";
  public static String PURPLE = "\u001B[35m";

  // Verbose levels
	private static final int MAX_VERBOSE = 3;
	private static int verbose = 1;

  // Whether or not to compare lines by printing out the missing lines and added lines  (as opposed to actual vs expected)
  public static boolean DIFF = false;
  
  public static void main(String[] args) {

  		/* Turn verbose on if '-v' flag was used, turn on colored output if '-c' flag was used, enable 'diff'-like output if '-d' flag is used */

    boolean coloredOutput = false;

    for (String arg : args)
  		if (arg.length() >=2) {
        if (arg.substring(0, 2).equals("-v"))
          verbose = MAX_VERBOSE;
        else if (arg.substring(0, 2).equals("-c"))
          coloredOutput = true;
        else if (arg.substring(0, 2).equals("-d"))
          DIFF = true;
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

    counter.add(runByteManipulatorTestRoutine());
    counter.add(runHelperMethodTestRoutine());
    counter.add(runEventCreationTestRoutine());
    counter.add(runStateCreationTestRoutine());
    counter.add(runAutomatonCapacityTestRoutine());
    counter.add(runGuiInputTestRoutine());
    counter.add(runAutomataStandardOperationsTestRoutine());
    counter.add(runAutomataSpecialOperationsTestRoutine());
    counter.add(runSpecialTransitionsTestRoutine());
    counter.add(runAutomataPropertiesTestRoutine());
  	counter.add(runExceptionHandlingTestRoutine());

  		/* Print summary of all tests */

  	if (counter.getFailedTests() > 0)
  		System.out.println(String.format("\n%s*** FAILED %d/%d TESTS ***%s", RED, counter.getFailedTests(), counter.getTotalTests(), RESET));
    else
      System.out.println(String.format("\n%sPASSED ALL %d TESTS%s", GREEN, counter.getTotalTests(), RESET));

  }

  private static TestCounter runByteManipulatorTestRoutine() {

    String testRoutineName = "BYTE MANIPULATOR";

    printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    TestCounter counter = new TestCounter();

    printTestOutput("Ensuring that values being written can be read again: ", 2);

    boolean passed = true;
    for (int i = 0; i <= 255; i++) {
      byte[] arr = new byte[1];
      ByteManipulator.writeLongAsBytes(arr, 0, i, 1);
      if (ByteManipulator.readBytesAsLong(arr, 0, 1) != i) {
        passed = false;
        break;
      }
    }
    printTestCase("Ensuring that one byte values were written and read properly", new TestResult(passed), counter);

    passed = true;
    for (int i = 256; i <= 65535; i++) {
      byte[] arr = new byte[2];
      ByteManipulator.writeLongAsBytes(arr, 0, i, 2);
      if (ByteManipulator.readBytesAsLong(arr, 0, 2) != i) {
        passed = false;
        break;
      }
    }
    printTestCase("Ensuring that two byte values were written and read properly", new TestResult(passed), counter);
   
      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static TestCounter runHelperMethodTestRoutine() {

    String testRoutineName = "HELPER METHOD";

    printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    TestCounter counter = new TestCounter();

      /* findCounterExample() Tests */

    printTestOutput("Liu's Thesis - findCounterExample(): ", 2);
    
    printTestOutput("Instantiating an automaton...", 3);
    Automaton automaton = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));

    printTestOutput("Taking the U-Structure of the automaton...", 3);
    UStructure uStructure = saveAndLoadUStructure(automaton.synchronizedComposition(null, null));
    
    printTestOutput("Finding the counter-example...", 3);
    List<List<String>> labelSequences = uStructure.findCounterExample(true);
    printTestCase("Ensuring that the 0th sequence is correct", new TestResult(labelSequences.get(0), new ArrayList<String>() {{ add("b"); add("a"); add("o"); }} ), counter);
    printTestCase("Ensuring that the 1st sequence is correct", new TestResult(labelSequences.get(1), new ArrayList<String>() {{ add("a"); add("b"); add("o"); }} ), counter);
    printTestCase("Ensuring that the 2nd sequence is correct", new TestResult(labelSequences.get(2), new ArrayList<String>() {{ add("a"); add("b"); add("o"); }} ), counter);

      /* acceptsCounterExample() Tests */

    printTestOutput("Liu's Thesis - acceptsCounterExample(): ", 2);
    
    printTestCase("Ensuring that the original automaton can accept the counter-example", new TestResult(automaton.acceptsCounterExample(labelSequences), -1), counter);
    labelSequences.add(new ArrayList<String>() {{ add("b"); add("o"); add("o"); }});
    printTestOutput("Adding a bad sequence to the list...", 3);
    printTestCase("Ensuring that the original automaton can no longer accept the counter-example", new TestResult(automaton.acceptsCounterExample(labelSequences), 2), counter);

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

      /* isValidLabel() Tests */

    printTestOutput("GUI Parsing - isValidLabel(): ", 2);
    
    printTestCase("Ensuring that a label with a bad vector is considered invalid", new TestResult(AutomatonGenerator.isValidLabel("<a,b"), false), counter);
    printTestCase("Ensuring that a label with a bad vector is considered invalid", new TestResult(AutomatonGenerator.isValidLabel("a,b>"), false), counter);
    printTestCase("Ensuring that a label with a good vector is considered valid", new TestResult(AutomatonGenerator.isValidLabel("<a,b>"), true), counter);

      /* createCombinedIDWithOrderedSet() Tests */

    printTestOutput("Combining IDs - combineIDs(): ", 2);
    
    ArrayList<Long> list = new ArrayList<Long>();
    list.add(4L);
    list.add(2L);
    list.add(7L);
    printTestCase("Ensuring that {4,2,7} with a max ID of 7 maps to 279", new TestResult(Automaton.combineIDs(list, 7), 279), counter);

    printTestOutput("Separating IDs - separateIDs(): ", 2);
    printTestCase("Ensuring that 279 with a max ID of 7 maps back to {4,2,7}", new TestResult(list, Automaton.separateIDs(279, 7)), counter);

      /* splitStringWithVectors() Tests */

    printTestOutput("Splitting Strings that contain Vectors - splitStringWithVectors(): ", 2);
    
    printTestCase("Splitting a string with no vectors", new TestResult(AutomatonGenerator.splitStringWithVectors("One,Two"), new String[]{"One", "Two"}), counter);
    printTestCase("Splitting a string containing vectors", new TestResult(AutomatonGenerator.splitStringWithVectors("<A,B>,C,<D,E,F>"), new String[]{"<A,B>", "C", "<D,E,F>"}), counter);
    printTestCase("Splitting a string with mismatched angled brackets (expecting null)", new TestResult(AutomatonGenerator.splitStringWithVectors("<A,B>,C>") == null), counter);

      /* Pareto Helper Method Tests */

    printTestOutput("Pareto Ranks - getParetoRanks(): ", 2);

    int[] x = {1, 2, 3, 3, 5, 5, 7, 8};
    int[] y = {2, 6, 2, 7, 5, 2, 4, 1};
    ArrayList<Integer> expectedIndexes = new ArrayList<Integer>();
    expectedIndexes.add(4);
    expectedIndexes.add(2);
    expectedIndexes.add(3);
    expectedIndexes.add(1);
    expectedIndexes.add(1);
    expectedIndexes.add(2);
    expectedIndexes.add(1);
    expectedIndexes.add(1);
    printTestCase("Ensuring that the Pareto ranks could be generated", new TestResult(new ArrayList<Integer>(Arrays.asList(getParetoRanks(x, y))), expectedIndexes), counter);


      /* combineCommunicationCosts() Tests */

    printTestOutput("Combining Communication Costs - combineCommunicationCosts(): ", 2);

    // NOTE: These tests do not check each individual cost to make sure it is correct,
    //       but instead, the costs are simply added together and compared to the expected sum.

    uStructure = saveAndLoadUStructure(AutomatonGenerator.generateFromGUICode(
      new UStructure(null, null, 2),
      "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF", // Events
      "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7", // States
      "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:NASH_COMMUNICATION-RS-0.125-0.125\n1_1_1,<a,a,a>,2_2_2:NASH_COMMUNICATION-SR-0.125-0.125\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:NASH_COMMUNICATION-RS-0.125-0.125\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:NASH_COMMUNICATION-SR-0.625-0.125\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:NASH_COMMUNICATION-RS-0.125-0.125\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:NASH_COMMUNICATION-RS-0.125-0.125\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:NASH_COMMUNICATION-SR-0.625-0.125\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:NASH_COMMUNICATION-SR-0.125-0.125\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6\n5_5_5,<o,o,o>,7_7_7" // Transitions
    ));
    List<Set<NashCommunicationData>> feasibleProtocols = uStructure.generateAllFeasibleProtocols(uStructure.getNashCommunications(), true);

    printTestOutput("Using protocol with four <a,a,a> event vectors...", 3);
    Set<NashCommunicationData> desiredProtocol = null;
    outer: for (Set<NashCommunicationData> protocol : feasibleProtocols) {
      for (NashCommunicationData communication : protocol)
        if (!uStructure.getEvent(communication.eventID).getLabel().equals("<a,a,a>"))
          continue outer;
      desiredProtocol = protocol;
      break;
    }
    
    double total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.MAX);
    printTestCase("Ensuring that the communication costs were correct when taking the max", new TestResult(total, 2.5), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.AVERAGE);
    printTestCase("Ensuring that the communication costs were correct when averaging", new TestResult(total, 1.5), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.SUM);
    printTestCase("Ensuring that the communication costs were correct when taking the sum", new TestResult(total, 6.0), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.UNIT);
    printTestCase("Ensuring that the communication costs were correct when using unit costs", new TestResult(total, 1.5), counter);

    printTestOutput("Using protocol with two <a,a,a> and two <b,b,b> event vectors...", 3);
    desiredProtocol = null;
    outer: for (Set<NashCommunicationData> protocol : feasibleProtocols) {
      int aaa = 0, bbb = 0;
      for (NashCommunicationData communication : protocol)
        if (uStructure.getEvent(communication.eventID).getLabel().equals("<a,a,a>"))
          aaa++;
        else if (uStructure.getEvent(communication.eventID).getLabel().equals("<b,b,b>"))
          bbb++;
      if (aaa == 2 && bbb == 2) {
        desiredProtocol = protocol;
        break;
      }
    }

    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.MAX);
    printTestCase("Ensuring that the communication costs were correct when taking the max", new TestResult(total, 0.5), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.AVERAGE);
    printTestCase("Ensuring that the communication costs were correct when averaging", new TestResult(total, 0.5), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.SUM);
    printTestCase("Ensuring that the communication costs were correct when taking the sum", new TestResult(total, 0.5), counter);
    total = combineCommunicationCostsHelper(uStructure, desiredProtocol, Crush.CombiningCosts.UNIT);
    printTestCase("Ensuring that the communication costs were correct when using unit costs", new TestResult(total, 0.5), counter);

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static double combineCommunicationCostsHelper(UStructure uStructure,
                                                     Set<NashCommunicationData> protocol,
                                                     Crush.CombiningCosts combiningCostsMethod) {   

    if (protocol == null)
      return 0;

    double total = 0;

    // Create copy in order to preserve original protocol
    Set<NashCommunicationData> copy = new HashSet<NashCommunicationData>();
    for (NashCommunicationData communication : protocol)
      copy.add((NashCommunicationData) communication.clone());
    
    // Combine costs, then calculate the total
    uStructure.combineCommunicationCosts(copy, combiningCostsMethod);
    for (NashCommunicationData communication : copy)
      total += communication.cost;
    
    return total;

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
  	int id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
  	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 1), counter);
    printTestCase("Ensuring that the added event is observable", new TestResult(a.getEvent(id).isObservable()[0], true), counter);
  	printTestCase("Ensuring that the added event is controllable", new TestResult(a.getEvent(id).isControllable()[0], true), counter);

  	printTestOutput("Adding an event that is observable, but not controllable...", 3);
  	id = a.addEventIfNonExisting("secondEvent", new boolean[] { true }, new boolean[] { false });
  	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 2), counter);
  	printTestCase("Ensuring that the added event is observable", new TestResult(a.getEvent(id).isObservable()[0], true), counter);
    printTestCase("Ensuring that the added event is not controllable", new TestResult(a.getEvent(id).isControllable()[0], false), counter);

  	printTestOutput("Adding an event that is controllable, but not observable...", 3);
  	id = a.addEventIfNonExisting("thirdEvent", new boolean[] { false }, new boolean[] { true });
  	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 3), counter);
  	printTestCase("Ensuring that the added event is not observable", new TestResult(a.getEvent(id).isObservable()[0], false), counter);
    printTestCase("Ensuring that the added event is controllable", new TestResult(a.getEvent(id).isControllable()[0], true), counter);

  	printTestOutput("Adding an event that neither controllable, nor observable...", 3);
  	id = a.addEventIfNonExisting("fourthEvent", new boolean[] { false }, new boolean[] { false });
  	printTestCase("Ensuring that 'events' set was expanded", new TestResult(a.getEvents().size(), 4), counter);
  	printTestCase("Ensuring that the added event is not observable", new TestResult(a.getEvent(id).isObservable()[0], false), counter);
    printTestCase("Ensuring that the added event is not controllable", new TestResult(a.getEvent(id).isControllable()[0], false), counter);

  	printTestOutput("Adding a pre-existing event...", 3);
  	id = a.addEventIfNonExisting("fourthEvent", new boolean[] { false }, new boolean[] { false });
  	printTestCase("Ensuring that 'events' set was not expanded", new TestResult(a.getEvents().size(), 4), counter);
  	printTestCase("Ensuring that the method returned proper negative value", new TestResult(id, -4), counter);

    	/* Event ID Assignment Tests */

  	printTestOutput("EVENT ID ASSIGNMENTS: ", 2);

  	printTestOutput("Instantiating empty automaton...", 3);
  	a = new Automaton();

  	printTestOutput("Adding an event...", 3);
  	id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
  	printTestCase("Ensuring that the event's ID is 1", new TestResult(id, 1), counter);

  	printTestOutput("Adding a second event...", 3);
  	id = a.addEventIfNonExisting("secondEvent", new boolean[] { true }, new boolean[] { true });
  	printTestCase("Ensuring that the event's ID is 2", new TestResult(id, 2), counter);

  	printTestOutput("Adding a pre-existing event...", 3);
  	id = a.addEventIfNonExisting("firstEvent", new boolean[] { true }, new boolean[] { true });
  	printTestCase("Ensuring that the method returned proper negative value", new TestResult(id, -1), counter);


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
  	Automaton automaton = new Automaton();

  	printTestOutput("Adding a state that is marked...", 3);
  	long id = automaton.addState("firstState", true, false);
  	printTestCase("Ensuring that 'nStates' was incremented", new TestResult(automaton.getNumberOfStates(), 1), counter);
  	printTestCase("Ensuring that 'stateCapacity' was not increased", new TestResult(automaton.getStateCapacity(), 255), counter);
  	printTestCase("Ensuring that the added state exists", new TestResult(automaton.stateExists(id), true), counter);
  	printTestCase("Ensuring that the added state was not labeled the initial state", new TestResult(automaton.getInitialStateID(), 0), counter);
  	printTestCase("Ensuring that the added state has the proper label", new TestResult(automaton.getState(id).getLabel(), "firstState"), counter);
  	printTestCase("Ensuring that the added state is marked", new TestResult(automaton.getState(id).isMarked(), true), counter);

  	printTestOutput("Adding an initial state that is unmarked...", 3);
  	id = automaton.addState("secondState", false, true);
  	printTestCase("Ensuring that 'nStates' was incremented", new TestResult(automaton.getNumberOfStates(), 2), counter);
  	printTestCase("Ensuring that 'stateCapacity' was not increased", new TestResult(automaton.getStateCapacity(), 255), counter);
  	printTestCase("Ensuring that the added state exists", new TestResult(automaton.stateExists(id), true), counter);
  	printTestCase("Ensuring that the added state was labeled the initial state", new TestResult(automaton.getInitialStateID(), id), counter);
  	printTestCase("Ensuring that the added state has the proper label", new TestResult(automaton.getState(id).getLabel(), "secondState"), counter);
  	printTestCase("Ensuring that the added state is unmarked", new TestResult(automaton.getState(id).isMarked(), false), counter);
  	
  		/* State ID Assignment Tests */

  	printTestOutput("STATE ID ASSIGNMENTS: ", 2);

  	printTestOutput("Instantiating empty automaton...", 3);
  	automaton = new Automaton();

  	printTestOutput("Adding a state...", 3);
  	id = automaton.addState("firstState", true, true);
  	printTestCase("Ensuring that the state's ID is 1", new TestResult(id, 1), counter);

  	printTestOutput("Adding a second state...", 3);
    id = automaton.addState("secondState", true, true);
    printTestCase("Ensuring that the state's ID is 2", new TestResult(id, 2), counter);

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

  	printTestOutput("Instantiating empty automaton (Event capacity: 0, State capacity: 0, Transition capacity: 0, Label length: 0, Number of controllers: 0)...", 3);
  	Automaton automaton = saveAndLoadAutomaton(new Automaton(0, 0, 0, 0, 0, true));
    printTestCase("Ensuring that 'eventCapacity' was reset to '255'", new TestResult(automaton.getEventCapacity(), 255), counter);
  	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", new TestResult(automaton.getStateCapacity(), 255), counter);
  	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", new TestResult(automaton.getTransitionCapacity(), 1), counter);
    printTestCase("Ensuring that 'labelLength' was reset to '1'", new TestResult(automaton.getLabelLength(), 1), counter);
  	printTestCase("Ensuring that 'nControllers' was reset to '1'", new TestResult(automaton.getNumberOfControllers(), 1), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '1'", new TestResult(automaton.getSizeOfEventID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(automaton.getSizeOfStateID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", new TestResult(automaton.getSizeOfState(), 4), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: -1, State capacity: -1, Transition capacity: -1, Label length: -1, Number of controllers: -1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(-1, -1, -1, -1, -1, true));
    printTestCase("Ensuring that 'eventCapacity' was reset to '255'", new TestResult(automaton.getEventCapacity(), 255), counter);
  	printTestCase("Ensuring that 'stateCapacity' was reset to '255'", new TestResult(automaton.getStateCapacity(), 255), counter);
  	printTestCase("Ensuring that 'transitionCapacity' was reset to '1'", new TestResult(automaton.getTransitionCapacity(), 1), counter);
  	printTestCase("Ensuring that 'labelLength' was reset to '1'", new TestResult(automaton.getLabelLength(), 1), counter);
    printTestCase("Ensuring that 'nControllers' was reset to '1'", new TestResult(automaton.getNumberOfControllers(), 1), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '1'", new TestResult(automaton.getSizeOfEventID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(automaton.getSizeOfStateID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '4'", new TestResult(automaton.getSizeOfState(), 4), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: 255, State capacity: 255, Transition capacity: 2, Label length: 1, Number of controllers: 1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(255, 255, 2, 1, 1, true));
    printTestCase("Ensuring that 'eventCapacity' was left at '255'", new TestResult(automaton.getEventCapacity(), 255), counter);
  	printTestCase("Ensuring that 'stateCapacity' was left at '255'", new TestResult(automaton.getStateCapacity(), 255), counter);
  	printTestCase("Ensuring that 'transitionCapacity' was left at '2'", new TestResult(automaton.getTransitionCapacity(), 2), counter);
  	printTestCase("Ensuring that 'labelLength' was left at '1'", new TestResult(automaton.getLabelLength(), 1), counter);
    printTestCase("Ensuring that 'nControllers' was left at '1'", new TestResult(automaton.getNumberOfControllers(), 1), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '1'", new TestResult(automaton.getSizeOfEventID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '1'", new TestResult(automaton.getSizeOfStateID(), 1), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '6'", new TestResult(automaton.getSizeOfState(), 6), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: 256, State capacity: 256, Transition capacity: 1, Label length: Automaton.MAX_LABEL_LENGTH, Number of controllers: Automaton.MAX_NUMBER_OF_CONTROLLERS)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(256, 256, 1, Automaton.MAX_LABEL_LENGTH, Automaton.MAX_NUMBER_OF_CONTROLLERS, true));
    printTestCase("Ensuring that 'eventCapacity' was increased to '65535'", new TestResult(automaton.getEventCapacity(), 65535), counter);
  	printTestCase("Ensuring that 'stateCapacity' was increased to '65535'", new TestResult(automaton.getStateCapacity(), 65535), counter);
  	printTestCase("Ensuring that 'transitionCapacity' was left at '1'", new TestResult(automaton.getTransitionCapacity(), 1), counter);
  	printTestCase("Ensuring that 'labelLength' was left at 'Automaton.MAX_LABEL_LENGTH'", new TestResult(automaton.getLabelLength(), Automaton.MAX_LABEL_LENGTH), counter);
    printTestCase("Ensuring that 'nControllers' was left at 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", new TestResult(automaton.getNumberOfControllers(), Automaton.MAX_NUMBER_OF_CONTROLLERS), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '2'", new TestResult(automaton.getSizeOfEventID(), 2), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '2'", new TestResult(automaton.getSizeOfStateID(), 2), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '100005'", new TestResult(automaton.getSizeOfState(), 100005), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: Integer.MAX_VALUE, State capacity: Long.MAX_VALUE, Transition capacity: Integer.MAX_VALUE, Label length: Automaton.MAX_LABEL_LENGTH + 1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(Integer.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Automaton.MAX_LABEL_LENGTH + 1, Automaton.MAX_NUMBER_OF_CONTROLLERS + 1, true));
    printTestCase("Ensuring that 'eventCapacity' remained at 'Integer.MAX_VALUE'", new TestResult(automaton.getEventCapacity(), Integer.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE'", new TestResult(automaton.getStateCapacity(), Long.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE'", new TestResult(automaton.getTransitionCapacity(), Integer.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'labelLength' was reduced to 'Automaton.MAX_LABEL_LENGTH'", new TestResult(automaton.getLabelLength(), Automaton.MAX_LABEL_LENGTH), counter);
    printTestCase("Ensuring that 'nControllers' was reduced to 'Automaton.MAX_NUMBER_OF_CONTROLLERS'", new TestResult(automaton.getNumberOfControllers(), Automaton.MAX_NUMBER_OF_CONTROLLERS), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '4'", new TestResult(automaton.getSizeOfEventID(), 4), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", new TestResult(automaton.getSizeOfStateID(), 8), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '100001 + 12 * Integer.MAX_VALUE'", new TestResult(automaton.getSizeOfState(), 100001 + 12 * (long) Integer.MAX_VALUE), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: Integer.MAX_VALUE - 1, State capacity: Long.MAX_VALUE - 1, Transition capacity: Integer.MAX_VALUE - 1, Label length: 1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(Integer.MAX_VALUE - 1, Long.MAX_VALUE - 1, Integer.MAX_VALUE - 1, 1, 1, true));
    printTestCase("Ensuring that 'eventCapacity' was increased to 'Integer.MAX_VALUE'", new TestResult(automaton.getEventCapacity(), Integer.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", new TestResult(automaton.getStateCapacity(), Long.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'transitionCapacity' remained at 'Integer.MAX_VALUE - 1'", new TestResult(automaton.getTransitionCapacity(), Integer.MAX_VALUE - 1), counter);
    printTestCase("Ensuring that 'nBytesPerEventID' was initialized to '4'", new TestResult(automaton.getSizeOfEventID(), 4), counter);
  	printTestCase("Ensuring that 'nBytesPerStateID' was initialized to '8'", new TestResult(automaton.getSizeOfStateID(), 8), counter);
  	printTestCase("Ensuring that 'nBytesPerState' was initialized to '2 + 12 * (Integer.MAX_VALUE - 1)'", new TestResult(automaton.getSizeOfState(), 2 + 12 * (long) (Integer.MAX_VALUE - 1)), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: (Integer.MAX_VALUE >> 7) + 1, State capacity: (Long.MAX_VALUE >> 7) + 1, Transition capacity: 1, Label length: 1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton((Integer.MAX_VALUE >> 7) + 1, (Long.MAX_VALUE >> 7) + 1, 1, 1, 1, true));
    printTestCase("Ensuring that 'eventCapacity' was increased to 'Integer.MAX_VALUE'", new TestResult(automaton.getEventCapacity(), Integer.MAX_VALUE), counter);
  	printTestCase("Ensuring that 'stateCapacity' was increased to 'Long.MAX_VALUE'", new TestResult(automaton.getStateCapacity(), Long.MAX_VALUE), counter);

  	printTestOutput("Instantiating empty automaton (Event capacity: Integer.MAX_VALUE >> 7, State capacity: Long.MAX_VALUE >> 7, Transition capacity: 1, Label length: 1)...", 3);
  	automaton = saveAndLoadAutomaton(new Automaton(Integer.MAX_VALUE >> 7, Long.MAX_VALUE >> 7, 1, 1, 1, true));
    printTestCase("Ensuring that 'eventCapacity' remained at 'Integer.MAX_VALUE >> 7'", new TestResult(automaton.getEventCapacity(), Integer.MAX_VALUE >> 7), counter);
  	printTestCase("Ensuring that 'stateCapacity' remained at 'Long.MAX_VALUE >> 7'", new TestResult(automaton.getStateCapacity(), Long.MAX_VALUE >> 7), counter);

    printTestOutput("AUTOMATON CAPACITY EXPANSION: ", 2);

    printTestOutput("Instantiating empty automaton...", 3);
    automaton = saveAndLoadAutomaton(new Automaton());
    
    printTestOutput("Adding 256 events to it...", 3);
    printTestCase("Ensuring that 'eventCapacity' was originally '255'", new TestResult(automaton.getEventCapacity(), 255), counter);
    boolean[] arbitraryArray = {true};
    for (int i = 0; i < 256; i++)
      automaton.addEvent(String.valueOf(i), arbitraryArray, arbitraryArray);
    automaton = saveAndLoadAutomaton(automaton);
    printTestCase("Ensuring that 'eventCapacity' was expanded to '65535'", new TestResult(automaton.getEventCapacity(), 65535), counter);
    
    printTestOutput("Adding 256 states to it...", 3);
    printTestCase("Ensuring that 'stateCapacity' was originally '255'", new TestResult(automaton.getStateCapacity(), 255), counter);
    printTestCase("Ensuring that 'labelLength' was originally '1'", new TestResult(automaton.getLabelLength(), 1), counter);
    for (long i = 0; i < 256; i++)
      automaton.addState(String.valueOf(i), i % 2 == 0, i == 135);
    automaton = saveAndLoadAutomaton(automaton);
    printTestCase("Ensuring that 'stateCapacity' was expanded to '65535'", new TestResult(automaton.getStateCapacity(), 65535), counter);
    printTestCase("Ensuring that 'labelLength' was expanded to '3'", new TestResult(automaton.getLabelLength(), 3), counter);
    
    printTestOutput("Adding transitions to it...", 3);
    printTestCase("Ensuring that 'transitionCapacity' was originally '1'", new TestResult(automaton.getTransitionCapacity(), 1), counter);
    for (int i = 0; i < 2; i++)
      automaton.addTransition(1, i + 1, 1);
    automaton = saveAndLoadAutomaton(automaton);
    printTestCase("Ensuring that 'transitionCapacity' was expanded to '2'", new TestResult(automaton.getTransitionCapacity(), 2), counter);
    
  		/* Print summary of this test routine */

  	printTestRoutineSummary(testRoutineName, counter);

  	return counter;

  }

  private static TestCounter runGuiInputTestRoutine() {

    String testRoutineName = "GUI INPUT";

    printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    TestCounter counter = new TestCounter();

      /* Basic GUI Input Tests */

    printTestOutput("BASIC GUI INPUT: ", 2);

    printTestOutput("Instantiating automaton from simple GUI input code...", 3);
    Automaton automaton = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(),
      "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
      "e,T\nf,F", // States  
      "e,a,f\nf,b,e" // Transitions
    ));
    automaton.generateInputForGUI();
    printTestCase("Ensuring the event input was saved and loaded correctly", new TestResult(automaton.getEventInput(), "a,T,T\nb,T,F\nc,F,T\nd,F,F"), counter);
    printTestCase("Ensuring the state input was saved and loaded correctly", new TestResult(automaton.getStateInput(), "e,T\nf,F"), counter);
    printTestCase("Ensuring the transition input was saved and loaded correctly", new TestResult(automaton.getTransitionInput(), "e,a,f\nf,b,e"), counter);

    printTestOutput("Instantiating automaton from GUI input code with duplicate labels, omitted optional parameters, and an initial state...", 3);
    automaton = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(),
      "a\nb,F,F\na,F,F\nb", // Events
      "@c\nc,F", // States  
      "" // Transitions
    ));
    automaton.generateInputForGUI();
    printTestCase("Ensuring the event input was saved and loaded correctly", new TestResult(automaton.getEventInput(), "a,T,T\nb,F,F"), counter);
    printTestCase("Ensuring the state input was saved and loaded correctly", new TestResult(automaton.getStateInput(), "@c,F"), counter);
    printTestCase("Ensuring the transition input was saved and loaded correctly", new TestResult(automaton.getTransitionInput(), ""), counter);

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static TestCounter runAutomataStandardOperationsTestRoutine() {

    String testRoutineName = "AUTOMATA STANDARD OPERATIONS";

    printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    TestCounter counter = new TestCounter();

      /* Co-Accessible Operation Tests */

    printTestOutput("CO-ACCESSIBLE OPERATION: ", 2);

    printTestOutput("Instantiating automaton from Figure 2.1...", 3);
    Automaton fig2_12 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_12.hdr"), new File("aut/fig2_12.bdy"), 1),
      "a,T,T\nb,T,T\ng,T,T", // Events
      "@zero,F\none,F\ntwo,T\nthree,F\nfour,F\nfive,F\nsix,F", // States 
      "zero,a,one\none,a,three\none,b,two\none,g,five\ntwo,g,zero\nthree,b,four\nfour,g,four\nfour,a,three\nsix,a,three\nsix,b,two" // Transitions
    ));

    printTestOutput("Taking the co-accessible part of Figure 2.12 (and comparing the result to the automaton in Figure 2.13a)...", 3);
    Automaton result = saveAndLoadAutomaton(fig2_12.coaccessible(new File("aut/coaccessible.hdr"), new File("aut/coaccessible.bdy")));

    result.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero,F\none,F\ntwo,T\nsix,F"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero,a,one\none,b,two\ntwo,g,zero\nsix,b,two"), counter);

      /* Trim Operation Tests */

    printTestOutput("TRIM OPERATION: ", 2);

    printTestOutput("Trimming the automaton in Figure 2.12 (and comparing the result to the automaton in Figure 2.13b)...", 3);
    result = saveAndLoadAutomaton(fig2_12.trim(new File("aut/trim.hdr"), new File("aut/trim.bdy")));

    result.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero,F\none,F\ntwo,T"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero,a,one\none,b,two\ntwo,g,zero"), counter);

      /* Complement Operation Tests */

    printTestOutput("COMPLEMENT OPERATION: ", 2);

    printTestOutput("Instantiating an automaton...", 3);
    Automaton complementExample = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/complementExample.hdr"), new File("aut/complementExample.bdy"), 3),
      "a1,TFF,FFF\na2,TFF,FFF\nb1,FTF,FFF\nb2,FTF,FFF\nc1,FFT,FFF\nc2,FFT,FFF\no,FFF,TTT", // Events
      "@0,F\n1,F\n2,F\n3,F\n4,F\n5,F\n6,F\n7,F\n8,F\n9,F\n10,F\n11,F\n12,F\n13,F\n14,F\n15,F\n16,F\n17,F\n18,F\n19,F", // States 
      "0,a1,4\n0,b2,3\n0,b1,2\n0,c1,1\n1,b2,6\n1,a2,5\n2,a1,7\n3,c2,8\n4,b1,9\n5,b1,10\n6,a1,11\n7,c2,12\n8,a2,13\n9,c1,14\n10,o,15\n11,o,16\n12,o,17\n13,o,18:BAD\n14,o,19:BAD" // Transitions
    ));

    printTestOutput("Taking the complement of the automaton...", 3);

    try {
    
      result = saveAndLoadAutomaton(complementExample.complement(new File("aut/complement.hdr"), new File("aut/complement.bdy")));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a1,TFF,FFF\na2,TFF,FFF\nb1,FTF,FFF\nb2,FTF,FFF\nc1,FFT,FFF\nc2,FFT,FFF\no,FFF,TTT"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@0,T\nDump State,F\n1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T\n8,T\n9,T\n10,T\n11,T\n12,T\n13,T\n14,T\n15,T\n16,T\n17,T\n18,T\n19,T"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "0,a1,4\n0,b2,3\n0,b1,2\n0,c1,1\n0,a2,Dump State\n0,c2,Dump State\n0,o,Dump State\n1,b2,6\n1,a2,5\n1,a1,Dump State\n1,b1,Dump State\n1,c1,Dump State\n1,c2,Dump State\n1,o,Dump State\n2,a1,7\n2,a2,Dump State\n2,b1,Dump State\n2,b2,Dump State\n2,c1,Dump State\n2,c2,Dump State\n2,o,Dump State\n3,c2,8\n3,a1,Dump State\n3,a2,Dump State\n3,b1,Dump State\n3,b2,Dump State\n3,c1,Dump State\n3,o,Dump State\n4,b1,9\n4,a2,Dump State\n4,a1,Dump State\n4,b2,Dump State\n4,c1,Dump State\n4,c2,Dump State\n4,o,Dump State\n5,b1,10\n5,a2,Dump State\n5,a1,Dump State\n5,b2,Dump State\n5,c1,Dump State\n5,c2,Dump State\n5,o,Dump State\n6,a1,11\n6,a2,Dump State\n6,b1,Dump State\n6,b2,Dump State\n6,c1,Dump State\n6,c2,Dump State\n6,o,Dump State\n7,c2,12\n7,a1,Dump State\n7,a2,Dump State\n7,b1,Dump State\n7,b2,Dump State\n7,c1,Dump State\n7,o,Dump State\n8,a2,13\n8,a1,Dump State\n8,c2,Dump State\n8,b1,Dump State\n8,b2,Dump State\n8,c1,Dump State\n8,o,Dump State\n9,c1,14\n9,a1,Dump State\n9,a2,Dump State\n9,b1,Dump State\n9,b2,Dump State\n9,c2,Dump State\n9,o,Dump State\n10,o,15\n10,a1,Dump State\n10,a2,Dump State\n10,b1,Dump State\n10,b2,Dump State\n10,c2,Dump State\n10,c1,Dump State\n11,o,16\n11,a1,Dump State\n11,a2,Dump State\n11,b1,Dump State\n11,b2,Dump State\n11,c2,Dump State\n11,c1,Dump State\n12,o,17\n12,a1,Dump State\n12,a2,Dump State\n12,b1,Dump State\n12,b2,Dump State\n12,c2,Dump State\n12,c1,Dump State\n13,o,18:BAD\n13,a1,Dump State\n13,a2,Dump State\n13,b1,Dump State\n13,b2,Dump State\n13,c2,Dump State\n13,c1,Dump State\n14,o,19:BAD\n14,a1,Dump State\n14,a2,Dump State\n14,b1,Dump State\n14,b2,Dump State\n14,c2,Dump State\n14,c1,Dump State\n15,o,Dump State\n15,a1,Dump State\n15,a2,Dump State\n15,b1,Dump State\n15,b2,Dump State\n15,c2,Dump State\n15,c1,Dump State\n16,o,Dump State\n16,a1,Dump State\n16,a2,Dump State\n16,b1,Dump State\n16,b2,Dump State\n16,c2,Dump State\n16,c1,Dump State\n17,o,Dump State\n17,a1,Dump State\n17,a2,Dump State\n17,b1,Dump State\n17,b2,Dump State\n17,c2,Dump State\n17,c1,Dump State\n18,o,Dump State\n18,a1,Dump State\n18,a2,Dump State\n18,b1,Dump State\n18,b2,Dump State\n18,c2,Dump State\n18,c1,Dump State\n19,o,Dump State\n19,a1,Dump State\n19,a2,Dump State\n19,b1,Dump State\n19,b2,Dump State\n19,c2,Dump State\n19,c1,Dump State\n"), counter);
    
    } catch(OperationFailedException e) {

      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    
    }

    printTestOutput("Instantiating an automaton...", 3);
    Automaton complementExample2 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/complementExample.hdr"), new File("aut/complementExample.bdy"), 1),
      "a,T,F\nb,T,T", // Events
      "0,T\n1,F", // States 
      "0,a,1\n0,b,0\n1,a,0\n1,b,0" // Transitions
    ));

    printTestOutput("Taking the complement of the automaton which will not need a dump state...", 3);

    try {
    
      result = saveAndLoadAutomaton(complementExample2.complement(null, null));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,F\nb,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "0,F\n1,T"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "0,a,1\n0,b,0\n1,a,0\n1,b,0"), counter);
    
    } catch(OperationFailedException e) {

      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    
    }

      /* Intersection Operation Tests */

    printTestOutput("INTERSECTION OPERATION: ", 2);

    printTestOutput("Instantiating automaton from Figure 2.1...", 3);
    Automaton fig2_1 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_1.hdr"), new File("aut/fig2_1.bdy"), 1),
      "a,T,T\nb,T,T\ng,T,T", // Events
      "@x,T\ny,F\nz,T", // States 
      "x,a,x\nx,g,z\ny,b,y\ny,a,x\nz,b,z\nz,a,y\nz,g,y" // Transitions
    ));
    printTestOutput("Instantiating automaton from Figure 2.2...", 3);
    Automaton fig2_2 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_2.hdr"), new File("aut/fig2_2.bdy"), 1),
      "a,T,T\nb,T,T", // Events
      "@zero,F\none,T", // States 
      "zero,b,zero\nzero,a,one\none,a,one\none,b,zero" // Transitions
    ));

    printTestOutput("Taking the intersection of Figure 2.1 and Figure 2.2 (and comparing the result to the first automaton in Figure 2.15)...", 3);

    try {
      result = saveAndLoadAutomaton(Automaton.intersection(fig2_1, fig2_2, new File("aut/intersection.hdr"), new File("aut/intersection.bdy")));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x_zero,F\nx_one,T"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x_zero,a,x_one\nx_one,a,x_one"), counter);
    } catch(IncompatibleAutomataException e) {
      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    }

    printTestOutput("Instantiating automaton from Figure 2.13(b)...", 3);
    Automaton fig2_13b = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_13b.hdr"), new File("aut/fig2_13b.bdy"), 1),
      "a,T,T\nb,T,T\ng,T,T", // Events
      "@zero,F\none,F\ntwo,T", // States 
      "zero,a,one\none,b,two\ntwo,g,zero" // Transitions
    ));

    printTestOutput("Taking the intersection of Figure 2.2 and Figure 2.13(b) (and comparing the result to the second automaton in Figure 2.15)...", 3);

    try {
      result = saveAndLoadAutomaton(Automaton.intersection(fig2_2, fig2_13b, new File("aut/intersection.hdr"), new File("aut/intersection.bdy")));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@zero_zero,F\none_one,F\nzero_two,F"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "zero_zero,a,one_one\none_one,b,zero_two"), counter);
    } catch(IncompatibleAutomataException e) {
      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    }

    printTestOutput("Instantiating the first automaton from Figure 2.20...", 3);
    Automaton fig2_20a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_20a.hdr"), new File("aut/fig2_20a.bdy"), 1),
      "a1\na2\nb\nr", // Events
      "@x1,F\nx2,F\nx3,T", // States 
      "x1,a1,x2\nx1,a2,x2\nx2,b,x3\nx3,r,x1" // Transitions
    ));

    printTestOutput("Instantiating the second automaton from Figure 2.20...", 3);
    Automaton fig2_20b = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_20b.hdr"), new File("aut/fig2_20b.bdy"), 1),
      "a1\nb\nc1\nr\na2\nc2", // Events
      "@y1,F\ny2,F\ny3,F\ny4,F\ny5,F\ny6,F", // States 
      "y1,a1,y2\ny2,b,y4\ny4,r,y1\ny4,c1,y6\ny6,r,y1\ny1,a2,y3\ny3,b,y5\ny5,c2,y6\ny5,r,y1" // Transitions
    ));

    printTestOutput("Taking the intersection of the first two automata in Figure 2.20 (and comparing the result to the third automaton in Figure 2.20)...", 3);
    try {
      result = saveAndLoadAutomaton(Automaton.intersection(fig2_20a, fig2_20b, new File("aut/intersection.hdr"), new File("aut/intersection.bdy")));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a1,T,T\na2,T,T\nb,T,T\nr,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x1_y1,F\nx2_y2,F\nx2_y3,F\nx3_y4,F\nx3_y5,F"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x1_y1,a1,x2_y2\nx1_y1,a2,x2_y3\nx2_y2,b,x3_y4\nx2_y3,b,x3_y5\nx3_y4,r,x1_y1\nx3_y5,r,x1_y1"), counter);
    } catch(IncompatibleAutomataException e) {
      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    }

      /* Union Operation Tests */

    printTestOutput("UNION OPERATION: ", 2);

    printTestOutput("Taking the union of Figure 2.1 and Figure 2.2 (and comparing the result to the automaton in Figure 2.16)...", 3);

    try {
      result = saveAndLoadAutomaton(Automaton.union(fig2_1, fig2_2, new File("aut/union.hdr"), new File("aut/union.bdy")));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\ng,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@x_zero,F\ny_zero,F\nz_zero,F\nx_one,T\ny_one,F\nz_one,T"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "x_zero,a,x_one\nx_zero,g,z_zero\ny_zero,b,y_zero\ny_zero,a,x_one\nz_zero,b,z_zero\nz_zero,a,y_one\nz_zero,g,y_zero\nx_one,a,x_one\nx_one,g,z_one\ny_one,b,y_zero\ny_one,a,x_one\nz_one,b,z_zero\nz_one,a,y_one\nz_one,g,y_one"), counter);
    } catch(IncompatibleAutomataException e) {
      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    }

    printTestOutput("Instantiating the first automaton from Figure 2.17...", 3);
    Automaton fig2_17a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_17a.hdr"), new File("aut/fig2_17a.bdy"), 1),
      "a,T,T\nb,T,T\nc,T,T", // Events
      "@one,T\ntwo,F", // States 
      "one,c,one\none,a,two\ntwo,b,two" // Transitions
    ));

    printTestOutput("Instantiating the second automaton from Figure 2.17...", 3);
    Automaton fig2_17b = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_17b.hdr"), new File("aut/fig2_17b.bdy"), 1),
      "b,T,T\na,T,T\nd,T,T", // Events
      "@A,T\nB,F", // States 
      "A,b,A\nA,a,B\nB,d,B" // Transitions
    ));

    printTestOutput("Instantiating the third automaton from Figure 2.17...", 3);
    Automaton fig2_17c = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/fig2_17c.hdr"), new File("aut/fig2_17c.bdy"), 1),
      "c,T,T\nb,T,T\na,T,T", // Events
      "@D,T\nE,F", // States 
      "D,c,D\nD,b,E\nE,a,E" // Transitions
    ));

    printTestOutput("Taking the union of the three automata in Figure 2.17 (and comparing the result to the automaton described in Example 2.17)...", 3);
    
    try {
      result = saveAndLoadAutomaton(Automaton.union(
        saveAndLoadAutomaton(Automaton.union(fig2_17a, fig2_17b, new File("aut/union1.hdr"), new File("aut/union1.bdy"))),
        fig2_17c,
        new File("aut/union2.hdr"),
        new File("aut/union2.bdy")
      ));
      result.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(result.getEventInput(), "a,T,T\nb,T,T\nc,T,T\nd,T,T"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(result.getStateInput(), "@one_A_D,T"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(result.getTransitionInput(), "one_A_D,c,one_A_D"), counter);
    } catch(IncompatibleAutomataException e) {
      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);
    }

    /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static TestCounter runAutomataSpecialOperationsTestRoutine() {

    String testRoutineName = "AUTOMATA SPECIAL OPERATIONS";

    printTestOutput("RUNNING " + testRoutineName + " TEST ROUTINE...", 1);

    TestCounter counter = new TestCounter();

      /* Synchronized Composition Operation Tests */

    printTestOutput("SYNCHRONIZED COMPOSITION OPERATION: ", 2);

    printTestOutput("Instantiating an automaton...", 3);
    Automaton synchronizedCompositionExample = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,TF,TF\nb,FT,FT\no,TT,TF", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));

    printTestOutput("Taking the U-Structure (expecting no conditional violations)...", 3);
    UStructure uStructure = saveAndLoadUStructure(synchronizedCompositionExample.synchronizedComposition(new File("aut/synchronizedComposition.hdr"), new File("aut/synchronizedComposition.bdy")));
    uStructure.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructure.getEventInput(), "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TF"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructure.getStateInput(), "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructure.getTransitionInput(), "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:UNCONDITIONAL_VIOLATION\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TF"), counter);

    printTestOutput("Instantiating a simple automaton with a self-loop...", 3);
    Automaton automatonSelfLoop = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 1),
      "a,F,T", // Events
      "@1,T", // States
      "1,a,1" // Transitions
    ));

    printTestOutput("Instantiating an automaton...", 3);
    synchronizedCompositionExample = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(new File("aut/synchronizedCompositionExample.hdr"), new File("aut/synchronizedCompositionExample.bdy"), 2),
      "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));

    printTestOutput("Taking the U-Structure of the automaton...", 3);
    uStructure = saveAndLoadUStructure(synchronizedCompositionExample.synchronizedComposition(new File("aut/synchronizedComposition.hdr"), new File("aut/synchronizedComposition.bdy")));
    uStructure.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructure.getEventInput(), "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructure.getStateInput(), "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructure.getTransitionInput(), "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT"), counter);

    printTestOutput("Taking the U-Structure of the automaton...", 3);
    UStructure uStructureSelfLoop = saveAndLoadUStructure(automatonSelfLoop.synchronizedComposition(null, null));
    uStructureSelfLoop.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructureSelfLoop.getEventInput(), "<a,*>,F,F\n<*,a>,F,T"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructureSelfLoop.getStateInput(), "@1_1"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructureSelfLoop.getTransitionInput(), "1_1,<a,*>,1_1\n1_1,<*,a>,1_1"), counter);

    printTestOutput("Instantiating a more complex automaton with a self-loop...", 3);
    Automaton automatonSelfLoopExtended = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,TF,TT\nb,FT,FT", // Events
      "@1,T\n2,T", // States
      "1,b,2\n1,a,1" // Transitions
    ));

    printTestOutput("Taking the synchronized composition of the automaton...", 3);
    UStructure uStructureSelfLoopExtended = saveAndLoadUStructure(automatonSelfLoopExtended.synchronizedComposition(null, null));
    uStructureSelfLoopExtended.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructureSelfLoopExtended.getEventInput(), "<a,a,*>,TF,TF\n<*,*,a>,FF,FT\n<*,b,*>,FF,FF\n<b,*,b>,FT,FT"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructureSelfLoopExtended.getStateInput(), "@1_1_1\n1_2_1\n2_1_2\n2_2_2"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructureSelfLoopExtended.getTransitionInput(), "1_1_1,<b,*,b>,2_1_2\n1_1_1,<a,a,*>,1_1_1\n1_1_1,<*,b,*>,1_2_1\n1_1_1,<*,*,a>,1_1_1\n1_2_1,<b,*,b>,2_2_2\n1_2_1,<*,*,a>,1_2_1\n2_1_2,<*,b,*>,2_2_2"), counter);

    printTestOutput("Instantiating an automaton which brings out the special observability case...", 3);
    Automaton automaton = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 1),
      "a,T,F\nb,F,F\no,F,T", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));

    printTestOutput("Taking the synchronized composition of the automaton...", 3);
    UStructure uStructure2 = saveAndLoadUStructure(automaton.synchronizedComposition(null, null));
    uStructure2.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructure2.getEventInput(), "<a,a>,T,F\n<b,*>,F,F\n<*,b>,F,F\n<o,*>,F,F\n<*,o>,F,T"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructure2.getStateInput(), "@1_1\n1_3\n2_2\n2_4\n2_5\n2_6\n2_7\n3_1\n3_3\n4_2\n4_4\n4_5\n4_6\n4_7\n5_2\n5_4\n5_5\n5_6\n5_7\n6_2\n6_4\n6_5\n6_6\n6_7\n7_2\n7_4\n7_5\n7_6\n7_7"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructure2.getTransitionInput(), "1_1,<a,a>,2_2\n1_1,<b,*>,3_1\n1_1,<*,b>,1_3\n1_3,<a,a>,2_5\n1_3,<b,*>,3_3\n2_2,<b,*>,4_2\n2_2,<*,b>,2_4\n2_4,<b,*>,4_4\n2_4,<*,o>,2_6\n2_5,<b,*>,4_5\n2_5,<*,o>,2_7\n2_6,<b,*>,4_6\n2_7,<b,*>,4_7\n3_1,<a,a>,5_2\n3_1,<*,b>,3_3\n3_3,<a,a>,5_5\n4_2,<o,*>,6_2\n4_2,<*,b>,4_4\n4_4,<o,*>,6_4\n4_4,<*,o>,4_6\n4_5,<o,*>,6_5\n4_5,<*,o>,4_7\n4_6,<o,*>,6_6\n4_7,<o,*>,6_7\n5_2,<o,*>,7_2\n5_2,<*,b>,5_4\n5_4,<o,*>,7_4:UNCONDITIONAL_VIOLATION\n5_4,<*,o>,5_6:UNCONDITIONAL_VIOLATION\n5_5,<o,*>,7_5\n5_5,<*,o>,5_7\n5_6,<o,*>,7_6:UNCONDITIONAL_VIOLATION\n5_7,<o,*>,7_7\n6_2,<*,b>,6_4\n6_4,<*,o>,6_6\n6_5,<*,o>,6_7\n7_2,<*,b>,7_4\n7_4,<*,o>,7_6:UNCONDITIONAL_VIOLATION\n7_5,<*,o>,7_7"), counter);

      /* Add Communications Operation Tests */

    printTestOutput("ADD COMMUNICATIONS OPERATION: ", 2);

    printTestOutput("Add communications to the automaton generated by synchronized composition (Test case for GitHub Issue #9)...", 3);
    UStructure addCommunications = null;

    try {

      addCommunications = saveAndLoadUStructure(uStructure.addCommunications(new File("aut/addCommunications.hdr"), new File("aut/addCommunications.bdy")));
      addCommunications.generateInputForGUI();
      printTestCase("Ensuring the events are correct", new TestResult(addCommunications.getEventInput(), "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF"), counter);
      printTestCase("Ensuring the states are correct", new TestResult(addCommunications.getStateInput(), "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7"), counter);
      printTestCase("Ensuring the transitions are correct", new TestResult(addCommunications.getTransitionInput(), "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:POTENTIAL_COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:POTENTIAL_COMMUNICATION-SR\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:POTENTIAL_COMMUNICATION-RS\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:POTENTIAL_COMMUNICATION-SR\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:POTENTIAL_COMMUNICATION-RS\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:POTENTIAL_COMMUNICATION-RS\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:POTENTIAL_COMMUNICATION-SR\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:POTENTIAL_COMMUNICATION-SR\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT"), counter);
    
    } catch (NullPointerException e) {

      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);

    }
    
    printTestOutput("Add communications to the same automaton as above (but this time generated by GUI input code)...", 3);
    printTestOutput("Instantiating a U-Structure...", 3);
    UStructure synchronizedComposition = saveAndLoadUStructure(AutomatonGenerator.generateFromGUICode(
      new UStructure(new File("aut/synchronizedComposition.hdr"), new File("aut/synchronizedComposition.bdy"), 2),
      "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT", // Events
      "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7", // States
      "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT" // Transitions
    ));
    addCommunications = saveAndLoadUStructure(synchronizedComposition.addCommunications(new File("aut/addCommunications.hdr"), new File("aut/addCommunications.bdy")));
    addCommunications.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(addCommunications.getEventInput(), "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(addCommunications.getStateInput(), "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(addCommunications.getTransitionInput(), "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:POTENTIAL_COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:POTENTIAL_COMMUNICATION-SR\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:POTENTIAL_COMMUNICATION-RS\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:POTENTIAL_COMMUNICATION-SR\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:POTENTIAL_COMMUNICATION-RS\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:POTENTIAL_COMMUNICATION-RS\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:POTENTIAL_COMMUNICATION-SR\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:POTENTIAL_COMMUNICATION-SR\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT"), counter);

      /* Feasible Protocol Operations Tests */

    printTestOutput("FEASIBLE PROTOCOL OPERATIONS: ", 2);

    printTestOutput("Generate all feasible protocols in the automaton generated above...", 3);
    List<Set<CommunicationData>> feasibleProtocols = addCommunications.generateAllFeasibleProtocols(addCommunications.getPotentialCommunications(), false);
    printTestCase("Ensuring that there are 8 feasible protocols", new TestResult(feasibleProtocols.size(), 8), counter);

    List<String> protocolsToString = protocolsToString(addCommunications, feasibleProtocols);
    printTestCase("Ensuring that protocol #1 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #2 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #3 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #4 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #5 is in the list", new TestResult(protocolsToString.contains("1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #6 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_3_1,<a,a,a>,2_5_2 (SR)\n3_1_3,<a,a,a>,5_2_5 (SR)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #7 is in the list", new TestResult(protocolsToString.contains("1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #8 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_3_1,<a,a,a>,2_5_2 (SR)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_1_3,<a,a,a>,5_2_5 (SR)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);

    printTestOutput("Generate smallest feasible protocols in the automaton generated above...", 3);
    List<Set<CommunicationData>> smallestFeasibleProtocols = addCommunications.generateSmallestFeasibleProtocols(addCommunications.getPotentialCommunications());
    printTestCase("Ensuring that there is 1 smallest feasible protocol", new TestResult(smallestFeasibleProtocols.size(), 1), counter);
    printTestCase("Ensuring that the protocol is correct", new TestResult(protocolsToString(addCommunications, smallestFeasibleProtocols).contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n")), counter);
    
    printTestOutput("Generating the pruned automaton for the feasible protocol with 2 communications...", 3);
    uStructure = addCommunications.applyProtocol(smallestFeasibleProtocols.get(0), null, null);
    uStructure.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructure.getEventInput(), "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructure.getStateInput(), "@1_1_1\n2_2_2\n2_4_2\n3_3_3\n3_3_5\n4_2_4\n4_4_4\n5_5_3\n5_5_5\n6_6_6\n7_7_7"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructure.getTransitionInput(), "1_1_1,<b,b,b>,3_3_3:COMMUNICATION-RS\n1_1_1,<a,a,a>,2_2_2:COMMUNICATION-SR\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_5,<a,a,*>,5_5_5\n4_2_4,<*,b,*>,4_4_4\n4_4_4,<o,o,o>,6_6_6\n5_5_3,<*,*,a>,5_5_5\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT"), counter);
    
    printTestOutput("Try to make a protocol containing 1 communication feasible...", 3);
    Set<CommunicationData> smallestProtocol = smallestFeasibleProtocols.get(0);
    smallestProtocol.remove(smallestProtocol.iterator().next());
    feasibleProtocols = addCommunications.makeProtocolFeasible(smallestProtocol);
    printTestCase("Ensuring that there are 6 feasible protocols", new TestResult(feasibleProtocols.size(), 6), counter);
    printTestCase("Ensuring that protocol #1 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #2 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #3 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #4 is in the list", new TestResult(protocolsToString.contains("1_1_1,<a,a,a>,2_2_2 (SR)\n1_1_1,<b,b,b>,3_3_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    printTestCase("Ensuring that protocol #5 is in the list", new TestResult(protocolsToString.contains("1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n")), counter);
    printTestCase("Ensuring that protocol #6 is in the list", new TestResult(protocolsToString.contains("1_1_1,<b,b,b>,3_3_3 (RS)\n1_1_2,<b,b,b>,3_3_4 (RS)\n2_2_1,<b,b,b>,4_4_3 (RS)\n2_2_2,<b,b,b>,4_4_4 (RS)\n3_3_3,<a,a,a>,5_5_5 (SR)\n")), counter);
    
    printTestOutput("Try to make a protocol containing 7 communications feasible...", 3);
    Set<CommunicationData> protocol = new HashSet<CommunicationData>(addCommunications.getPotentialCommunications());
    protocol.remove(protocol.iterator().next());
    feasibleProtocols = addCommunications.makeProtocolFeasible(protocol);
    printTestCase("Ensuring that there are no feasible protocols", new TestResult(feasibleProtocols.size(), 0), counter);

      /* Crush Operation Tests */

    printTestOutput("CRUSH OPERATION: ", 2);

    printTestOutput("Instantiating a pruned U-Structure...", 3);
    PrunedUStructure crushExample = saveAndLoadPrunedUStructure(AutomatonGenerator.generateFromGUICode(
      new PrunedUStructure(new File("aut/crushExample.hdr"), new File("aut/crushExample.bdy"), 2),
      "<a,a,a>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<o,o,o>,TT,TT", // Events
      "@1_1_1\n1_3_1\n2_2_2\n2_4_2\n2_5_2\n3_1_3\n3_3_3\n4_2_4\n4_4_4\n4_5_4\n5_2_5\n5_4_5\n5_5_5\n6_6_6\n6_7_6\n7_6_7\n7_7_7", // States
      "1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<a,a,a>,2_2_2:NASH_COMMUNICATION-SR-1.0-0.25\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<a,a,a>,2_5_2:NASH_COMMUNICATION-SR-2.0-0.25\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<a,a,a>,5_2_5:NASH_COMMUNICATION-SR-3.0-0.25\n3_3_3,<a,a,a>,5_5_5:NASH_COMMUNICATION-SR-4.0-0.25\n4_2_4,<*,b,*>,4_4_4\n4_4_4,<o,o,o>,6_6_6\n4_5_4,<o,o,o>,6_7_6\n5_2_5,<*,b,*>,5_4_5\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT" // Transitions
    ));

    printTestOutput("Applying crush operation with respect to controller 2...", 3);
    Crush crush = crushExample.crush(null, null, 2, null, Crush.CombiningCosts.SUM);

    crush.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(crush.getEventInput(), "<b,*,b>,FT,FT\n<o,o,o>,TT,TT\n<a,a,a>,TF,TF"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(crush.getStateInput(), "@<1_1_1,1_3_1>\n<3_1_3,3_3_3>\n<2_2_2,2_4_2,2_5_2>\n<4_2_4,4_4_4,4_5_4>\n<6_6_6,6_7_6>\n<5_2_5,5_4_5,5_5_5>\n<7_7_7,7_6_7>"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(crush.getTransitionInput(), "<1_1_1,1_3_1>,<b,*,b>,<3_1_3,3_3_3>\n<1_1_1,1_3_1>,<a,a,a>,<2_2_2,2_4_2,2_5_2>:NASH_COMMUNICATION-SR-3.0-0.5\n<3_1_3,3_3_3>,<a,a,a>,<5_2_5,5_4_5,5_5_5>:NASH_COMMUNICATION-SR-7.0-0.5\n<2_2_2,2_4_2,2_5_2>,<b,*,b>,<4_2_4,4_4_4,4_5_4>\n<4_2_4,4_4_4,4_5_4>,<o,o,o>,<6_6_6,6_7_6>\n<5_2_5,5_4_5,5_5_5>,<o,o,o>,<7_7_7,7_6_7>:DISABLEMENT_DECISION-FT"), counter);

    printTestOutput("Instantiating a U-Structure used to bring out a special case...", 3);
    UStructure crushExample2 = saveAndLoadUStructure(AutomatonGenerator.generateFromGUICode(
      new UStructure(null, null, 2),
      "<b,b,b>,TT,FF\n<c,*,c>,FT,FF\n<a,a,*>,TF,TF\n<*,c,*>,FF,TF\n<*,*,a>,FF,FT", // Events
      "1_1_1\n1_1_2\n2_2_1\n@2_2_2", // States
      "1_1_1,<c,*,c>,1_1_1\n1_1_1,<*,c,*>,1_1_1\n1_1_1,<a,a,*>,2_2_1\n1_1_1,<a,a,*>,1_1_2\n1_1_2,<*,c,*>,1_1_2\n2_2_1,<*,*,a>,2_2_2\n1_1_2,<a,a,*>,2_2_2\n2_2_2,<b,b,b>,1_1_1" // Transitions
    ));

    crush = crushExample2.crush(null, null, 2);

    crush.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(crush.getEventInput(), "<b,b,b>,TT,FF\n<c,*,c>,FT,FF"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(crush.getStateInput(), "@<1_1_1,1_1_2,2_2_1,2_2_2>"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(crush.getTransitionInput(), "<1_1_1,1_1_2,2_2_1,2_2_2>,<b,b,b>,<1_1_1,1_1_2,2_2_1,2_2_2>\n<1_1_1,1_1_2,2_2_1,2_2_2>,<c,*,c>,<1_1_1,1_1_2,2_2_1,2_2_2>"), counter);

      /* Nash Operation Tests */

    printTestOutput("NASH OPERATION: ", 2);

    UStructure nashExample = saveAndLoadUStructure(AutomatonGenerator.generateFromGUICode(
      new UStructure(new File("aut/nashExample.hdr"), new File("aut/nashExample.bdy"), 2),
      "<a,a,*>,TF,TF\n<b,*,b>,FT,FT\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<o,o,o>,TT,TT\n<*,b,a>,FF,FF\n<b,b,b>,FT,FT\n<a,a,a>,TF,TF", // Events
      "@1_1_1\n1_1_2\n1_3_1\n1_3_2\n2_2_1\n2_2_2\n2_4_1\n2_4_2\n2_5_1\n2_5_2\n3_1_3\n3_1_4\n3_1_5\n3_3_3\n3_3_4\n3_3_5\n4_2_3\n4_2_4\n4_2_5\n4_4_3\n4_4_4\n4_4_5\n4_5_3\n4_5_4\n4_5_5\n5_2_3\n5_2_4\n5_2_5\n5_4_3\n5_4_4\n5_4_5\n5_5_3\n5_5_4\n5_5_5\n6_6_6\n6_6_7\n6_7_6\n6_7_7\n7_6_6\n7_6_7\n7_7_6\n7_7_7", // States
      "1_1_1,<a,a,*>,2_2_1\n1_1_1,<b,*,b>,3_1_3\n1_1_1,<*,b,*>,1_3_1\n1_1_1,<*,*,a>,1_1_2\n1_1_1,<*,b,a>,1_3_2:INVALID_COMMUNICATION\n1_1_1,<b,b,b>,3_3_3:NASH_COMMUNICATION-RS-1-0.125\n1_1_1,<a,a,a>,2_2_2:NASH_COMMUNICATION-SR-1-0.125\n1_1_2,<a,a,*>,2_2_2\n1_1_2,<b,*,b>,3_1_4\n1_1_2,<*,b,*>,1_3_2\n1_1_2,<b,b,b>,3_3_4:NASH_COMMUNICATION-RS-1-0.125\n1_3_1,<a,a,*>,2_5_1\n1_3_1,<b,*,b>,3_3_3\n1_3_1,<*,*,a>,1_3_2\n1_3_1,<a,a,a>,2_5_2:NASH_COMMUNICATION-SR-1-0.125\n1_3_2,<a,a,*>,2_5_2\n1_3_2,<b,*,b>,3_3_4\n2_2_1,<b,*,b>,4_2_3\n2_2_1,<*,b,*>,2_4_1\n2_2_1,<*,*,a>,2_2_2\n2_2_1,<*,b,a>,2_4_2:INVALID_COMMUNICATION\n2_2_1,<b,b,b>,4_4_3:NASH_COMMUNICATION-RS-1-0.125\n2_2_2,<b,*,b>,4_2_4\n2_2_2,<*,b,*>,2_4_2\n2_2_2,<b,b,b>,4_4_4:NASH_COMMUNICATION-RS-1-0.125\n2_4_1,<b,*,b>,4_4_3\n2_4_1,<*,*,a>,2_4_2\n2_4_2,<b,*,b>,4_4_4\n2_5_1,<b,*,b>,4_5_3\n2_5_1,<*,*,a>,2_5_2\n2_5_2,<b,*,b>,4_5_4\n3_1_3,<a,a,*>,5_2_3\n3_1_3,<*,b,*>,3_3_3\n3_1_3,<*,*,a>,3_1_5\n3_1_3,<*,b,a>,3_3_5:INVALID_COMMUNICATION\n3_1_3,<a,a,a>,5_2_5:NASH_COMMUNICATION-SR-1-0.125\n3_1_4,<a,a,*>,5_2_4\n3_1_4,<*,b,*>,3_3_4\n3_1_5,<a,a,*>,5_2_5\n3_1_5,<*,b,*>,3_3_5\n3_3_3,<a,a,*>,5_5_3\n3_3_3,<*,*,a>,3_3_5\n3_3_3,<a,a,a>,5_5_5:NASH_COMMUNICATION-SR-1-0.125\n3_3_4,<a,a,*>,5_5_4\n3_3_5,<a,a,*>,5_5_5\n4_2_3,<*,b,*>,4_4_3\n4_2_3,<*,*,a>,4_2_5\n4_2_3,<*,b,a>,4_4_5:INVALID_COMMUNICATION\n4_2_4,<*,b,*>,4_4_4\n4_2_5,<*,b,*>,4_4_5\n4_4_3,<*,*,a>,4_4_5\n4_4_4,<o,o,o>,6_6_6\n4_4_5,<o,o,o>,6_6_7\n4_5_3,<*,*,a>,4_5_5\n4_5_4,<o,o,o>,6_7_6\n4_5_5,<o,o,o>,6_7_7:CONDITIONAL_VIOLATION\n5_2_3,<*,b,*>,5_4_3\n5_2_3,<*,*,a>,5_2_5\n5_2_3,<*,b,a>,5_4_5:INVALID_COMMUNICATION\n5_2_4,<*,b,*>,5_4_4\n5_2_5,<*,b,*>,5_4_5\n5_4_3,<*,*,a>,5_4_5\n5_4_4,<o,o,o>,7_6_6:UNCONDITIONAL_VIOLATION\n5_4_5,<o,o,o>,7_6_7:DISABLEMENT_DECISION-FT\n5_5_3,<*,*,a>,5_5_5\n5_5_4,<o,o,o>,7_7_6:DISABLEMENT_DECISION-TF\n5_5_5,<o,o,o>,7_7_7:DISABLEMENT_DECISION-TT" // Transitions
    ));

    try {

      List<Set<NashCommunicationData>> nashEquilibria = nashExample.findNashEquilibria(Crush.CombiningCosts.UNIT);
      printTestCase("Ensuring that there are 3 Nash equilibria", new TestResult(nashEquilibria.size(), 3), counter);

      List<String> equilibriaToString = equilibriaToString(nashExample, nashEquilibria);
      printTestCase("Ensuring that Nash equilibria #1 is in the list", new TestResult(equilibriaToString.contains("1_1_1,<a,a,a>,2_2_2 (SR),1.0,0.125\n1_1_1,<b,b,b>,3_3_3 (RS),1.0,0.125\n")), counter);
      printTestCase("Ensuring that Nash equilibria #2 is in the list", new TestResult(equilibriaToString.contains("1_1_1,<b,b,b>,3_3_3 (RS),1.0,0.125\n1_1_2,<b,b,b>,3_3_4 (RS),1.0,0.125\n2_2_1,<b,b,b>,4_4_3 (RS),1.0,0.125\n2_2_2,<b,b,b>,4_4_4 (RS),1.0,0.125\n")), counter);
      printTestCase("Ensuring that Nash equilibria #3 is in the list", new TestResult(equilibriaToString.contains("1_1_1,<a,a,a>,2_2_2 (SR),1.0,0.125\n1_3_1,<a,a,a>,2_5_2 (SR),1.0,0.125\n3_1_3,<a,a,a>,5_2_5 (SR),1.0,0.125\n3_3_3,<a,a,a>,5_5_5 (SR),1.0,0.125\n")), counter);
      
    } catch (DoesNotSatisfyObservabilityException e) {

      e.printStackTrace();
      counter.increment(false);
      counter.increment(false);
      counter.increment(false);
      System.out.println(RED + "\t\t\t*** FAILED 3 TESTS DUE TO EXCEPTION ***" + RESET);

    }

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }
  
  private static TestCounter runSpecialTransitionsTestRoutine() {

    String testRoutineName = "SPECIAL TRANSITIONS";

    printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    TestCounter counter = new TestCounter();

      /* Controllability Tests */

    printTestOutput("DISABLEMENT DECISIONS: ", 2);

    printTestOutput("Instantiating an Automaton...", 3);

    Automaton automaton = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,TF,FF\nb,FT,FF\nc,TT,FT", // Events
      "@0,F\n1,F\n2,F\n3,F\n4,F", // States
      "0,a,1\n0,b,2\n1,c,3\n2,c,4:BAD" // Transitions
    ));

    printTestOutput("Performing synchronized composition on the automaton...", 3);
    UStructure uStructure = automaton.synchronizedComposition(null, null);
    uStructure.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(uStructure.getEventInput(), "<a,a,*>,TF,FF\n<b,*,b>,FT,FF\n<*,b,*>,FF,FF\n<*,*,a>,FF,FF\n<c,c,c>,TT,FT"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(uStructure.getStateInput(), "@0_0_0\n0_0_1\n0_2_0\n0_2_1\n1_1_0\n1_1_1\n2_0_2\n2_2_2\n3_3_3\n4_4_4"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(uStructure.getTransitionInput(), "0_0_0,<a,a,*>,1_1_0\n0_0_0,<b,*,b>,2_0_2\n0_0_0,<*,b,*>,0_2_0\n0_0_0,<*,*,a>,0_0_1\n0_0_1,<a,a,*>,1_1_1\n0_0_1,<*,b,*>,0_2_1\n0_2_0,<b,*,b>,2_2_2\n0_2_0,<*,*,a>,0_2_1\n1_1_0,<*,*,a>,1_1_1\n1_1_1,<c,c,c>,3_3_3\n2_0_2,<*,b,*>,2_2_2\n2_2_2,<c,c,c>,4_4_4:DISABLEMENT_DECISION-FT"), counter);

    printTestOutput("Taking the crush of the U-Structure...", 3);
    Crush crush = uStructure.crush(null, null, 1);
    crush.generateInputForGUI();
    printTestCase("Ensuring the events are correct", new TestResult(crush.getEventInput(), "<a,a,*>,TF,FF\n<c,c,c>,TT,FT"), counter);
    printTestCase("Ensuring the states are correct", new TestResult(crush.getStateInput(), "@<0_0_0,0_0_1,0_2_0,0_2_1,2_0_2,2_2_2>\n<1_1_0,1_1_1>\n<3_3_3>\n<4_4_4>"), counter);
    printTestCase("Ensuring the transitions are correct", new TestResult(crush.getTransitionInput(), "<0_0_0,0_0_1,0_2_0,0_2_1,2_0_2,2_2_2>,<a,a,*>,<1_1_0,1_1_1>\n<0_0_0,0_0_1,0_2_0,0_2_1,2_0_2,2_2_2>,<c,c,c>,<4_4_4>:DISABLEMENT_DECISION-FT\n<1_1_0,1_1_1>,<c,c,c>,<3_3_3>"), counter);

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static TestCounter runAutomataPropertiesTestRoutine() {

    String testRoutineName = "TESTING FOR AUTOMATA PROPERTIES";

    printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    TestCounter counter = new TestCounter();

      /* Controllability Tests */

    printTestOutput("Instantiating automaton...", 3);
    Automaton a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "c,TF,TF\nb,TF,TF\na,TF,TF", // Events
      "@1,T\n2,F", // States 
      "1,c,1\n1,b,2:BAD\n2,a,2" // Transitions
    ));
    printTestCase("Ensuring that the automaton is controllable", new TestResult(a.testControllability(), true), counter);

    printTestOutput("Instantiating automaton...", 3);
    a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "c,TF,TF\nb,TF,TF\na,TF,FF", // Events
      "@1,T\n2,F", // States 
      "1,c,1:BAD\n1,b,2\n2,a,2:BAD" // Transitions
    ));
    printTestCase("Ensuring that the automaton is not controllable", new TestResult(a.testControllability(), false), counter);

      /* Observability Tests */

    printTestOutput("TESTING OBSERVABILITY: ", 2);

    printTestOutput("Instantiating automaton...", 3);
    a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,TF,TF\nb,FT,FT\no,TT,TT", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States 
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));
    printTestCase("Ensuring that the automaton is observable", new TestResult(a.testObservability(), true), counter);

    printTestOutput("Instantiating automaton...", 3);
    a = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "a,FF,TF\nb,FT,FT\no,TT,TT", // Events
      "@1,T\n2,T\n3,T\n4,T\n5,T\n6,T\n7,T", // States 
      "1,a,2\n1,b,3\n2,b,4\n3,a,5\n4,o,6\n5,o,7:BAD" // Transitions
    ));
    printTestCase("Ensuring that the automaton is not observable", new TestResult(a.testObservability(), false), counter);

      /* Shapley Value Tests */

    printTestOutput("FINDING SHAPLEY VALUES: ", 2);

    printTestOutput("Loading pre-generated U-Structure...", 3);
    UStructure uStructure = new UStructure(new File("saved automata/shapleyUStructure.hdr"), new File("saved automata/shapleyUStructure.bdy"));

    printTestOutput("Finding Shapley values...", 3);
    Map<Set<Integer>, Integer> shapleyValues = uStructure.findShapleyValues();

    printTestCase("Ensuring that coalition [] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>()), 0), counter);
    printTestCase("Ensuring that coalition [1] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(1))), 1), counter);
    printTestCase("Ensuring that coalition [2] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(2))), 3), counter);
    printTestCase("Ensuring that coalition [3] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(3))), 2), counter);
    printTestCase("Ensuring that coalition [1, 2] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(1, 2))), 3), counter);
    printTestCase("Ensuring that coalition [1, 3] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(1, 3))), 2), counter);
    printTestCase("Ensuring that coalition [2, 3] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(2, 3))), 3), counter);
    printTestCase("Ensuring that coalition [1, 2, 3] is correct", new TestResult(shapleyValues.get(new HashSet<Integer>(Arrays.asList(1, 2, 3))), 3), counter);

    printTestOutput("Modifying one coalition value to match the error in the paper...", 3);
    shapleyValues.put(new HashSet<Integer>(Arrays.asList(1, 3)), 3);    

    printTestOutput("Find Shapley values for each controller comparing against answers in the paper...", 3);
    printTestCase("Ensuring that value for controller 1 is correct", new TestResult(uStructure.findShapleyValueForController(shapleyValues, 1), 0.5), counter);
    printTestCase("Ensuring that value for controller 2 is correct", new TestResult(uStructure.findShapleyValueForController(shapleyValues, 2), 1.5), counter);
    printTestCase("Ensuring that value for controller 3 is correct", new TestResult(uStructure.findShapleyValueForController(shapleyValues, 3), 1.0), counter);

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  private static TestCounter runExceptionHandlingTestRoutine() {

    String testRoutineName = "EXCEPTION HANDLING";

    printTestOutput("RUNNING " + testRoutineName + " TESTS...", 1);

    TestCounter counter = new TestCounter();

      /* IncompatibleAutomataException Tests */

    printTestOutput("IncompatibleAutomataException Tests: ", 2);

    printTestOutput("Instantiating an automaton...", 3);
    Automaton automaton1 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(),
      "a,T,T\nb,T,F\nc,F,T\nd,F,F", // Events
      "", // States  
      "" // Transitions
    ));

    printTestOutput("Instantiating a second automaton (with an incompatible event)...", 3);
    Automaton automaton2 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(),
      "a,T,T\nc,T,T\ne,T,F", // Events
      "", // States  
      "" // Transitions
    ));

    try {
      printTestOutput("Taking the union of the two instantiated automata...", 3);
      Automaton.union(automaton1, automaton2, null, null);
      printTestCase("Ensuring that an IncompatibleAutomataException was raised", new TestResult(false), counter);
    } catch(IncompatibleAutomataException e) {
      printTestCase("Ensuring that an IncompatibleAutomataException was raised", new TestResult(true), counter);
    }

    printTestOutput("Instantiating a third automaton (with different number of controllers)...", 3);
    Automaton automaton3 = saveAndLoadAutomaton(AutomatonGenerator.generateFromGUICode(
      new Automaton(null, null, 2),
      "", // Events
      "", // States  
      "" // Transitions
    ));

    try {
      printTestOutput("Taking the union of the first and third instantiated automata...", 3);
      Automaton.union(automaton1, automaton3, null, null);
      printTestCase("Ensuring that an IncompatibleAutomataException was raised", new TestResult(false), counter);
    } catch(IncompatibleAutomataException e) {
      printTestCase("Ensuring that an IncompatibleAutomataException was raised", new TestResult(true), counter);
    }

      /* Print summary of this test routine */

    printTestRoutineSummary(testRoutineName, counter);

    return counter;

  }

  // This brings out a lot of subtle bugs
  private static Automaton saveAndLoadAutomaton(Automaton automaton) {
    
    automaton.closeFiles();

    return new Automaton(automaton.getHeaderFile(), automaton.getBodyFile(), false);

  }

  private static UStructure saveAndLoadUStructure(UStructure uStructure) {
    
    uStructure.closeFiles();
    
    return new UStructure(uStructure.getHeaderFile(), uStructure.getBodyFile());

  }

  private static PrunedUStructure saveAndLoadPrunedUStructure(PrunedUStructure prunedUStructure) {
    
    prunedUStructure.closeFiles();
    
    return new PrunedUStructure(prunedUStructure.getHeaderFile(), prunedUStructure.getBodyFile());

  }

  private static List<String> protocolsToString(UStructure uStructure, List<Set<CommunicationData>> protocols) {

    List<String> list = new ArrayList<String>();
    
    for (Set<CommunicationData> protocol : protocols) {

      // Put each communication as a string into a list
      List<String> communications = new ArrayList<String>();
      for (CommunicationData data : protocol)
        communications.add(data.toString(uStructure) + "\n");

      // Sort the list, so that the it is always in alphabetical order (meaning the test cases are more consistent)
      Collections.sort(communications);

      // Put together the sorted strings
      StringBuilder stringBuilder = new StringBuilder();
      for (String str : communications)
        stringBuilder.append(str);

      list.add(stringBuilder.toString());

    }

    return list;

  }

  private static List<String> equilibriaToString(UStructure uStructure, List<Set<NashCommunicationData>> equilibria) {

    List<String> list = new ArrayList<String>();
    
    for (Set<NashCommunicationData> equilibrium : equilibria) {

      // Put each communication as a string into a list
      List<String> communications = new ArrayList<String>();
      for (NashCommunicationData data : equilibrium)
        communications.add(data.toNashString(uStructure) + "\n");

      // Sort the list, so that the it is always in alphabetical order (meaning the test cases are more consistent)
      Collections.sort(communications);

      // Put together the sorted strings
      StringBuilder stringBuilder = new StringBuilder();
      for (String str : communications)
        stringBuilder.append(str);

      list.add(stringBuilder.toString());

    }

    return list;

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

  private static Integer[] getParetoRanks(int[] objective1, int[] objective2) {

    if (objective1.length != objective2.length)
      return null;

    int nIndividuals = objective1.length;
    Integer[] ranks = new Integer[nIndividuals];
    boolean[] isAssigned = new boolean[nIndividuals];
    int nRanksAssigned = 0;
    int currentRank = 1;

    // Repeat process until all individuals have been assigned a rank
    while (nRanksAssigned < nIndividuals) {

        /* Find the next pareto front, assigning those individuals the proper rank */

      List<Integer> paretoFront = getParetoFront(objective1, objective2, isAssigned);

      for (int i = 0; i < paretoFront.size(); i++) {
        ranks[paretoFront.get(i)] = currentRank;
        isAssigned[paretoFront.get(i)] = true;
      }

      currentRank++;
      nRanksAssigned += paretoFront.size();

    }

    return ranks;

  }

  /**
   *
   * @param alreadyUsed An array of booleans indicating which individuals have already been used, and are
   *                    no longer eligible to be considered as part of the pareto front
   *                    NOTE: To consider all individuals, pass an array filled with false values
   **/
  private static ArrayList<Integer> getParetoFront(int[] objective1, int[] objective2, boolean[] alreadyUsed) {

    // Error checking
    if (objective1.length != objective2.length || objective1.length != alreadyUsed.length)
      return null;

    // Setup
    int nIndividuals = objective1.length;
    ArrayList<Integer> individualsInFront = new ArrayList<Integer>();

    // Build pareto front
    for (int i = 0; i < nIndividuals; i++) {

      // Skip this individual if they cannot be part of this pareto front
      if (alreadyUsed[i])
        continue;

      List<Integer> individualsToRemoveFromFront = new ArrayList<Integer>();

      boolean partOfFront = true;

      for (int other : individualsInFront) {

        if (paretoDominates(objective1[i], objective2[i], objective1[other], objective2[other]))
          individualsToRemoveFromFront.add(other);
        
        else if (paretoDominates(objective1[other], objective2[other], objective1[i], objective2[i])) {
          partOfFront = false;
          break;
        }

      }

      for (Integer individual : individualsToRemoveFromFront)
        individualsInFront.remove(individual);

      if (partOfFront)
        individualsInFront.add(i);

    }

    return individualsInFront;

  }

  private static boolean paretoDominates(int x1, int y1, int x2, int y2) {

    // It does not pareto dominate if it is weaker in some way
    if (x1 < x2 || y1 < y2)
      return false;

    // It does not pareto dominate if they are equal in every respect
    if (x1 == x2 && y1 == y2)
      return false;

    // Otherwise, it must pareto dominate
    return true;

  }

}

class TestResult {

  public boolean passed;
  private String summary = "";

  public TestResult(long actual, long expected) {
    if (actual == expected)
      passed = true;
    else {
      passed = false;
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
    }
  }

  public TestResult(double actual, double expected) {
    if (actual == expected)
      passed = true;
    else {
      passed = false;
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";
    }
  }

  public TestResult(boolean actual, boolean expected) {
    passed = (actual == expected);
    
    if (!passed)
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";

  }

  public TestResult(String actual, String expected) {

    this(actual.split("\n"), expected.split("\n"));

  }

  public TestResult(String[] actual, String[] expected) {

    Arrays.sort(actual);
    Arrays.sort(expected);

    passed = Arrays.deepEquals(actual, expected);
    
    if (!passed) {

      if (TestAutomata.DIFF) {

          /* Find lines that were added */

        StringBuilder addedLines = new StringBuilder();
        List<String> listOfExpectedLines = Arrays.asList((String[]) expected.clone());

        for (String str : actual) {
          int index = listOfExpectedLines.indexOf(str);

          // Not found
          if (index == -1)
            addedLines.append(str + "\n");
          else
            listOfExpectedLines.set(index, null);

        }

          /* Find lines that were missing */

        StringBuilder missingLines = new StringBuilder();
        List<String> listOfActualLines = Arrays.asList((String[]) actual.clone());

        for (String str : expected) {
          int index = listOfActualLines.indexOf(str);

          // Not found
          if (index == -1)
            missingLines.append(str + "\n");
          else
            listOfActualLines.set(index, null);

        }

        summary = "\nADDED LINES:\n" + addedLines.toString() + "\n\nMISSING LINES:\n" + missingLines.toString() + "\n";

      } else
        summary = "\nEXPECTED:\n" + Arrays.toString(expected) + "\n\nACTUAL:\n" + Arrays.toString(actual) + "\n";

    }

  }

  public TestResult(List<Long> actual, List<Long> expected) {
    
    passed = (actual.equals(expected));
    
    if (!passed)
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";

  }

  public TestResult(ArrayList<Integer> actual, ArrayList<Integer> expected) {
    
    passed = (actual.equals(expected));
    
    if (!passed)
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";

  }

  public TestResult(List<String> actual, ArrayList<String> expected) {
    
    passed = (actual.equals(expected));
    
    if (!passed)
      summary = "\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + actual + "\n";

  }

  public TestResult(boolean[] actual, boolean[] expected) {
    
    passed = (Arrays.equals(actual, expected));

    if (!passed)
      summary = "\nEXPECTED:\n" + Arrays.toString(expected) + "\n\nACTUAL:\n" + Arrays.toString(actual) + "\n";
    
  }

  public TestResult(boolean passed) {

    this.passed = passed;

    if (!passed)
      summary = "";
    
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