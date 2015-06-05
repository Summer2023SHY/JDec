public enum CommunicationRole {
  
    NONE((byte) 0),
    SENDER((byte) 1),
    RECIEVER((byte) 2);

    private final byte value;

    CommunicationRole(byte value) {
      this.value = value;
    }

    public byte getValue() {
      return value;
    }

    public static CommunicationRole getRole(byte value) {

      for (CommunicationRole role : CommunicationRole.values()) {
        if (role.getValue() == value)
          return role;
      }

      return null;

    }

} 