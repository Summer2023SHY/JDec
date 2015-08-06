import java.util.*;

public class LHTest {
	
	public static void main(String[] args) {
	
		// double[][] a = {
		// 	{100, 100, 100,   0},
		// 	{ 50,  50,   0,   0},
		// 	{ 25,   0,   0,  25},
		// 	{  0,   0, 500,   0}
		// };

		// double[][] b = {
		// 	{100,  50,  25,   0},
		// 	{100,  50,   0,   0},
		// 	{100,   0,   0, 500},
		// 	{  0,   0,  25,   0}
		// };

		double[][] a = {
			{1, 3, 0},
			{0, 0, 2},
			{2, 1, 1}
		};

		double[][] b = {
			{2, 1, 0},
			{1, 3, 1},
			{0, 0, 3}
		};

		int width = 3, height = 3;

	    BimatrixGame game = new BimatrixGame(a, b);    
	    BimatrixGame.ReducedGame reducedGame = game.getReducedGame();
	    LemkeHowson3 LH3 = new LemkeHowson3(reducedGame);

	    for (int pivot = 1; pivot <= height; pivot++) {

		    LHEquilibriumProfile prof = LH3.get_lemke_howson_single(pivot);

		    if (prof == null)
		      return;
		    
		    LHEquilibriumList list = new LHEquilibriumList() {

		    	// Overridden to remove print statement
		    	@Override public void addBackStrictDom(double[][] A, double[][] B, int[] map1, int[] map2, int m, int n) {
				    for (LHEquilibriumProfile ep : this)
				    {
				      if ((ep.pivotList != null) && (ep.pivotList.size() > 0))
				      {
				        HashSet<Integer> newPivotList = new HashSet<Integer>();
				        for (Iterator localIterator2 = ep.pivotList.iterator(); localIterator2.hasNext();)
				        {
				          int i = ((Integer)localIterator2.next()).intValue();
				          newPivotList.add(Integer.valueOf(map1[(i - 1)] + 1));
				        }
				        ep.pivotList = newPivotList;
				      }
				      double[] s1_ = new double[m];
				      double[] s2_ = new double[n];
				      for (int i = 0; i < ep.s1.length; i++) {
				        s1_[map1[i]] = ep.s1[i];
				      }
				      ep.s1 = s1_;
				      for (int i = 0; i < ep.s2.length; i++) {
				        s2_[map2[i]] = ep.s2[i];
				      }
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
}