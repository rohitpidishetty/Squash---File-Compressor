package enums;

public enum Files {
  FILE(0),
  FOLDER(1);

  private final int rootType;

  Files(int rootType) {
    this.rootType = rootType;
  }

  public int getRootType() {
    return this.rootType;
  }
}
