// package squash.archive;

public class CharFrequency {

  protected Character ch;
  protected Integer freq;
  protected NodeType type;
  protected Integer frequency;
  protected Character charId;
  protected CharFrequency leftNode = null, rightNode = null;

  public CharFrequency(Character ch, Integer freq, NodeType type) {
    this.ch = ch;
    this.freq = freq;
    this.type = type;
  }

  @Override
  public String toString() {
    return "(" + this.ch + " " + this.freq + ")";
  }
}
