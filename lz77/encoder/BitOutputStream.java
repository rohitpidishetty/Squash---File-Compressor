package lz77.encoder;

import java.io.IOException;
import java.io.OutputStream;

final class BitOutputStream {

  private final OutputStream output;

  private int currentByte;
  private int bitCount;
  private boolean finished;

  BitOutputStream(OutputStream output) {
    if (output == null) throw new IllegalArgumentException(
      "Output stream cannot be null"
    );

    this.output = output;
  }

  void writeBit(int bit) throws IOException {
    ensureOpen();

    currentByte = (currentByte << 1) | (bit & 1);

    bitCount++;

    if (bitCount == 8) {
      flushCurrentByte();
    }
  }

  void writeBits(int value, int numberOfBits) throws IOException {
    ensureOpen();

    if (
      numberOfBits < 0 || numberOfBits > 32
    ) throw new IllegalArgumentException("Invalid bit count: " + numberOfBits);

    for (int shift = numberOfBits - 1; shift >= 0; shift--) {
      writeBit((value >>> shift) & 1);
    }
  }

  void finish() throws IOException {
    if (finished) return;

    if (bitCount > 0) {
      currentByte <<= 8 - bitCount;
      output.write(currentByte);
    }

    output.flush();

    currentByte = 0;
    bitCount = 0;
    finished = true;
  }

  private void flushCurrentByte() throws IOException {
    output.write(currentByte);
    currentByte = 0;
    bitCount = 0;
  }

  private void ensureOpen() {
    if (finished) throw new IllegalStateException(
      "Bit stream has already been finished"
    );
  }
}
