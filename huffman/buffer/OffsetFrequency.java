package huffman.buffer;

public class OffsetFrequency extends Buffer {

  public OffsetFrequency(int offset) {
    super(offset, 0);
  }

  public int getKey() {
    return super.Key;
  }

  public void incrementFrequency() {
    super.Frequency++;
  }
}
