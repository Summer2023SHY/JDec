import java.util.*;

public class ProtocolVector {

	private NashCommunicationData[][] communications;
  private double[] value;
  private double totalValue;
  private Set<NashCommunicationData> protocol;
		
	public ProtocolVector(Set<NashCommunicationData> protocol, int nControllers) {

    this.protocol = protocol;

    communications = new NashCommunicationData[nControllers][];
		value = new double[nControllers];

		for (int i = 0; i < nControllers; i++) {

			List<NashCommunicationData> list = new ArrayList<NashCommunicationData>();

			for (NashCommunicationData data : protocol)
				if (data.getIndexOfSender() == i) {
					list.add(data);
          value[i] += (data.probability * (double) data.cost);
        }

      totalValue += value[i];

			communications[i] = list.toArray(new NashCommunicationData[list.size()]);

		}

	}

  /**
   * Retrieve the original protocol (before it was vectorized).
   * @return  The protocol
   **/
  public Set<NashCommunicationData> getOriginalProtocol() {
    return protocol;
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
    return value[index];
  }

  /**
   * Get the total value of all communications in the protocol.
   * @return  The total value, where value is the product of cost and probability
   **/
  public double getValue() {
    return totalValue;
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