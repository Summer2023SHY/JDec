import java.util.*;

public class ProtocolVector {

	private NashCommunicationData[][] communications;
  private double[] totalValue;
		
	public ProtocolVector(Set<NashCommunicationData> protocol, int nControllers) {

    communications = new NashCommunicationData[nControllers][];
		totalValue = new double[nControllers];

		for (int i = 0; i < nControllers; i++) {

			List<NashCommunicationData> list = new ArrayList<NashCommunicationData>();

			for (NashCommunicationData data : protocol)
				if (data.getIndexOfSender() == i) {
					list.add(data);
          totalValue[i] += (data.probability * (double) data.cost);
        }

			communications[i] = list.toArray(new NashCommunicationData[list.size()]);

		}

	}

  /**
   * Retrieve the array of all communications associated with a specified sending controller.
   * @param index The index of the associated sending controller (0-based)
   * @return      The array of communications associated with the specified sender
   **/
	public NashCommunicationData[] getCommunications(int index) {
		return communications[index];
	}

  /**
   * Get the total value of all communications corresponding with the specified sender.
   * @param index The index of the associated sending controller (0-based)
   * @return      The total value, where value is the product of cost and probability
   **/
  public double getValue(int index) {
    return totalValue[index];
  }

	/**
   * Given the source automaton, represent this object as a string.
   * @param automaton The automaton where this protocol came from
   * @return          The string representation
   **/
	public String toString(Automaton automaton) {
	  
    StringBuilder stringBuilder = new StringBuilder();

    for (NashCommunicationData[] element : communications) { 
      String str = "";
      for (NashCommunicationData data : element)
        str += "," + data.toString(automaton);

      if (str.length() > 0)
        stringBuilder.append(",[" + str.substring(1) + "]");
      else
        stringBuilder.append(",[]");

    }

    if (stringBuilder.length() > 0)
      return "(" + stringBuilder.toString().substring(1) + ")";
    else
      return "()";
	}

}