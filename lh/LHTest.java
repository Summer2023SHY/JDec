/**
 * USAGE:
 * -To try all pivots: java LHTest       
 * -To specify a pivot (1-based): java LHTest -pivot=2
 *
 * NOTE: Of course, using shell redirection is the best way to automate it.
 **/

import java.util.*;

public class LHTest {

	static Scanner sc = new Scanner(System.in);
	
	public static void main(String[] args) {

      /* Check for Specified Pivot */

    Integer specifiedPivot = null;

    for (String arg : args)
      if (arg.substring(0, 7).equals("-pivot="))
        specifiedPivot = Integer.valueOf(arg.substring(7));

			/* Input */

		System.out.println("Width: ");
		int width = Integer.valueOf(sc.nextLine());

		System.out.println("Height: ");
		int height = Integer.valueOf(sc.nextLine());
	
		double[][] a = new double[height][width];
		double[][] b = new double[height][width];

		System.out.println("Grid 1: ");
		for (int y = 0; y < height; y++) {
			String[] split = sc.nextLine().split(" +");
			for (int x = 0; x < width; x++)
				a[y][x] = Integer.valueOf(split[x]);
		}

		System.out.println("Grid 2: ");
		for (int y = 0; y < height; y++) {
			String[] split = sc.nextLine().split(" +");
			for (int x = 0; x < width; x++)
				b[y][x] = Integer.valueOf(split[x]);
		}

      /* Calculations and Results */

    BimatrixGame game = new BimatrixGame(a, b);    
    BimatrixGame.ReducedGame reducedGame = game.getReducedGame();
    LemkeHowson3 lemkeHowson = new LemkeHowson3(reducedGame);

    if (specifiedPivot == null)
      for (int pivot = 1; pivot <= height; pivot++)
        tryPivot(game, reducedGame, lemkeHowson, pivot, width, height);
    else
      tryPivot(game, reducedGame, lemkeHowson, specifiedPivot, width, height);

	}

  private static void tryPivot(BimatrixGame game, BimatrixGame.ReducedGame reducedGame, LemkeHowson3 lemkeHowson, int pivot, int width, int height) {

    LHEquilibriumProfile prof = lemkeHowson.get_lemke_howson_single(pivot);

    if (prof == null)
      return;
    
    LHEquilibriumList list = new LHEquilibriumList() {

      // Overridden to remove print statement
      @Override public void addBackStrictDom(double[][] A, double[][] B, int[] map1, int[] map2, int m, int n) {
        for (LHEquilibriumProfile ep : this) {
          if ((ep.pivotList != null) && (ep.pivotList.size() > 0)) {
            HashSet<Integer> newPivotList = new HashSet<Integer>();
            for (Iterator localIterator2 = ep.pivotList.iterator(); localIterator2.hasNext();) {
              int i = ((Integer) localIterator2.next()).intValue();
              newPivotList.add(Integer.valueOf(map1[(i - 1)] + 1));
            }
            ep.pivotList = newPivotList;
          }
          double[] s1_ = new double[m];
          double[] s2_ = new double[n];
          for (int i = 0; i < ep.s1.length; i++)
            s1_[map1[i]] = ep.s1[i];
          ep.s1 = s1_;
          for (int i = 0; i < ep.s2.length; i++)
            s2_[map2[i]] = ep.s2[i];
          ep.s2 = s2_;
          ep.setPayoffs(MatrixUtilities.rightMult(A, s2_), MatrixUtilities.leftMult(s1_, B));
        }
      }
    };

    list.add(prof);
    list.addBackStrictDom(game.A, game.B, reducedGame.map1, reducedGame.map2, height, width);
    
    System.out.println(list.toReducedString());
    System.out.println();

  }
}