package automata;
import java.io.*;
import java.util.*;

public class Liu2 {

  enum FirstCriteria {

    PLANT_OVER_SPEC,
    SPEC_OVER_PLANT,
    ALTERNATING
  
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

  // static JDec jdec = new JDec();

  static int[][][][][] nInnerLoops;
  static long[][][][][] uStructureStates;
  static long[][][][][] automatonStates;
  static long[][][][][] timeElapsed;

  static Map<String, UStructure> storedUStructures = new HashMap<String, UStructure>();
  static Map<String, Long> storedTime = new HashMap<String, Long>();

  public static void main(String[] args) throws IncompatibleAutomataException {

    // Large Example
    List<Automaton> plants = new ArrayList<Automaton>();
    plants.add(duplicate(new Automaton(new File("Thesis/Large/NAME.hdr"), new File("Thesis/Large/NAME.bdy"), false)));
    List<Automaton> specs = new ArrayList<Automaton>();
    specs.add(duplicate(new Automaton(new File("Thesis/Large/NAME.hdr"), new File("Thesis/Large/NAME.bdy"), false)));
    Automaton gSigmaStar = new Automaton(new File("Thesis/Large/G_SIGMA_STAR.hdr"), new File("Thesis/Large/G_SIGMA_STAR.bdy"), false);

    // First Example
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/SENDER.hdr"), new File("Thesis/SENDER.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/RECEIVER.hdr"), new File("Thesis/RECEIVER.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/CHANNEL.hdr"), new File("Thesis/CHANNEL.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/SpecSNDR.hdr"), new File("Thesis/SpecSNDR.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/SpecRCVR.hdr"), new File("Thesis/SpecRCVR.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/SpecSEQ.hdr"), new File("Thesis/SpecSEQ.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/G_SIGMA_STAR.hdr"), new File("Thesis/G_SIGMA_STAR.bdy"), false);

    // // Second Example
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/SecondExample/SenderB.hdr"), new File("Thesis/SecondExample/SenderB.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/SecondExample/ReceiverB.hdr"), new File("Thesis/SecondExample/ReceiverB.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/SecondExample/ChannelRS.hdr"), new File("Thesis/SecondExample/ChannelRS.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/SecondExample/ChannelSR.hdr"), new File("Thesis/SecondExample/ChannelSR.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/SecondExample/Specification.hdr"), new File("Thesis/SecondExample/Specification.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/SecondExample/G_SIGMA_STAR.hdr"), new File("Thesis/SecondExample/G_SIGMA_STAR.bdy"), false);

    // Third example
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/PackagingSystem.hdr"), new File("Thesis/ThirdExample/PackagingSystem.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/Source.hdr"), new File("Thesis/ThirdExample/Source.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/Sink.hdr"), new File("Thesis/ThirdExample/Sink.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/TestUnit.hdr"), new File("Thesis/ThirdExample/TestUnit.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/PathFlowModel.hdr"), new File("Thesis/ThirdExample/PathFlowModel.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/AttachPartToAssembly.hdr"), new File("Thesis/ThirdExample/AttachPartToAssembly.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/DefineNewEvents.hdr"), new File("Thesis/ThirdExample/DefineNewEvents.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/PolishPart.hdr"), new File("Thesis/ThirdExample/PolishPart.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample/AttachCaseToAssembly.hdr"), new File("Thesis/ThirdExample/AttachCaseToAssembly.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/InBuffer.hdr"), new File("Thesis/ThirdExample/InBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/OutBuffer.hdr"), new File("Thesis/ThirdExample/OutBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/PackageBuffer.hdr"), new File("Thesis/ThirdExample/PackageBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/EnsureMatFb.hdr"), new File("Thesis/ThirdExample/EnsureMatFb.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/PolishingSequence.hdr"), new File("Thesis/ThirdExample/PolishingSequence.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/SequenceTasks.hdr"), new File("Thesis/ThirdExample/SequenceTasks.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample/AffixPart.hdr"), new File("Thesis/ThirdExample/AffixPart.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/ThirdExample/G_SIGMA_STAR.hdr"), new File("Thesis/ThirdExample/G_SIGMA_STAR.bdy"), false);

    // Third example - try #2
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/PackagingSystem.hdr"), new File("Thesis/ThirdExample2/PackagingSystem.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/Source.hdr"), new File("Thesis/ThirdExample2/Source.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/Sink.hdr"), new File("Thesis/ThirdExample2/Sink.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/TestUnit.hdr"), new File("Thesis/ThirdExample2/TestUnit.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/Interface.hdr"), new File("Thesis/ThirdExample2/Interface.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/InBuffer.hdr"), new File("Thesis/ThirdExample2/InBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/OutBuffer.hdr"), new File("Thesis/ThirdExample2/OutBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/PackageBuffer.hdr"), new File("Thesis/ThirdExample2/PackageBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/ThirdExample2/EnsureMatFb.hdr"), new File("Thesis/ThirdExample2/EnsureMatFb.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/ThirdExample2/G_SIGMA_STAR.hdr"), new File("Thesis/ThirdExample2/G_SIGMA_STAR.bdy"), false);


    // Project 1A
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1A/PackagingSystem.hdr"), new File("Thesis/Project1A/PackagingSystem.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1A/Source.hdr"), new File("Thesis/Project1A/Source.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1A/Sink.hdr"), new File("Thesis/Project1A/Sink.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1A/TestUnit.hdr"), new File("Thesis/Project1A/TestUnit.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1A/Interface.hdr"), new File("Thesis/Project1A/Interface.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1A/OutBuffer.hdr"), new File("Thesis/Project1A/OutBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1A/InBuffer.hdr"), new File("Thesis/Project1A/InBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1A/PackageBuffer.hdr"), new File("Thesis/Project1A/PackageBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1A/EnsureMatFb.hdr"), new File("Thesis/Project1A/EnsureMatFb.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/Project1A/G_SIGMA_STAR.hdr"), new File("Thesis/Project1A/G_SIGMA_STAR.bdy"), false);

    // High Level
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1AOne/PackagingSystem.hdr"), new File("Thesis/Project1AOne/PackagingSystem.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1AOne/Source.hdr"), new File("Thesis/Project1AOne/Source.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1AOne/Sink.hdr"), new File("Thesis/Project1AOne/Sink.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1AOne/TestUnit.hdr"), new File("Thesis/Project1AOne/TestUnit.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/Project1AOne/Interface.hdr"), new File("Thesis/Project1AOne/Interface.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1AOne/InBuffer.hdr"), new File("Thesis/Project1AOne/InBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1AOne/OutBuffer.hdr"), new File("Thesis/Project1AOne/OutBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1AOne/PackageBuffer.hdr"), new File("Thesis/Project1AOne/PackageBuffer.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/Project1AOne/EnsureMatFb.hdr"), new File("Thesis/Project1AOne/EnsureMatFb.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/Project1AOne/G_SIGMA_STAR.hdr"), new File("Thesis/Project1AOne/G_SIGMA_STAR.bdy"), false);

    // Low Level
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(duplicate(new Automaton(new File("Thesis/LowLevel/PathFlowModel.hdr"), new File("Thesis/LowLevel/PathFlowModel.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/LowLevel/AttachPartToAssembly.hdr"), new File("Thesis/LowLevel/AttachPartToAssembly.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/LowLevel/DefineNewEvents.hdr"), new File("Thesis/LowLevel/DefineNewEvents.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/LowLevel/PolishPart.hdr"), new File("Thesis/LowLevel/PolishPart.bdy"), false)));
    // plants.add(duplicate(new Automaton(new File("Thesis/LowLevel/AttachCaseToAssembly.hdr"), new File("Thesis/LowLevel/AttachCaseToAssembly.bdy"), false)));
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(duplicate(new Automaton(new File("Thesis/LowLevel/Interface.hdr"), new File("Thesis/LowLevel/Interface.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/LowLevel/PolishingSequence.hdr"), new File("Thesis/LowLevel/PolishingSequence.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/LowLevel/SequenceTasks.hdr"), new File("Thesis/LowLevel/SequenceTasks.bdy"), false)));
    // specs.add(duplicate(new Automaton(new File("Thesis/LowLevel/AffixPart.hdr"), new File("Thesis/LowLevel/AffixPart.bdy"), false)));
    // Automaton gSigmaStar = new Automaton(new File("Thesis/LowLevel/G_SIGMA_STAR.hdr"), new File("Thesis/LowLevel/G_SIGMA_STAR.bdy"), false);

    // Add self-loops to all plants and specifications
    addSelfLoops(plants);
    addSelfLoops(specs);

    // List<List<Automaton>> plantPerms = generateNoPerm(plants);
    // List<List<Automaton>> specPerms = generateNoPerm(specs);
    List<List<Automaton>> plantPerms = generatePerm(plants);
    List<List<Automaton>> specPerms = generatePerm(specs);
    int firstSize  = FirstCriteria.values().length;
    int secondSize = SecondCriteria.values().length;
    int thirdSize  = ThirdCriteria.values().length;
    int fourthSize = FourthCriteria.values().length;
    int nCombinations = plantPerms.size()*specPerms.size();

    System.err.println("nCombinations: " + nCombinations);

    nInnerLoops      = new int[firstSize][secondSize][thirdSize][fourthSize][nCombinations];
    uStructureStates = new long[firstSize][secondSize][thirdSize][fourthSize][nCombinations];
    automatonStates  = new long[firstSize][secondSize][thirdSize][fourthSize][nCombinations];
    timeElapsed      = new long[firstSize][secondSize][thirdSize][fourthSize][nCombinations];

    int nWays = nCombinations*firstSize*secondSize*thirdSize*fourthSize;
    int counter = 0;
    int combinationIndex = 0;

    try {

      // Automaton lPrime = gSigmaStar.duplicate();
      // for (Automaton a : plants) {
      //   System.out.println("starting " + a);
      //   lPrime = Automaton.union(lPrime, a.generateTwinPlant2(null, null), null, null);
      //   System.out.println(lPrime.getNumberOfStates());
      //   // System.out.println(lPrime.synchronizedComposition(null, null).getNumberOfStates());
      // }
      // for (Automaton a : specs) {
      //   System.out.println("starting " + a);
      //   lPrime = Automaton.union(lPrime, a.generateTwinPlant2(null, null), null, null);
      //   System.out.println(lPrime.getNumberOfStates());
      //   // System.out.println(lPrime.synchronizedComposition(null, null).getNumberOfStates());
      // }
      // System.out.println("RESULT: " + lPrime.synchronizedComposition(null, null).getNumberOfStates());
      
      outer: for (List<Automaton> plantPerm : plantPerms) {
        for (List<Automaton> specPerm : specPerms) {
          for (int first = 0; first < firstSize; first++) {
            for (int second = 0; second < secondSize; second++) {
              for (int third = 0; third < thirdSize; third++) {
                for (int fourth = 0; fourth < fourthSize; fourth++) {

                  System.err.printf("Trial %d/%d:\n", ++counter, nWays);
                  System.err.println("\tOrder of plants: " + plantPerm);
                  System.err.println("\tOrder of specifications: " + specPerm);
                  FirstCriteria firstCritera = FirstCriteria.values()[first];
                  System.err.println("\tFirst: " + firstCritera);
                  SecondCriteria secondCriteria = SecondCriteria.values()[second];
                  System.err.println("\tSecond: " + secondCriteria);
                  ThirdCriteria thirdCriteria = ThirdCriteria.values()[third];
                  System.err.println("\tThird: " + thirdCriteria);
                  FourthCriteria fourthCriteria = FourthCriteria.values()[fourth];
                  System.err.println("\tFourth: " + fourthCriteria);
                  
                  long startTime = System.nanoTime();
                  boolean result = incrementalVerification(
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
                    timeElapsed[first][second][third][fourth],
                    combinationIndex
                  );
                  long endTime = System.nanoTime();
                  long totalTime = endTime - startTime;
                  timeElapsed[first][second][third][fourth][combinationIndex] += totalTime;
                  System.err.print("TIME: " + timeElapsed[first][second][third][fourth][combinationIndex]);
                  System.err.println("\tResult: " + result);

                  System.err.println();
                }
              }
            }
          }
          combinationIndex++;
        }
      }

    } catch (IncompatibleAutomataException e) {
      e.printStackTrace();
    }


    // PRINT RESULTS
    System.out.println("Heuristics,,,,min iter,avg iter,max iter,min states in U,avg states in U,max states in U,min states in A,avg states in A,max states in A,min time (s),avg time (s),max time (s)");
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

            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;
            long totalTime = 0;

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

              minTime = Math.min(minTime, timeElapsed[first][second][third][fourth][i]);
              maxTime = Math.max(maxTime, timeElapsed[first][second][third][fourth][i]);
              totalTime += timeElapsed[first][second][third][fourth][i];

            }

