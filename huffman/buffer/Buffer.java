package huffman.buffer;

public class Buffer {

  public int Key;
  public int Frequency;
  public Buffer leftTuple = null,
    rightTuple = null;

  public Buffer(int Key, int Frequency) {
    this.Key = Key;
    this.Frequency = Frequency;
  }

  public boolean isLeaf() {
    return leftTuple == null && rightTuple == null;
  }
}
