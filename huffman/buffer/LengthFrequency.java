package huffman.buffer;

public class LengthFrequency extends Buffer {

  public LengthFrequency(int length) {
    super(length, 0);
  }

  public int getKey() {
    return super.Key;
  }

  public void incrementFrequency() {
    super.Frequency++;
  }
}