            // System.out.println(FirstCriteria.values()[first] + " " +
            //                    SecondCriteria.values()[second] + " " +
            //                    ThirdCriteria.values()[third] + " " +
            //                    FourthCriteria.values()[fourth]);
            // System.out.println("# Inner Iterations:");
            // System.out.println("\tMinimum: " + minNInnerLoops);
            // System.out.println("\tAverage: " + ((double)totalNInnerLoops)/((double)nCombinations));
            // System.out.println("\tMaximum: " + maxNInnerLoops);
            // System.out.println("# States in largest U-Structure:");
            // System.out.println("\tMinimum: " + minUStructureStates);
            // System.out.println("\tAverage: " + ((double)totalUStructureStates)/((double)nCombinations));
            // System.out.println("\tMaximum: " + maxUStructureStates);
            // System.out.println("# States in largest Automaton:");
            // System.out.println("\tMinimum: " + minAutomatonStates);
            // System.out.println("\tAverage: " + ((double)totalAutomatonStates)/((double)nCombinations));
            // System.out.println("\tMaximum: " + maxAutomatonStates);
            // System.out.println("Time Elasped:");
            // System.out.println("\tMinimum: " + minTime);
            // System.out.println("\tAverage: " + ((double)totalTime)/((double)nCombinations));
            // System.out.println("\tMaximum: " + maxTime);
            // System.out.println();

