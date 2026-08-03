package lz77.decoder;

import java.io.IOException;
import java.io.InputStream;

public final class BitInputStream {

  private final InputStream input;

  private int currentByte;
  private int bitCount;

  public BitInputStream(InputStream input) {
    if (input == null) {
      throw new IllegalArgumentException("Input stream cannot be null");
    }
    this.input = input;
  }

  public int readBit() throws IOException {
    if (bitCount == 0) {
      currentByte = input.read();
      if (currentByte == -1) return -1;
      bitCount = 8;
    }
    bitCount--;
    return (currentByte >>> bitCount) & 1;
  }

  public int readBits(int count) throws IOException {
    if (count < 0 || count > 32) {
      throw new IllegalArgumentException("Invalid bit count");
    }
    int value = 0;
    for (int i = 0; i < count; i++) {
      int bit = readBit();
      if (bit == -1) throw new IOException("Unexpected end of bit stream");
      value = (value << 1) | bit;
    }
    return value;
  }
}
