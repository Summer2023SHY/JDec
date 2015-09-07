import java.io.*;
import java.util.*;

public class Liu2 {

  enum FirstCriteria {

    PLANT_OVER_SPEC
    // SPEC_OVER_PLANT, DON'T FORGET TO FIX BELOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // ALTERNATING;
  
  }

  enum SecondCriteria {

    SHORTEST_COUNTER_EXAMPLE,
    LONGEST_COUNTER_EXAMPLE

  }

  enum ThirdCriteria {

    INSERT_SPECS_AT_START,
    INSERT_SPECS_AT_END,

  }

  enum FourthCriteria {

    FIRST_MATCH,
    MIN_TRANSITIONS,
    MIN_STATES,
    SHORTEST_REJECTION,
    LONGEST_REJECTION

  }

  static int[][][][][] nInnerLoops;
  static long[][][][][] uStructureStates;
  static long[][][][][] automatonStates;

  static Map<String, UStructure> storedUStructures = new HashMap<String, UStructure>();

  public static void main(String[] args) throws IncompatibleAutomataException {

    // First Example
    List<Automaton> plants = new ArrayList<Automaton>();
    plants.add(new Automaton(new File("Thesis/SENDER.hdr"), new File("Thesis/SENDER.bdy"), false));
    plants.add(new Automaton(new File("Thesis/RECEIVER.hdr"), new File("Thesis/RECEIVER.bdy"), false));
    plants.add(new Automaton(new File("Thesis/CHANNEL.hdr"), new File("Thesis/CHANNEL.bdy"), false));
    List<Automaton> specs = new ArrayList<Automaton>();
    specs.add(new Automaton(new File("Thesis/SpecSNDR.hdr"), new File("Thesis/SpecSNDR.bdy"), false));
    specs.add(new Automaton(new File("Thesis/SpecRCVR.hdr"), new File("Thesis/SpecRCVR.bdy"), false));
    specs.add(new Automaton(new File("Thesis/SpecSEQ.hdr"), new File("Thesis/SpecSEQ.bdy"), false));
    Automaton gSigmaStar = new Automaton(new File("Thesis/G_SIGMA_STAR.hdr"), new File("Thesis/G_SIGMA_STAR.bdy"), false);

    // // Second Example
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(new Automaton(new File("Thesis/SecondExample/SenderB.hdr"), new File("Thesis/SecondExample/SenderB.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/SecondExample/ReceiverB.hdr"), new File("Thesis/SecondExample/ReceiverB.bdy"), false));
    // DON'T FORGET TO CHANGE ARRAY SIZE!!!!!
    // plants.add(new Automaton(new File("Thesis/SecondExample/ChannelRS.hdr"), new File("Thesis/SecondExample/ChannelRS.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/SecondExample/ChannelSR.hdr"), new File("Thesis/SecondExample/ChannelSR.bdy"), false));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(new Automaton(new File("Thesis/SecondExample/Specification.hdr"), new File("Thesis/SecondExample/Specification.bdy"), false));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/SecondExample/G_SIGMA_STAR.hdr"), new File("Thesis/SecondExample/G_SIGMA_STAR.bdy"), false);


    // Add self-loops to all plants and specifications
    addSelfLoops(plants);
    addSelfLoops(specs);

    List<List<Automaton>> plantPerms = generatePerm(plants);
    List<List<Automaton>> specPerms = generatePerm(specs);
    int firstSize  = FirstCriteria.values().length;
    int secondSize = SecondCriteria.values().length;
    int thirdSize  = ThirdCriteria.values().length;
    int fourthSize = FourthCriteria.values().length;
    int nCombinations = plantPerms.size()*specPerms.size();

    nInnerLoops      = new int[firstSize][secondSize][thirdSize][fourthSize][nCombinations];
    uStructureStates = new long[firstSize][secondSize][thirdSize][fourthSize][nCombinations];
    automatonStates  = new long[firstSize][secondSize][thirdSize][fourthSize][nCombinations];

    int nWays = nCombinations*firstSize*secondSize*thirdSize*fourthSize;
    int counter = 0;
    int combinationIndex = 0;

    System.out.println("!!!" + nCombinations);

    try {
      
      for (List<Automaton> plantPerm : plantPerms) {
        for (List<Automaton> specPerm : specPerms) {
          for (int first = 0; first < firstSize; first++) {
            for (int second = 0; second < secondSize; second++) {
              for (int third = 0; third < thirdSize; third++) {
                for (int fourth = 0; fourth < fourthSize; fourth++) {
                  System.out.printf("Trial %d/%d:\n", ++counter, nWays);
                  System.out.println("\tOrder of plants: " + plantPerm);
                  System.out.println("\tOrder of specifications: " + specPerm);
                  FirstCriteria firstCritera = FirstCriteria.values()[first];
                  System.out.println("\tFirst: " + firstCritera);
                  SecondCriteria secondCriteria = SecondCriteria.values()[second];
                  System.out.println("\tSecond: " + secondCriteria);
                  ThirdCriteria thirdCriteria = ThirdCriteria.values()[third];
                  System.out.println("\tThird: " + thirdCriteria);
                  FourthCriteria fourthCriteria = FourthCriteria.values()[fourth];
                  System.out.println("\tFourth: " + fourthCriteria);
                  
                  System.out.println("\tResult: " + incrementalVerification(
                      new ArrayList<Automaton>(plantPerm),
                      new ArrayList<Automaton>(specPerm),
                      gSigmaStar,
                      firstCritera,
                      secondCriteria,
                      thirdCriteria,
                      fourthCriteria,
                      nInnerLoops[first][second][third][fourth],
                      uStructureStates[first][second][third][fourth],
                      automatonStates[first][second][third][fourth],
                      combinationIndex
                    )
                  );

                  System.out.println();
                }
              }
            }
          }
          combinationIndex++;
        }
        combinationIndex++;
      }

    } catch (IncompatibleAutomataException e) {
      e.printStackTrace();
    }


    // PRINT RESULTS
    for (int first = 0; first < firstSize; first++)
      for (int second = 0; second < secondSize; second++)
        for (int third = 0; third < thirdSize; third++)
          for (int fourth = 0; fourth < fourthSize; fourth++) {

            long minUStructureStates = Long.MAX_VALUE;
            long maxUStructureStates = Long.MIN_VALUE;
            long totalUStructureStates = 0;

            long minAutomatonStates = Long.MAX_VALUE;
            long maxAutomatonStates = Long.MIN_VALUE;
            long totalAutomatonStates = 0;

            long minNInnerLoops = Long.MAX_VALUE;
            long maxNInnerLoops = Long.MIN_VALUE;
            long totalNInnerLoops = 0;

            for (int i = 0; i < nCombinations; i++) {

              minUStructureStates = Math.min(minUStructureStates, uStructureStates[first][second][third][fourth][i]);
              maxUStructureStates = Math.max(maxUStructureStates, uStructureStates[first][second][third][fourth][i]);
              totalUStructureStates += uStructureStates[first][second][third][fourth][i];

              minAutomatonStates = Math.min(minAutomatonStates, automatonStates[first][second][third][fourth][i]);
              maxAutomatonStates = Math.max(maxAutomatonStates, automatonStates[first][second][third][fourth][i]);
              totalAutomatonStates += automatonStates[first][second][third][fourth][i];

              minNInnerLoops = Math.min(minNInnerLoops, nInnerLoops[first][second][third][fourth][i]);
              maxNInnerLoops = Math.max(maxNInnerLoops, nInnerLoops[first][second][third][fourth][i]);
              totalNInnerLoops += nInnerLoops[first][second][third][fourth][i];

            }

            System.out.println(FirstCriteria.values()[first] + " " +
                               SecondCriteria.values()[second] + " " +
                               ThirdCriteria.values()[third] + " " +
                               FourthCriteria.values()[fourth]);

            System.out.println("# Inner Iterations:");
            System.out.println("\tMinimum: " + minNInnerLoops);
            System.out.println("\tAverage: " + ((double)totalNInnerLoops)/((double)nCombinations));
            System.out.println("\tMaximum: " + maxNInnerLoops);

            System.out.println("# States in largest U-Structure:");
            System.out.println("\tMinimum: " + minUStructureStates);
            System.out.println("\tAverage: " + ((double)totalUStructureStates)/((double)nCombinations));
            System.out.println("\tMaximum: " + maxUStructureStates);

            System.out.println("# States in largest Automaton:");
            System.out.println("\tMinimum: " + minAutomatonStates);
            System.out.println("\tAverage: " + ((double)totalAutomatonStates)/((double)nCombinations));
            System.out.println("\tMaximum: " + maxAutomatonStates);
            
            System.out.println();

          }

  }