            System.out.print(FirstCriteria.values()[first] + "," +
                               SecondCriteria.values()[second] + "," +
                               ThirdCriteria.values()[third] + "," +
                               FourthCriteria.values()[fourth] + ",");

            System.out.printf("%.2f,", ((double)minNInnerLoops));
            System.out.printf("%.2f,", (((double)totalNInnerLoops)/((double)nCombinations)));
            System.out.printf("%.2f,", ((double)maxNInnerLoops));

            System.out.printf("%.2f,", ((double)minUStructureStates));
            System.out.printf("%.2f,", (((double)totalUStructureStates)/((double)nCombinations)));
            System.out.printf("%.2f,", ((double)maxUStructureStates));

            System.out.printf("%.2f,", ((double)minAutomatonStates));
            System.out.printf("%.2f,", (((double)totalAutomatonStates)/((double)nCombinations)));
            System.out.printf("%.2f,", ((double)maxAutomatonStates));

            System.out.printf("%.2f,", ((double)minTime)/1000000000.0);
            System.out.printf("%.2f,", (((double)totalTime)/((double)nCombinations))/1000000000.0);
            System.out.printf("%.2f\n", ((double)maxTime)/1000000000.0);

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

  public static <E> List<List<E>> generateNoPerm(List<E> original) {

    List<List<E>> result = new ArrayList<List<E>>();
    result.add(original);
    return result;
   
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
    long[] timeElapsedData,
    int combinationIndex) throws IncompatibleAutomataException {

    // // Create duplicates in order to prevent the originals from being modified
    // plants = duplicateList(plants);
    // specs = duplicateList(specs);    

    boolean choosePlantFirst = false; // toggled before use, so it is actually true the first time

    int iterationOuter = 0;
    int iterationInner = 0;
    while (specs.size() > 0) {

      iterationOuter++;
      System.err.printf("\t\tStarting outer loop iteration #%d.\n", iterationOuter);

      Automaton kPrime = specs.get(0);
      Automaton lPrime = gSigmaStar.duplicate();

      // Temporary
      List<Automaton> automataInKPrime = new ArrayList<Automaton>();
      automataInKPrime.add(kPrime);
      List<Automaton> automataInLPrime = new ArrayList<Automaton>();
      automataInLPrime.add(lPrime);

      List<List<String>> counterExample;
      loop: while ( (counterExample = hasCounterExample(lPrime, kPrime, automataInLPrime, automataInKPrime, secondCriteria, uStructureStateData, timeElapsedData, combinationIndex)) != null ) {
        
        iterationInner++;
        nInnerLoopData[combinationIndex] = iterationInner;
        System.err.printf("\t\t\tStarting inner loop iteration #%d.\n", iterationInner);

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
              System.err.println("\t\t\t\tCan choose the following from L\\L': " + automaton);

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
              System.err.println("\t\t\t\tCan choose the following from K\\K': " + automaton);
              
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
        else if (firstCritera == FirstCriteria.SPEC_OVER_PLANT)
          choosePlantFirst = false;
        // if (firstCritera == FirstCriteria.SPEC_OVER_PLANT)
        //   choosePlantFirst = false;

        // Choose one
        if (choosePlantFirst) {
          if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.err.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            System.err.println("\t\t\t\tSize of L': " + lPrime.getNumberOfStates());
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], lPrime.getNumberOfStates());
            continue loop;
          } else if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.err.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            System.err.println("\t\t\t\tSize of K': " + kPrime.getNumberOfStates());
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], kPrime.getNumberOfStates());
            continue loop;
          }
        } else {
          if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.err.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            System.err.println("\t\t\t\tSize of K': " + kPrime.getNumberOfStates());
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], kPrime.getNumberOfStates());
            continue loop;
          } else if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.err.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            System.err.println("\t\t\t\tSize of L': " + lPrime.getNumberOfStates());
            automatonStateData[combinationIndex] = Math.max(automatonStateData[combinationIndex], lPrime.getNumberOfStates());
            continue loop;
          }
        }

        System.err.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
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

      System.err.println("\t\t# States in L': " + lPrime.getNumberOfStates());
      System.err.println("\t\t# Transitions in L': " + lPrime.getNumberOfTransitions());

      // Close files since we are finished with these automata
      // kPrime.closeFiles();
      // lPrime.closeFiles();
    
    }
    
    System.err.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
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

  public static Automaton duplicate(Automaton a) {

    File newHeaderFile = append(a.getHeaderFile(), "d");
    File newBodyFile = append(a.getBodyFile(), "d");
    return a.duplicate(newHeaderFile, newBodyFile);

  }

  public static void addSelfLoops(List<Automaton> automata) {

    for (Automaton a : automata) {
      a.addSelfLoopsForInactiveEvents();
      // jdec.createTab(a);
    }

  }

  public static File append(File file, String suffix) {
    return new File(file.getAbsolutePath() + suffix);
  }

  public static List<List<String>> hasCounterExample(Automaton lPrime, Automaton kPrime, List<Automaton> automataInLPrime, List<Automaton> automataInKPrime, SecondCriteria secondCriteria, long[] uStructureStateData, long[] timeElapsed, int combinationIndex) throws IncompatibleAutomataException {

    UStructure uStructure;

    String encoded = encode(automataInLPrime, automataInKPrime);
    UStructure stored = storedUStructures.get(encoded);
    if (stored != null) {
      uStructure = stored;
      timeElapsed[combinationIndex] += storedTime.get(encoded);
    } else {
      long startTime = System.nanoTime();
      Automaton automaton = kPrime.generateTwinPlant2(null, null);
      uStructure = Automaton.union(lPrime, automaton, null, null).synchronizedComposition(null, null);
      long endTime = System.nanoTime();
      storedUStructures.put(encoded, uStructure);
      storedTime.put(encoded, (endTime - startTime));
    }

    long nStates = uStructure.getNumberOfStates();
    uStructureStateData[combinationIndex] = Math.max(uStructureStateData[combinationIndex], nStates);
    System.err.println("\n\t\t\t\t# States in U: " + nStates);

    List<List<String>> counterExample;

    if (secondCriteria == SecondCriteria.LONGEST_COUNTER_EXAMPLE) {
      counterExample = uStructure.findCounterExample(false);
      System.err.println("\t\t\t\tUsing 'longest' shortest counter-example: " + counterExample);
    } else {
      counterExample = uStructure.findCounterExample(true);
      System.err.println("\t\t\t\tUsing shortest counter-example: " + counterExample);
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

    return fileNames.toString();

  }

}

