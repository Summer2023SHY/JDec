class CommunicationLabelVector extends LabelVector {

  public CommunicationRole[] roles;

  public CommunicationLabelVector(String label, CommunicationRole[] roles) {

    super(label);

    this.roles = roles;

  }

}