  public static <E> List<List<E>> generatePerm(List<E> original) {

    if (original.size() == 0) { 
      List<List<E>> result = new ArrayList<List<E>>();
      result.add(new ArrayList<E>());
      return result;
    }

    E firstElement = original.remove(0);
    List<List<E>> returnValue = new ArrayList<List<E>>();
    List<List<E>> permutations = generatePerm(original);
    for (List<E> smallerPermutated : permutations) {
      for (int index = 0; index <= smallerPermutated.size(); index++) {
        List<E> temp = new ArrayList<E>(smallerPermutated);
        temp.add(index, firstElement);
        returnValue.add(temp);
      }
    }
    
    return returnValue;
 }

  /**
   * NOTE: All plants and specs must have the entire alphabet of events listed (even if they are inactive
   *       in that particular automaton. This is not currently being checked for.
   * NOTE: Duplicates are being made in temporary files so as to not modify the originals.
   **/
  public static boolean incrementalVerification(
    List<Automaton> plants,
    List<Automaton> specs,
    Automaton gSigmaStar,
    FirstCriteria firstCritera,
    SecondCriteria secondCriteria,
    ThirdCriteria thirdCriteria,
    FourthCriteria fourthCriteria,
    int[] nInnerLoopData,
    long[] uStructureStateData,
    long[] automatonStateData,
    int combinationIndex) throws IncompatibleAutomataException {

    // // Create duplicates in order to prevent the originals from being modified
    // plants = duplicateList(plants);
    // specs = duplicateList(specs);    

    boolean choosePlantFirst = false; // toggled before use, so it is actually true the first time

    int iterationOuter = 0;
    int iterationInner = 0;
    while (specs.size() > 0) {

      iterationOuter++;
      System.out.printf("\t\tStarting outer loop iteration #%d.\n", iterationOuter);

      Automaton kPrime = specs.get(0);
      Automaton lPrime = gSigmaStar.duplicate();

      // Temporary
      List<Automaton> automataInKPrime = new ArrayList<Automaton>();
      automataInKPrime.add(kPrime);
      List<Automaton> automataInLPrime = new ArrayList<Automaton>();
      automataInLPrime.add(lPrime);

      List<List<String>> counterExample;
      loop: while ( (counterExample = hasCounterExample(lPrime, kPrime, automataInLPrime, automataInKPrime, secondCriteria, uStructureStateData, combinationIndex)) != null ) {
        
        iterationInner++;
        nInnerLoopData[combinationIndex] = iterationInner;
        System.out.printf("\t\t\tStarting inner loop iteration #%d.\n", iterationInner);

        Integer firstMatch = null;

        // Print out our options (and store them)
        int[] stepsForPlants = new int[plants.size()];
        long[] transitionsForPlants = new long[plants.size()];
        Integer minTransitionsForPlants = null;
        long[] statesForPlants = new long[plants.size()];
        Integer minStatesForPlants = null;
        Integer firstMatchForPlants = null;
        Integer shortestStepsForPlants = null;
        Integer longestStepsForPlants  = null;
        for (int i = 0; i < plants.size(); i++) {
          Automaton automaton = plants.get(i);
          if (!automataInLPrime.contains(automaton)) {
            stepsForPlants[i] = automaton.acceptsCounterExample(counterExample);
            if (stepsForPlants[i] != -1) {
              System.out.println("\t\t\t\tCan choose the following from L\\L': " + automaton);

              if (shortestStepsForPlants == null || stepsForPlants[i] < stepsForPlants[shortestStepsForPlants])
                shortestStepsForPlants = i;
              if (longestStepsForPlants  == null || stepsForPlants[i] > stepsForPlants[longestStepsForPlants] )
                longestStepsForPlants = i;

              long nTransitions = automaton.getNumberOfTransitions();
              if (minTransitionsForPlants == null || nTransitions < transitionsForPlants[minTransitionsForPlants])
                minTransitionsForPlants = i;

              long nStates = automaton.getNumberOfStates();
              if (minStatesForPlants == null || nStates < statesForPlants[minStatesForPlants])
                minStatesForPlants = i;

              if (firstMatchForPlants == null)
                firstMatchForPlants = i;
              
            }
          }
        }
        int[] stepsForSpecs = new int[specs.size()];
        long[] transitionsForSpecs = new long[specs.size()];
        Integer minTransitionsForSpecs = null;
        long[] statesForSpecs = new long[specs.size()];
        Integer minStatesForSpecs = null;
        Integer firstMatchForSpecs = null;
        Integer shortestStepsForSpecs = null;
        Integer longestStepsForSpecs  = null;
        for (int i = 0; i < specs.size(); i++) {
          Automaton automaton = specs.get(i);
          if (!automataInKPrime.contains(automaton)) {
            stepsForSpecs[i] = automaton.acceptsCounterExample(counterExample);
            if (stepsForSpecs[i] != -1) {
              System.out.println("\t\t\t\tCan choose the following from K\\K': " + automaton);
              
              if (shortestStepsForSpecs == null || stepsForSpecs[i] < stepsForSpecs[shortestStepsForSpecs])
                shortestStepsForSpecs = i;
              if (longestStepsForSpecs  == null || stepsForSpecs[i] > stepsForSpecs[longestStepsForSpecs] )
                longestStepsForSpecs = i;

              long nTransitions = automaton.getNumberOfTransitions();
              if (minTransitionsForSpecs == null || nTransitions < transitionsForSpecs[minTransitionsForSpecs])
                minTransitionsForSpecs = i;

              long nStates = automaton.getNumberOfStates();
              if (minStatesForSpecs == null || nStates < statesForPlants[minStatesForSpecs])
                minStatesForSpecs = i;

              if (firstMatchForSpecs == null)
                firstMatchForSpecs = i;

            }
          }
        }

        Integer chosenPlant = null, chosenSpec = null;

        switch (fourthCriteria) {

          case SHORTEST_REJECTION:
            chosenPlant = shortestStepsForPlants;
            chosenSpec = shortestStepsForSpecs;
            break;

          case LONGEST_REJECTION:
            chosenPlant = longestStepsForPlants;
            chosenSpec = longestStepsForSpecs;
            break;

          case MIN_STATES:
            chosenPlant = minStatesForPlants;
            chosenSpec = minStatesForSpecs;
            break;

          case MIN_TRANSITIONS:
            chosenPlant = minTransitionsForPlants;
            chosenSpec = minTransitionsForSpecs;
            break;

          case FIRST_MATCH:
            chosenPlant = firstMatchForPlants;
            chosenSpec = firstMatchForSpecs;
            break;

          default:
            System.err.println("THIS SHOULD BE UNREACHABLE!!!!");
            break;

        }

        // Alternate between plant first and spec first (doesn't matter unless firstCriteria == FirstCriteria.ALTERNATING)
        choosePlantFirst = !choosePlantFirst;
        if (firstCritera == FirstCriteria.PLANT_OVER_SPEC)
          choosePlantFirst = true;
        // else if (firstCritera == FirstCriteria.SPEC_OVER_PLANT)
        //   choosePlantFirst = false;

        // Choose one
        if (choosePlantFirst) {
          if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], lPrime.getNumberOfStates());
            continue loop;
          } else if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], kPrime.getNumberOfStates());
            continue loop;
          }
        } else {
          if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], kPrime.getNumberOfStates());
            continue loop;
          } else if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], lPrime.getNumberOfStates());
            continue loop;
          }
        }

        System.out.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
        return false;

      }

      for (Automaton a : automataInKPrime)
        if (!plants.contains(a)) {
          if (thirdCriteria == ThirdCriteria.INSERT_SPECS_AT_START)
            plants.add(0, a);
          else
            plants.add(a);
        }
      specs.removeAll(automataInKPrime);

      System.out.println("\t\t# States in L': " + lPrime.getNumberOfStates());
      System.out.println("\t\t# Transitions in L': " + lPrime.getNumberOfTransitions());
    
    }
    
    System.out.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
    return true;

  }

  // public static List<Automaton> duplicateList(List<Automaton> automata) {

  //   List<Automaton> duplicatedAutomata = new ArrayList<Automaton>();

  //   for (Automaton a : automata) {
  //     File newHeaderFile = append(a.getHeaderFile(), "d");
  //     File newBodyFile = append(a.getBodyFile(), "d");
  //     duplicatedAutomata.add(a.duplicate(newHeaderFile, newBodyFile));
  //   }

  //   return duplicatedAutomata;

  // }

  public static void addSelfLoops(List<Automaton> automata) {

    for (Automaton a : automata)
      a.addSelfLoopsForInactiveEvents();

  }

  public static File append(File file, String suffix) {
    return new File(file.getAbsolutePath() + suffix);
  }

  public static List<List<String>> hasCounterExample(Automaton lPrime, Automaton kPrime, List<Automaton> automataInLPrime, List<Automaton> automataInKPrime, SecondCriteria secondCriteria, long[] uStructureStateData, int combinationIndex) throws IncompatibleAutomataException {

    UStructure uStructure;

    String encoded = encode(automataInLPrime, automataInKPrime);
    UStructure stored = storedUStructures.get(encoded);
    if (stored != null) {
      uStructure = stored;
    } else {
      Automaton automaton = kPrime.generateTwinPlant2(null, null);
      uStructure = Automaton.union(lPrime, automaton, null, null).synchronizedComposition(null, null);
      storedUStructures.put(encoded, uStructure);
    }

    long nStates = uStructure.getNumberOfStates();
    uStructureStateData[combinationIndex] = Math.max(uStructureStateData[combinationIndex], nStates);
    System.out.println("\n\t\t\t\t# States in U: " + nStates);

    List<List<String>> counterExample;

    if (secondCriteria == SecondCriteria.LONGEST_COUNTER_EXAMPLE) {
      counterExample = uStructure.findCounterExample(false);
      System.out.println("\t\t\t\tUsing 'longest' shortest counter-example: " + counterExample);
    } else {
      counterExample = uStructure.findCounterExample(true);
      System.out.println("\t\t\t\tUsing shortest counter-example: " + counterExample);
    }

    return counterExample;

  }

  public static String encode(List<Automaton> list1, List<Automaton> list2) {

    List<String> fileNames = new ArrayList<String>();

    for (Automaton a : list1)
      if (a.getHeaderFile().getName().indexOf(".hdr") > -1)
        fileNames.add(a.getHeaderFile().getName());
    for (Automaton a : list2)
      if (a.getHeaderFile().getName().indexOf(".hdr") > -1)
        fileNames.add(a.getHeaderFile().getName());

    Collections.sort(fileNames);

    // System.out.println("encoded: " + fileNames.toString());

    return fileNames.toString();

  }

}

