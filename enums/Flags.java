package enums;

public enum Flags {
  SQUASH("-squash"),
  DESQUASH("-desquash"),
  CLEAN("-clean");

  private final String flag;

  Flags(String flag) {
    this.flag = flag;
  }

  public static Flags filter(String argFlag) {
    for (Flags flag : values()) if (flag.flag.equals(argFlag)) return flag;
    return null;
  }
}
