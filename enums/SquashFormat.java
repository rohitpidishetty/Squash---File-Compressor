package enums;

public enum SquashFormat {
  V2(2, "+--SQ-PGM--+");

  private final String header;
  private final int versionId;

  SquashFormat(int versionId, String header) {
    this.header = header;
    this.versionId = versionId;
  }

  public String getHeader() {
    return this.header;
  }

  public int getCurrentVersion() {
    return this.versionId;
  }
}
