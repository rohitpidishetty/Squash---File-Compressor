package huffman.buffer;

public class CodeFrequency extends Buffer {

  public CodeFrequency(Byte code) {
    super(code, 0);
  }

  public int getKey() {
    return super.Key;
  }

  public void incrementFrequency() {
    super.Frequency++;
  }
}
