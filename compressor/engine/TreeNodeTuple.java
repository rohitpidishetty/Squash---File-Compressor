package compressor.engine;

public class TreeNodeTuple {

  public byte Byte;
  public int Frequency;
  public TreeNodeTuple leftTuple = null, rightTuple = null;

  public TreeNodeTuple(byte Byte, int Frequency) {
    this.Byte = Byte;
    this.Frequency = Frequency;
  }

  public boolean isLeaf() {
    return leftTuple == null && rightTuple == null;
  }

  @Override
  public String toString() {
    return "(" + this.Byte + " " + this.Frequency + ")";
  }
}
