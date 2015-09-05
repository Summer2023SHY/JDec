import java.io.*;
import java.util.*;

public class Liu {

  // static JDec jdec = new JDec();

  public static void main(String[] args) throws IncompatibleAutomataException {
    perms();
    // permsFirstAutomaton();
  }

	public static void perms() {

    // Plants
    List<Automaton> plants = new ArrayList<Automaton>();
    plants.add(new Automaton(new File("Thesis/SecondExample/SenderB.hdr"), new File("Thesis/SecondExample/SenderB.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ReceiverB.hdr"), new File("Thesis/SecondExample/ReceiverB.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ChannelRS.hdr"), new File("Thesis/SecondExample/ChannelRS.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ChannelSR.hdr"), new File("Thesis/SecondExample/ChannelSR.bdy"), false));

    // Specifications
    List<Automaton> specs = new ArrayList<Automaton>();
    specs.add(new Automaton(new File("Thesis/SecondExample/Specification.hdr"), new File("Thesis/SecondExample/Specification.bdy"), false));

    // // Plants
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(new Automaton(new File("Thesis/SENDER.hdr"), new File("Thesis/SENDER.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/RECEIVER.hdr"), new File("Thesis/RECEIVER.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/CHANNEL.hdr"), new File("Thesis/CHANNEL.bdy"), false));

    // // Specifications
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(new Automaton(new File("Thesis/SpecSNDR.hdr"), new File("Thesis/SpecSNDR.bdy"), false));
    // specs.add(new Automaton(new File("Thesis/SpecRCVR.hdr"), new File("Thesis/SpecRCVR.bdy"), false));
    // specs.add(new Automaton(new File("Thesis/SpecSEQ.hdr"), new File("Thesis/SpecSEQ.bdy"), false));

    // G{Sigma*}
    Automaton gSigmaStar = new Automaton(new File("Thesis/G_SIGMA_STAR.hdr"), new File("Thesis/G_SIGMA_STAR.bdy"), false);

    try {
      
      // All perms
      List<List<Automaton>> plantPerms = generatePerm(plants);
      List<List<Automaton>> specPerms = generatePerm(specs);
      int nWays = plantPerms.size()*specPerms.size()*2*2*2;
      int counter = 0;
      for (List<Automaton> plantPerm : plantPerms)
        for (List<Automaton> specPerm : specPerms)
          for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
              for (int k = 0; k < 2; k++) {
                System.out.printf("Trial %d/%d:\n", ++counter, nWays);
                System.out.println("\tOrder of plants: " + plantPerm);
                System.out.println("\tOrder of specifications: " + specPerm);
                boolean choosePlantFirst = (i==1);
                boolean insertSpecAtStart = (j==1);
                boolean smallestNumberOfStepsToCounterExample = (k==1);
                System.out.println("\tChoose plant before specification: " + choosePlantFirst);
                System.out.println("\tFirst automaton will be chosen");
                System.out.println("\tInsert specification at start of list: " + insertSpecAtStart);
                System.out.println("\tChoosing automata with smallest number of steps needed to reject counter-example: " + smallestNumberOfStepsToCounterExample);
                System.out.println("\tResult: " + incrementalVerification(new ArrayList<Automaton>(plantPerm), new ArrayList<Automaton>(specPerm), gSigmaStar, choosePlantFirst, insertSpecAtStart, smallestNumberOfStepsToCounterExample));
                System.out.println();
              }

    } catch (IncompatibleAutomataException e) {
      e.printStackTrace();
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
  public static boolean incrementalVerification(List<Automaton> plants, List<Automaton> specs, Automaton gSigmaStar, boolean choosePlantFirst, boolean insertSpecAtStart, boolean smallestNumberOfStepsToCounterExample) throws IncompatibleAutomataException {

    // Create duplicates in order to prevent the originals from being modified
    plants = duplicateList(plants);
    specs = duplicateList(specs);

    // Add self-loops to all plants and specifications
    addSelfLoops(plants);
    addSelfLoops(specs);

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
      loop: while ( (counterExample = hasCounterExample(lPrime, kPrime)) != null ) {
        
        iterationInner++;
        System.out.printf("\t\t\tStarting inner loop iteration #%d.\n", iterationInner);

        // Print out our options (and store them)
        int[] stepsForPlants = new int[plants.size()];
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
            }
          }
        }
        int[] stepsForSpecs = new int[specs.size()];
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
            }
          }
        }

        Integer chosenPlant = (smallestNumberOfStepsToCounterExample ? shortestStepsForPlants : longestStepsForPlants);
        Integer chosenSpec  = (smallestNumberOfStepsToCounterExample ? shortestStepsForSpecs  : longestStepsForSpecs );

        // Choose one
        if (choosePlantFirst) {
          if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            continue loop;
          } else if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            continue loop;
          }
        } else {
          if (chosenSpec != null) {
            Automaton automaton = specs.get(chosenSpec);
            kPrime = Automaton.intersection(kPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
            automataInKPrime.add(automaton);
            continue loop;
          } else if (chosenPlant != null) {
            Automaton automaton = plants.get(chosenPlant);
            lPrime = Automaton.intersection(lPrime, automaton, null, null);
            System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
            automataInLPrime.add(automaton);
            continue loop;
          }
        }

        System.out.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
        return false;

      }

      for (Automaton a : automataInKPrime)
        if (!plants.contains(a)) {
          if (insertSpecAtStart)
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

  public static List<Automaton> duplicateList(List<Automaton> automata) {

    List<Automaton> duplicatedAutomata = new ArrayList<Automaton>();

    for (Automaton a : automata) {
      File newHeaderFile = append(a.getHeaderFile(), "d");
      File newBodyFile = append(a.getBodyFile(), "d");
      duplicatedAutomata.add(a.duplicate(newHeaderFile, newBodyFile));
    }

    return duplicatedAutomata;

  }

  public static void addSelfLoops(List<Automaton> automata) {

    for (Automaton a : automata)
      a.addSelfLoopsForInactiveEvents();

  }

  public static File append(File file, String suffix) {
    return new File(file.getAbsolutePath() + suffix);
  }

  public static List<List<String>> hasCounterExample(Automaton lPrime, Automaton kPrime) throws IncompatibleAutomataException {

    Automaton automaton = kPrime.generateTwinPlant2(null, null);
    // jdec.createTab(automaton);
    UStructure uStructure = Automaton.union(lPrime, automaton, null, null).synchronizedComposition(null, null);
    
    System.out.println("\n\t\t\t\t# States in U: " + uStructure.getNumberOfStates());
    System.out.println("\t\t\t\t# Transitions in U: " + uStructure.getNumberOfTransitions());

    List<List<String>> counterExample = uStructure.findCounterExample(false);
    System.out.println("\t\t\t\tUsing 'longest' shortest counter-example: " + counterExample);

    // List<List<String>> counterExample = uStructure.findCounterExample(true);
    // System.out.println("\t\t\t\tUsing shortest counter-example: " + counterExample);

    return counterExample;

  }


/**






**/


    public static void permsFirstAutomaton() {

      // Plants
    List<Automaton> plants = new ArrayList<Automaton>();
    plants.add(new Automaton(new File("Thesis/SecondExample/SenderB.hdr"), new File("Thesis/SecondExample/SenderB.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ReceiverB.hdr"), new File("Thesis/SecondExample/ReceiverB.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ChannelRS.hdr"), new File("Thesis/SecondExample/ChannelRS.bdy"), false));
    plants.add(new Automaton(new File("Thesis/SecondExample/ChannelSR.hdr"), new File("Thesis/SecondExample/ChannelSR.bdy"), false));

    // Specifications
    List<Automaton> specs = new ArrayList<Automaton>();
    specs.add(new Automaton(new File("Thesis/SecondExample/Specification.hdr"), new File("Thesis/SecondExample/Specification.bdy"), false));

    // // Plants
    // List<Automaton> plants = new ArrayList<Automaton>();
    // plants.add(new Automaton(new File("Thesis/SENDER.hdr"), new File("Thesis/SENDER.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/RECEIVER.hdr"), new File("Thesis/RECEIVER.bdy"), false));
    // plants.add(new Automaton(new File("Thesis/CHANNEL.hdr"), new File("Thesis/CHANNEL.bdy"), false));

    // // Specifications
    // List<Automaton> specs = new ArrayList<Automaton>();
    // specs.add(new Automaton(new File("Thesis/SpecSNDR.hdr"), new File("Thesis/SpecSNDR.bdy"), false));
    // specs.add(new Automaton(new File("Thesis/SpecRCVR.hdr"), new File("Thesis/SpecRCVR.bdy"), false));
    // specs.add(new Automaton(new File("Thesis/SpecSEQ.hdr"), new File("Thesis/SpecSEQ.bdy"), false));

    // G{Sigma*}
    Automaton gSigmaStar = new Automaton(new File("Thesis/G_SIGMA_STAR.hdr"), new File("Thesis/G_SIGMA_STAR.bdy"), false);

    try {
      
      // All perms
      List<List<Automaton>> plantPerms = generatePerm(plants);
      List<List<Automaton>> specPerms = generatePerm(specs);
      int nWays = plantPerms.size()*specPerms.size()*2  ;
      int counter = 0;
      for (List<Automaton> plantPerm : plantPerms)
        for (List<Automaton> specPerm : specPerms)
          // for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) {
              // for (int k = 0; k < 2; k++) {
                System.out.printf("Trial %d/%d:\n", ++counter, nWays);
                System.out.println("\tOrder of plants: " + plantPerm);
                System.out.println("\tOrder of specifications: " + specPerm);
                // boolean choosePlantFirst = (i==1);
                boolean insertSpecAtStart = (j==1);
                // boolean smallestNumberOfStepsToCounterExample = (k==1);
                // System.out.println("\tChoose plant before specification: " + choosePlantFirst);
                System.out.println("\tFirst automaton will be chosen");
                System.out.println("\tInsert specification at start of list: " + insertSpecAtStart);
                // System.out.println("\tChoosing automata with smallest number of steps needed to reject counter-example: " + smallestNumberOfStepsToCounterExample);
                System.out.println("\tResult: " + incrementalVerificationChooseFirstAutomaton(new ArrayList<Automaton>(plantPerm), new ArrayList<Automaton>(specPerm), gSigmaStar, /*choosePlantFirst,*/ insertSpecAtStart/*, smallestNumberOfStepsToCounterExample*/));
                System.out.println();
              }

    } catch (IncompatibleAutomataException e) {
      e.printStackTrace();
    }

  }

  /**
   * NOTE: All plants and specs must have the entire alphabet of events listed (even if they are inactive
   *       in that particular automaton. This is not currently being checked for.
   * NOTE: Duplicates are being made in temporary files so as to not modify the originals.
   **/
  public static boolean incrementalVerificationChooseFirstAutomaton(List<Automaton> plants, List<Automaton> specs, Automaton gSigmaStar, /*boolean pickShortestCounterExample, */boolean insertSpecAtStart/*, boolean smallestNumberOfStepsToCounterExample*/) throws IncompatibleAutomataException {

    // Create duplicates in order to prevent the originals from being modified
    plants = duplicateList(plants);
    specs = duplicateList(specs);

    // Add self-loops to all plants and specifications
    addSelfLoops(plants);
    addSelfLoops(specs);

    boolean choosePlantFirst = false; // this is toggled before first used so we actually choose a plant first

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
      loop: while ( (counterExample = hasCounterExample(lPrime, kPrime)) != null ) {
        
        iterationInner++;
        System.out.printf("\t\t\tStarting inner loop iteration #%d.\n", iterationInner);

        choosePlantFirst = !choosePlantFirst;

        // Choose one
        if (choosePlantFirst) {

          for (Automaton automaton : plants) {
            if (!automataInLPrime.contains(automaton) && automaton.acceptsCounterExample(counterExample) != -1) {
              lPrime = Automaton.intersection(lPrime, automaton, null, null);
              System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
              automataInLPrime.add(automaton);
              continue loop;
            }
          }

          for (Automaton automaton : specs) {
            if (!automataInKPrime.contains(automaton) && automaton.acceptsCounterExample(counterExample) != -1) {
              kPrime = Automaton.intersection(kPrime, automaton, null, null);
              System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
              automataInKPrime.add(automaton);
              continue loop;
            }
          }

        } else {

          for (Automaton automaton : specs) {
            if (!automataInKPrime.contains(automaton) && automaton.acceptsCounterExample(counterExample) != -1) {
              kPrime = Automaton.intersection(kPrime, automaton, null, null);
              System.out.println("\t\t\t\tPicking automaton from K\\K': " + automaton);
              automataInKPrime.add(automaton);
              continue loop;
            }
          }

          for (Automaton automaton : plants) {
            if (!automataInLPrime.contains(automaton) && automaton.acceptsCounterExample(counterExample) != -1) {
              lPrime = Automaton.intersection(lPrime, automaton, null, null);
              System.out.println("\t\t\t\tPicking automaton from L\\L': " + automaton);
              automataInLPrime.add(automaton);
              continue loop;
            }
          }

        }

        System.out.printf("\tRequired %d outer iterations and a total of %d inner iterations.\n", iterationOuter, iterationInner);
        return false;

      }

      for (Automaton a : automataInKPrime)
        if (!plants.contains(a)) {
          if (insertSpecAtStart)
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

}

